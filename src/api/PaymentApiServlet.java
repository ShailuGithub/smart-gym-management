package api;

import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import model.Member;
import model.MemberSubscription;
import model.Payment;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * /api/payments - mirrors servlet.PaymentServlet's web behavior over JSON.
 * GET  /api/payments?branchId=  -> branch payments ledger, newest first
 * POST /api/payments            -> record a standalone/ad-hoc payment (e.g. collecting an old due);
 *                                   most payment rows are instead created automatically by
 *                                   /api/subscriptions when a plan is assigned/renewed.
 */
@WebServlet("/api/payments")
public class PaymentApiServlet extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    private static final class PaymentInput {
        int memberId;
        Integer subscriptionId;
        LocalDate paymentDate;
        BigDecimal amount;
        String mode;
        String referenceNo;
        String remarks;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessPayments()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have access to Payments.");
            return;
        }
        try {
            Integer branchId = ApiSupport.resolveConcreteBranchId(user, gymBranchDAO, request.getParameter("branchId"));
            if (branchId == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Specify a branchId query parameter (see GET /api/branches).");
                return;
            }
            JsonUtil.writeJson(response, paymentDAO.findByBranch(branchId));
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        if (!user.canAccessPayments() || !user.canCreate()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_FORBIDDEN, "You don't have permission to record payments.");
            return;
        }
        PaymentInput input;
        try {
            input = JsonUtil.readJson(request, PaymentInput.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (input == null || input.memberId <= 0 || input.amount == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "memberId and amount are required.");
            return;
        }
        try {
            // A payment's branch is always its member's true branch - never trusted blindly for a
            // MANAGER/STAFF, who must only ever record payments for members in their own branch.
            Member member = memberDAO.findById(input.memberId);
            if (member == null || !ApiSupport.canAccessBranch(user, member.getBranchId())) {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Member not found.");
                return;
            }
            Payment p = new Payment();
            p.setMemberId(input.memberId);
            p.setBranchId(member.getBranchId());
            if (input.subscriptionId != null) {
                p.setSubscriptionId(input.subscriptionId);
            } else {
                MemberSubscription latest = subscriptionDAO.findLatestForMember(input.memberId);
                p.setSubscriptionId(latest == null ? null : latest.getSubscriptionId());
            }
            p.setPaymentDate(input.paymentDate == null ? LocalDate.now() : input.paymentDate);
            p.setAmount(input.amount);
            p.setMode(input.mode);
            p.setReferenceNo(input.referenceNo);
            p.setRemarks(input.remarks);
            int newId = paymentDAO.insert(p);
            ApiAudit.record(user, member.getBranchId(), "CREATE", "PAYMENT", newId,
                    "Recorded payment of Rs. " + p.getAmount() + " from " + member.getFullName());
            JsonUtil.writeJson(response, paymentDAO.findById(newId));
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
