package servlet;

import dao.GymBranchDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.PaymentDAO;
import model.GymBranch;
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
import java.util.List;

/**
 * Payments ledger for whichever single branch is currently in scope (see BranchContext) - same
 * "never silently guess the branch" rule as MemberServlet. Most rows are created automatically
 * by MemberSubscriptionServlet when a plan is assigned/renewed; this screen additionally lets
 * staff record a standalone/ad-hoc payment (e.g. collecting an old due) not tied to a fresh
 * subscription.
 */
@WebServlet("/payments")
public class PaymentServlet extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessPayments()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        try {
            List<GymBranch> allBranches = gymBranchDAO.findAll();
            if (allBranches.isEmpty()) {
                request.setAttribute("noBranch", true);
                request.getRequestDispatcher("/Forms/Transaction/payments.jsp").forward(request, response);
                return;
            }

            Integer scopedBranchId = BranchContext.currentBranchIdOrNull(request);
            if (scopedBranchId == null) {
                request.setAttribute("selectBranchFirst", true);
                request.setAttribute("allBranches", allBranches);
                request.getRequestDispatcher("/Forms/Transaction/payments.jsp").forward(request, response);
                return;
            }

            if ("form".equals(request.getParameter("view"))) {
                request.setAttribute("activeMembers", memberDAO.findActiveByBranch(scopedBranchId));
                request.setAttribute("targetBranchName", branchName(allBranches, scopedBranchId));
                String preselectMemberId = request.getParameter("memberId");
                if (preselectMemberId != null) {
                    request.setAttribute("preselectMemberId", preselectMemberId);
                }
                request.getRequestDispatcher("/Forms/Transaction/payment_form.jsp").forward(request, response);
                return;
            }

            request.setAttribute("payments", paymentDAO.findByBranch(scopedBranchId));
            request.setAttribute("currentBranchId", scopedBranchId);
            request.setAttribute("currentBranchName", branchName(allBranches, scopedBranchId));
            request.getRequestDispatcher("/Forms/Transaction/payments.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = Permissions.userOf(request);
        if (!user.canAccessPayments()) {
            Permissions.deny(request, response, "/dashboard.jsp");
            return;
        }
        String action = request.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "add": {
                    if (!user.canCreate()) {
                        Permissions.deny(request, response, "/payments");
                        return;
                    }
                    // A payment's branch is always its member's true branch - never the ambient
                    // header-switcher selection, and never trusted blindly for a MANAGER/STAFF
                    // (who must only ever record payments for members in their own branch).
                    int memberId = Integer.parseInt(request.getParameter("memberId"));
                    Member member = memberDAO.findById(memberId);
                    if (member == null || !MemberServlet.canAccess(user, member.getBranchId())) {
                        Permissions.deny(request, response, "/payments");
                        return;
                    }
                    Payment p = readForm(request);
                    p.setMemberId(memberId);
                    p.setBranchId(member.getBranchId());
                    String subscriptionIdParam = request.getParameter("subscriptionId");
                    if (subscriptionIdParam != null && !subscriptionIdParam.trim().isEmpty()) {
                        p.setSubscriptionId(Integer.parseInt(subscriptionIdParam));
                    } else {
                        MemberSubscription latest = subscriptionDAO.findLatestForMember(memberId);
                        p.setSubscriptionId(latest == null ? null : latest.getSubscriptionId());
                    }
                    int newId = paymentDAO.insert(p);
                    AuditLog.record(request, member.getBranchId(), "CREATE", "PAYMENT", newId,
                            "Recorded payment of Rs. " + p.getAmount() + " from " + member.getFullName());
                    break;
                }
                case "delete": {
                    if (!user.canDelete()) {
                        Permissions.deny(request, response, "/payments");
                        return;
                    }
                    int paymentId = Integer.parseInt(request.getParameter("paymentId"));
                    Payment existing = paymentDAO.findById(paymentId);
                    if (existing == null || !MemberServlet.canAccess(user, existing.getBranchId())) {
                        Permissions.deny(request, response, "/payments");
                        return;
                    }
                    paymentDAO.delete(existing.getBranchId(), paymentId);
                    AuditLog.record(request, existing.getBranchId(), "DELETE", "PAYMENT", paymentId, "Deleted payment");
                    break;
                }
                default:
                    break;
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        response.sendRedirect(request.getContextPath() + "/payments");
    }

    private String branchName(List<GymBranch> allBranches, int branchId) {
        for (GymBranch b : allBranches) {
            if (b.getBranchId() == branchId) {
                return b.getName();
            }
        }
        return "";
    }

    private Payment readForm(HttpServletRequest request) {
        Payment p = new Payment();
        String date = request.getParameter("paymentDate");
        p.setPaymentDate(date == null || date.trim().isEmpty() ? LocalDate.now() : LocalDate.parse(date));
        p.setAmount(new BigDecimal(request.getParameter("amount")));
        p.setMode(request.getParameter("mode"));
        p.setReferenceNo(request.getParameter("referenceNo"));
        p.setRemarks(request.getParameter("remarks"));
        return p;
    }
}
