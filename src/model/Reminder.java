package model;

import java.time.LocalDateTime;

public class Reminder {

    public static final String EXPIRY = "EXPIRY";
    public static final String DUE = "DUE";

    public static final String SMS = "SMS";
    public static final String PUSH = "PUSH";

    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    // Sent when AppSettings.smsEnabled is off - the reminder is recorded but never actually
    // handed to SmsService, so it's neither a real SENT nor a delivery FAILED.
    public static final String LOGGED_ONLY = "LOGGED_ONLY";

    private int reminderId;
    private int memberId;
    private int branchId;
    private String type = DUE;
    private String channel = SMS;
    private String message;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public int getReminderId() {
        return reminderId;
    }

    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Bootstrap color suffix for badges (bg-<x>-subtle text-<x>). */
    public String getBadgeColor() {
        switch (status == null ? "" : status) {
            case SENT: return "success";
            case FAILED: return "danger";
            case LOGGED_ONLY: return "secondary";
            default: return "secondary";
        }
    }
}
