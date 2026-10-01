package sms;

/**
 * Pluggable SMS delivery - a real gateway (MSG91, Twilio, etc.) can implement this later without
 * any caller (ReminderServlet) needing to change. See LoggingSmsService for the current
 * dependency-free default.
 */
public interface SmsService {

    SmsResult send(String phone, String message);

    /** Outcome of one send attempt - success/failure plus a short human-readable reason. */
    final class SmsResult {

        private final boolean success;
        private final String statusMessage;

        private SmsResult(boolean success, String statusMessage) {
            this.success = success;
            this.statusMessage = statusMessage;
        }

        public static SmsResult success(String statusMessage) {
            return new SmsResult(true, statusMessage);
        }

        public static SmsResult failure(String statusMessage) {
            return new SmsResult(false, statusMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getStatusMessage() {
            return statusMessage;
        }
    }
}
