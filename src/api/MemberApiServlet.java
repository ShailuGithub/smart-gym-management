package api;

import dao.AppSettingsDAO;
import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import model.AppSettings;
import model.Member;
import model.MemberSubscription;
import model.MembershipStatus;
import model.Payment;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /api/members - branch-scoped member CRUD, mirroring servlet.MemberServlet's rules (a member
 * always belongs to exactly one branch; a MANAGER/STAFF is always locked to their own; a member's
 * branch never changes via update) but over JSON/path-info instead of JSP forms.
 * GET /api/members?branchId=          -> list, each with its computed MembershipStatus
 * GET /api/members/{id}               -> one member, with status + subscription/payment history
 * POST /api/members                   -> create (branchId required in body for an ADMIN, ignored for MANAGER/STAFF)
 * PUT /api/members/{id}                -> update (branchId immutable, same as the web app)
 */
@WebServlet("/api/members/*")
public class MemberApiServlet extends HttpServlet {

    private final MemberDAO memberDAO = new MemberDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();
    private final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();

    private static final class MemberView {
        final Member member;
        final MembershipStatus membershipStatus;

        MemberView(Member member, MembershipStatus status) {
            this.member = member;
            this.membershipStatus = status;
        }
    }

    private static final class MemberDetail {
        final Member member;
        final MembershipStatus membershipStatus;
        final List<MemberSubscription> subscriptionHistory;
        final List<Payment> paymentHistory;

        MemberDetail(Member member, MembershipStatus status, List<MemberSubscription> subs, List<Payment> payments) {
            this.member = member;
            this.membershipStatus = status;
            this.subscriptionHistory = subs;
            this.paymentHistory = payments;
        }
    }

    private static final class MemberInput {
        String fullName;
        String phone;
        String email;
        String gender;
        LocalDate dob;
        String address;
        LocalDate joinDate;
        String status;
        Integer branchId; // only honored for ADMIN on create; ignored on update and for MANAGER/STAFF
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessMembers()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Members.");
            return;
        }
        try {
            Integer memberId = pathId(request);
            if (memberId != null) {
                getOne(user, memberId, response);
                return;
            }

            Integer branchId = ApiSupport.resolveConcreteBranchId(user, gymBranchDAO, request.getParameter("branchId"));
            if (branchId == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Specify a branchId query parameter (see GET /api/branches).");
                return;
            }
            AppSettings settings = appSettingsDAO.find();
            Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(branchId);
            List<MemberView> views = new ArrayList<>();
            for (Member m : memberDAO.findByBranch(branchId)) {
                MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()),
                        settings.getReminderDaysBeforeExpiry());
                views.add(new MemberView(m, status));
            }
            JsonUtil.writeJson(response, views);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private void getOne(User user, int memberId, HttpServletResponse response) throws SQLException, IOException {
        Member member = memberDAO.findById(memberId);
        if (member == null || !ApiSupport.canAccessBranch(user, member.getBranchId())) {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Member not found.");
            return;
        }
        AppSettings settings = appSettingsDAO.find();
        MemberSubscription latest = subscriptionDAO.findLatestForMember(memberId);
        MembershipStatus status = MembershipStatus.of(memberId, latest, settings.getReminderDaysBeforeExpiry());
        JsonUtil.writeJson(response, new MemberDetail(member, status,
                subscriptionDAO.findByMember(memberId), paymentDAO.findByMember(memberId)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessMembers() || !user.canCreate()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to add members.");
            return;
        }
        MemberInput input;
        try {
            input = JsonUtil.readJson(request, MemberInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null || input.fullName == null || input.fullName.trim().isEmpty()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "fullName is required.");
            return;
        }
        try {
            Integer branchId = ApiSupport.resolveConcreteBranchId(user, gymBranchDAO,
                    input.branchId == null ? null : String.valueOf(input.branchId));
            if (branchId == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Specify a valid branchId (see GET /api/branches).");
                return;
            }
            Member m = new Member();
            m.setBranchId(branchId);
            m.setFullName(input.fullName);
            m.setPhone(input.phone);
            m.setEmail(input.email);
            m.setGender(input.gender);
            m.setDob(input.dob);
            m.setAddress(input.address);
            m.setJoinDate(input.joinDate == null ? LocalDate.now() : input.joinDate);
            m.setStatus(Member.INACTIVE.equals(input.status) ? Member.INACTIVE : Member.ACTIVE);
            int newId = memberDAO.insert(m);
            ApiAudit.record(user, branchId, "CREATE", "MEMBER", newId, "Added member: " + m.getFullName());
            getOne(user, newId, response);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessMembers() || !user.canEdit()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to edit members.");
            return;
        }
        Integer memberId = pathId(request);
        if (memberId == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "PUT /api/members/{id} requires an id.");
            return;
        }
        MemberInput input;
        try {
            input = JsonUtil.readJson(request, MemberInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        try {
            Member existing = memberDAO.findById(memberId);
            if (existing == null || !ApiSupport.canAccessBranch(user, existing.getBranchId())) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Member not found.");
                return;
            }
            Member m = new Member();
            m.setMemberId(memberId);
            m.setBranchId(existing.getBranchId()); // a member's branch never changes via edit
            m.setFullName(input != null && input.fullName != null ? input.fullName : existing.getFullName());
            m.setPhone(input != null ? input.phone : existing.getPhone());
            m.setEmail(input != null ? input.email : existing.getEmail());
            m.setGender(input != null ? input.gender : existing.getGender());
            m.setDob(input != null ? input.dob : existing.getDob());
            m.setAddress(input != null ? input.address : existing.getAddress());
            m.setJoinDate(input != null && input.joinDate != null ? input.joinDate : existing.getJoinDate());
            m.setStatus(input != null && Member.INACTIVE.equals(input.status) ? Member.INACTIVE : Member.ACTIVE);
            memberDAO.update(m);
            ApiAudit.record(user, existing.getBranchId(), "UPDATE", "MEMBER", memberId, "Updated member: " + m.getFullName());
            getOne(user, memberId, response);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    /** Extracts the numeric id from a "/{id}" path-info segment, or null for the collection root. */
    private Integer pathId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo(); // e.g. "/42", or null/"/" for /api/members itself
        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }
        String segment = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
