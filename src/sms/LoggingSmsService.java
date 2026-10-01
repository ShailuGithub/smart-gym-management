package sms;

/**
 * Dev-mode SmsService - no real gateway wired up, matching this project's dependency-free
 * approach elsewhere (e.g. Forgot Password shows the reset link on-screen instead of emailing
 * it). Logs the message to stdout and always reports success, so the rest of the app (reminder
 * history, audit log) behaves exactly as it would once a real gateway is plugged in behind this
 * same interface - only ReminderServlet's `new LoggingSmsService()` line would need to change.
 */
public class LoggingSmsService implements SmsService {

    @Override
    public SmsResult send(String phone, String message) {
        System.out.println("[SMS] To " + phone + ": " + message);
        return SmsResult.success("Logged only (no SMS gateway configured)");
    }
}
