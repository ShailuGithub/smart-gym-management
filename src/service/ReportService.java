package service;

import dao.AppSettingsDAO;
import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import model.AppSettings;
import model.GymBranch;
import model.Member;
import model.MemberSubscription;
import model.MembershipStatus;
import model.Payment;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the three Reports rows (Membership Due/Expiring, Revenue/Collection, Branch-wise Active
 * Members) - the shared business logic behind both the web (servlet.ReportServlet) and JSON API
 * (api.ReportApiServlet) entry points, so this scope/aggregation logic exists in exactly one
 * place, same pattern as SubscriptionService/ReminderService.
 */
public final class ReportService {

    private static final GymBranchDAO gymBranchDAO = new GymBranchDAO();
    private static final MemberDAO memberDAO = new MemberDAO();
    private static final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    private ReportService() {
    }

    /** View-model row for the Membership Due/Expiring report. */
    public static final class DueRow {
        public final String branchName;
        public final Member member;
        public final MembershipStatus status;
        public final LocalDate lastPaymentDate;

        DueRow(String branchName, Member member, MembershipStatus status, LocalDate lastPaymentDate) {
            this.branchName = branchName;
            this.member = member;
            this.status = status;
            this.lastPaymentDate = lastPaymentDate;
        }
    }

    /** View-model row for the Revenue/Collection report - one calendar day's total across scope. */
    public static final class RevenueRow {
        public final LocalDate date;
        public final BigDecimal amount;

        RevenueRow(LocalDate date, BigDecimal amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    /** View-model row for the Branch-wise Active Members summary - one branch's member-status counts. */
    public static final class BranchSummaryRow {
        public final String branchName;
        public final int active;
        public final int expiringSoon;
        public final int expired;
        public final int noPlan;

        BranchSummaryRow(String branchName, int active, int expiringSoon, int expired, int noPlan) {
            this.branchName = branchName;
            this.active = active;
            this.expiringSoon = expiringSoon;
            this.expired = expired;
            this.noPlan = noPlan;
        }
    }

    /** Restricts allBranches to just the branch matching scopedBranchId, or returns allBranches unchanged if null. */
    public static List<GymBranch> resolveScope(List<GymBranch> allBranches, Integer scopedBranchId) {
        if (scopedBranchId == null) {
            return allBranches;
        }
        List<GymBranch> list = new ArrayList<>();
        for (GymBranch b : allBranches) {
            if (b.getBranchId() == scopedBranchId) {
                list.add(b);
            }
        }
        return list;
    }

    /**
     * Every member across scope who is EXPIRING_SOON or EXPIRED right now, with their current
     * plan and last payment date - a point-in-time snapshot, not filtered by date range (unlike
     * Revenue), since "due" inherently means "as of today".
     */
    public static List<DueRow> buildDueRows(List<GymBranch> scope) throws SQLException {
        AppSettings settings = appSettingsDAO.find();
        List<DueRow> rows = new ArrayList<>();
        for (GymBranch b : scope) {
            Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(b.getBranchId());
            Map<Integer, Payment> lastPaymentByMember = latestPaymentByMember(paymentDAO.findByBranch(b.getBranchId()));
            for (Member m : memberDAO.findActiveByBranch(b.getBranchId())) {
                MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()),
                        settings.getReminderDaysBeforeExpiry());
                if (status.isExpiringSoon() || status.isExpired()) {
                    Payment lastPayment = lastPaymentByMember.get(m.getMemberId());
                    rows.add(new DueRow(b.getName(), m, status, lastPayment == null ? null : lastPayment.getPaymentDate()));
                }
            }
        }
        return rows;
    }

    /** First (newest, since findByBranch orders newest-first) payment seen per member. */
    private static Map<Integer, Payment> latestPaymentByMember(List<Payment> payments) {
        Map<Integer, Payment> map = new LinkedHashMap<>();
        for (Payment p : payments) {
            map.putIfAbsent(p.getMemberId(), p);
        }
        return map;
    }

    public static List<RevenueRow> buildRevenueRows(List<GymBranch> scope, LocalDate start, LocalDate end) throws SQLException {
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            byDay.put(d, BigDecimal.ZERO);
        }
        for (GymBranch b : scope) {
            for (Payment p : paymentDAO.findByBranchAndDateRange(b.getBranchId(), start, end)) {
                byDay.merge(p.getPaymentDate(), p.getAmount(), BigDecimal::add);
            }
        }
        List<RevenueRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : byDay.entrySet()) {
            rows.add(new RevenueRow(e.getKey(), e.getValue()));
        }
        return rows;
    }

    public static BigDecimal sumRevenue(List<GymBranch> scope, LocalDate start, LocalDate end) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (GymBranch b : scope) {
            total = total.add(paymentDAO.totalForBranchAndDateRange(b.getBranchId(), start, end));
        }
        return total;
    }

    /** Always every branch, regardless of any branch-scope filter - that's the point of this report. */
    public static List<BranchSummaryRow> buildBranchSummaryRows() throws SQLException {
        return buildBranchSummaryRows(gymBranchDAO.findAll());
    }

    public static List<BranchSummaryRow> buildBranchSummaryRows(List<GymBranch> allBranches) throws SQLException {
        AppSettings settings = appSettingsDAO.find();
        List<BranchSummaryRow> rows = new ArrayList<>();
        for (GymBranch b : allBranches) {
            Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(b.getBranchId());
            int active = 0, expiringSoon = 0, expired = 0, noPlan = 0;
            for (Member m : memberDAO.findActiveByBranch(b.getBranchId())) {
                MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()),
                        settings.getReminderDaysBeforeExpiry());
                if (status.isActive()) { active++; }
                else if (status.isExpiringSoon()) { expiringSoon++; }
                else if (status.isExpired()) { expired++; }
                else { noPlan++; }
            }
            rows.add(new BranchSummaryRow(b.getName(), active, expiringSoon, expired, noPlan));
        }
        return rows;
    }
}
