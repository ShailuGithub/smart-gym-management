package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment actually collected from a member - distinct from member_subscriptions.amountCharged
 * (what was owed for a plan period). Most rows are created automatically alongside a subscription
 * (see MemberSubscriptionServlet), but a payment can also be recorded standalone (subscriptionId
 * null) for a walk-in/ad-hoc collection not tied to a fresh plan purchase.
 */
public class Payment {

    private int paymentId;
    private int branchId;
    private int memberId;
    private Integer subscriptionId; // nullable - not every payment is tied to a specific subscription
    private LocalDate paymentDate;
    private BigDecimal amount = BigDecimal.ZERO;
    private String mode; // CASH, UPI, BANK_TRANSFER, CARD, CHEQUE
    private String referenceNo;
    private String remarks;

    // Populated only by DAO methods that join members - not persisted directly.
    private String memberName;

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
