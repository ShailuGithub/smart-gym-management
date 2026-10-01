package service;

import dao.AppSettingsDAO;
import dao.MemberDAO;
import dao.MemberSubscriptionDAO;
import dao.ReminderDAO;
import model.AppSettings;
import model.Member;
import model.MemberSubscription;
import model.MembershipStatus;
import model.Reminder;
import sms.LoggingSmsService;
import sms.SmsService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds members due for a reminder and sends them one - the shared business logic behind both the
 * web (servlet.ReminderServlet) and JSON API (api.ReminderApiServlet) entry points, so the due
 * computation and message/delivery logic exist in exactly one place. Reuses model.MembershipStatus
 * for the due computation rather than re-deriving it. Callers are responsible for their own
 * permission checks and audit logging (this class has no notion of "who is asking").
 */
public final class ReminderService {

    private static final MemberDAO memberDAO = new MemberDAO();
    private static final MemberSubscriptionDAO subscriptionDAO = new MemberSubscriptionDAO();
    private static final AppSettingsDAO appSettingsDAO = new AppSettingsDAO();
    private static final ReminderDAO reminderDAO = new ReminderDAO();
    private static final SmsService smsService = new LoggingSmsService();

    private ReminderService() {
    }

    /** View-model row: a due member paired with their computed status and last reminder (if any). */
    public static final class DueMember {
        public final Member member;
        public final MembershipStatus status;
        public final Reminder lastReminder;

        DueMember(Member member, MembershipStatus status, Reminder lastReminder) {
            this.member = member;
            this.status = status;
            this.lastReminder = lastReminder;
        }
    }

    /** ACTIVE members in a branch whose MembershipStatus is EXPIRING_SOON or EXPIRED. */
    public static List<DueMember> findDueMembers(int branchId) throws SQLException {
        AppSettings settings = appSettingsDAO.find();
        Map<Integer, MemberSubscription> latestByMember = subscriptionDAO.findLatestForBranch(branchId);
        Map<Integer, Reminder> lastReminderByMember = reminderDAO.findLatestForBranch(branchId);
        List<Member> members = memberDAO.findActiveByBranch(branchId);
        List<DueMember> due = new ArrayList<>();
        for (Member m : members) {
            MembershipStatus status = MembershipStatus.of(m.getMemberId(), latestByMember.get(m.getMemberId()),
                    settings.getReminderDaysBeforeExpiry());
            if (status.isExpiringSoon() || status.isExpired()) {
                due.add(new DueMember(m, status, lastReminderByMember.get(m.getMemberId())));
            }
        }
        return due;
    }

    /** True if this member already got a reminder today (used by "send to all" to avoid spamming). */
    public static boolean remindedToday(int memberId) throws SQLException {
        return reminderDAO.remindedToday(memberId);
    }

    /**
     * Builds and "sends" (subject to AppSettings.smsEnabled - see SmsService) a reminder to one
     * member, and logs it. Returns the inserted Reminder so callers can audit-log with their own
     * context (an HTTP session's user for the web app, an api_tokens-authenticated user for the API).
     */
    public static Reminder sendReminderTo(Member member) throws SQLException {
        AppSettings settings = appSettingsDAO.find();
        MemberSubscription latest = subscriptionDAO.findLatestForMember(member.getMemberId());
        MembershipStatus status = MembershipStatus.of(member.getMemberId(), latest, settings.getReminderDaysBeforeExpiry());

        String type = status.isExpired() ? Reminder.EXPIRY : Reminder.DUE;
        String message = buildMessage(settings, member, status);

        Reminder r = new Reminder();
        r.setMemberId(member.getMemberId());
        r.setBranchId(member.getBranchId());
        r.setType(type);
        r.setChannel(Reminder.SMS);
        r.setMessage(message);

        if (!settings.isSmsEnabled()) {
            r.setStatus(Reminder.LOGGED_ONLY);
        } else {
            SmsService.SmsResult result = smsService.send(member.getPhone(), message);
            r.setStatus(result.isSuccess() ? Reminder.SENT : Reminder.FAILED);
        }
        int reminderId = reminderDAO.insert(r);
        r.setReminderId(reminderId);
        return r;
    }

    private static String buildMessage(AppSettings settings, Member member, MembershipStatus status) {
        String business = settings.getBusinessName() == null ? "your gym" : settings.getBusinessName();
        if (status.isExpired()) {
            return "Hi " + member.getFullName() + ", your membership at " + business
                    + " has expired. Please renew to continue enjoying our services.";
        }
        String endDate = status.getCurrentSubscription() == null ? "" : " (" + status.getCurrentSubscription().getEndDate() + ")";
        return "Hi " + member.getFullName() + ", your membership at " + business + " expires in "
                + status.getDaysRemaining() + " day(s)" + endDate + ". Renew soon to avoid interruption.";
    }
}
