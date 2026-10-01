package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A member's plan status, computed on the fly from their latest member_subscriptions row rather
 * than stored - same "don't store what you can derive" rule the reference project used for rent
 * dues (see PaymentStatus there). NO_PLAN = never had a subscription. EXPIRED = latest
 * subscription's end date has passed (or it was CANCELLED). EXPIRING_SOON = still within its
 * validity window, but within app_settings.reminder_days_before_expiry of ending. ACTIVE =
 * comfortably valid.
 */
public class MembershipStatus {

    public static final String NO_PLAN = "NO_PLAN";
    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRING_SOON = "EXPIRING_SOON";
    public static final String EXPIRED = "EXPIRED";

    private final int memberId;
    private final MemberSubscription currentSubscription; // latest row by end_date, or null if none exists
    private final String state;
    private final long daysRemaining; // negative once past end_date; meaningless (0) for NO_PLAN

    private MembershipStatus(int memberId, MemberSubscription currentSubscription, String state, long daysRemaining) {
        this.memberId = memberId;
        this.currentSubscription = currentSubscription;
        this.state = state;
        this.daysRemaining = daysRemaining;
    }

    public static MembershipStatus of(int memberId, MemberSubscription latestSubscription, int reminderDaysBeforeExpiry) {
        if (latestSubscription == null) {
            return new MembershipStatus(memberId, null, NO_PLAN, 0);
        }
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), latestSubscription.getEndDate());
        if (daysRemaining < 0 || !MemberSubscription.ACTIVE.equals(latestSubscription.getStatus())) {
            return new MembershipStatus(memberId, latestSubscription, EXPIRED, daysRemaining);
        }
        if (daysRemaining <= reminderDaysBeforeExpiry) {
            return new MembershipStatus(memberId, latestSubscription, EXPIRING_SOON, daysRemaining);
        }
        return new MembershipStatus(memberId, latestSubscription, ACTIVE, daysRemaining);
    }

    public int getMemberId() {
        return memberId;
    }

    public MemberSubscription getCurrentSubscription() {
        return currentSubscription;
    }

    public String getState() {
        return state;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public boolean hasNoPlan() {
        return NO_PLAN.equals(state);
    }

    public boolean isActive() {
        return ACTIVE.equals(state);
    }

    public boolean isExpiringSoon() {
        return EXPIRING_SOON.equals(state);
    }

    public boolean isExpired() {
        return EXPIRED.equals(state);
    }

    /** Human label for badges, e.g. "Active", "Expiring Soon", "Expired", "No Plan". */
    public String getLabel() {
        switch (state) {
            case ACTIVE: return "Active";
            case EXPIRING_SOON: return "Expiring Soon";
            case EXPIRED: return "Expired";
            default: return "No Plan";
        }
    }

    /** Bootstrap color suffix for badges (bg-<x>-subtle text-<x>). */
    public String getBadgeColor() {
        switch (state) {
            case ACTIVE: return "success";
            case EXPIRING_SOON: return "warning";
            case EXPIRED: return "danger";
            default: return "secondary";
        }
    }
}
