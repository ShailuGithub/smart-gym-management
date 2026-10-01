package service;

import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import model.Member;
import model.MemberSubscription;
import model.MembershipPlan;
import model.Payment;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Assigns a membership plan to a member, or renews their current one - the shared business logic
 * behind both the web (MemberSubscriptionServlet) and JSON API (api.SubscriptionApiServlet)
 * entry points, so the renewal date math and the "a subscription is also a payment" rule exist in
 * exactly one place. There's only one operation ("assign or renew") because the date math is
 * identical for a member's first-ever plan and a later renewal: with no prior subscription, the
 * new period simply starts today (or the given date) instead of the day after a previous end_date.
 */
public final class SubscriptionService {

    private static final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();

    private SubscriptionService() {
    }

    /** The subscription and payment rows created by one assign/renew call. */
    public static final class Result {
        public final MemberSubscription subscription;
        public final Payment payment;

        Result(MemberSubscription subscription, Payment payment) {
            this.subscription = subscription;
            this.payment = payment;
        }
    }

    /**
     * @param requestedStartDate override for the computed start date, or null to use the default
     *                           (day after the current subscription's end date, or today if none/lapsed)
     * @param requestedAmount    override for the amount charged, or null to use the plan's price
     * @param mode               payment mode (CASH, UPI, ...); defaults to CASH if null/blank
     */
    public static Result assignOrRenew(Member member, MembershipPlan plan, LocalDate requestedStartDate,
                                        BigDecimal requestedAmount, String mode) throws SQLException {
        LocalDate today = LocalDate.now();
        MemberSubscription latest = subscriptionDAO.findLatestForMember(member.getMemberId());
        // Renewing before expiry continues seamlessly from the old end date (no gap, no overlap);
        // renewing a lapsed/expired plan (or a brand-new member) simply starts today.
        LocalDate candidateStart = latest == null ? today : latest.getEndDate().plusDays(1);
        LocalDate startDate = candidateStart.isAfter(today) ? candidateStart : today;
        if (requestedStartDate != null) {
            startDate = requestedStartDate;
        }
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());
        BigDecimal amount = requestedAmount != null ? requestedAmount : plan.getPrice();

        MemberSubscription s = new MemberSubscription();
        s.setMemberId(member.getMemberId());
        s.setBranchId(member.getBranchId());
        s.setPlanId(plan.getPlanId());
        s.setStartDate(startDate);
        s.setEndDate(endDate);
        s.setAmountCharged(amount);
        s.setStatus(MemberSubscription.ACTIVE);
        int subscriptionId = subscriptionDAO.insert(s);
        s.setSubscriptionId(subscriptionId);
        s.setPlanName(plan.getName());

        // Recording a subscription is also collecting money for it - one call, both a
        // subscription-history row and a payments-ledger row, so callers never have to key the
        // same amount in twice (see PaymentServlet/PaymentApiServlet for the standalone/ad-hoc path).
        Payment payment = new Payment();
        payment.setBranchId(member.getBranchId());
        payment.setMemberId(member.getMemberId());
        payment.setSubscriptionId(subscriptionId);
        payment.setPaymentDate(today);
        payment.setAmount(amount);
        payment.setMode(mode == null || mode.trim().isEmpty() ? "CASH" : mode);
        payment.setRemarks("Plan payment: " + plan.getName());
        int paymentId = paymentDAO.insert(payment);
        payment.setPaymentId(paymentId);

        return new Result(s, payment);
    }
}
