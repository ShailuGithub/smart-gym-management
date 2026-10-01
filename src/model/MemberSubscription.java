package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row per plan purchase/renewal for a member. A member's CURRENT plan is whichever row has
 * the latest end_date - there is no separate "current plan" column kept in sync (see
 * MemberSubscriptionDAO.findLatestForMember / MembershipStatus).
 */
public class MemberSubscription {

    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";

    private int subscriptionId;
    private int memberId;
    private int branchId;
    private int planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountCharged = BigDecimal.ZERO;
    private String status = ACTIVE;

    // Populated only by DAO methods that join membership_plans/members - not persisted directly.
    private String planName;
    private String memberName;

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getAmountCharged() {
        return amountCharged;
    }

    public void setAmountCharged(BigDecimal amountCharged) {
        this.amountCharged = amountCharged;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
