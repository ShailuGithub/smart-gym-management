package model;

public class AppSettings {

    private String businessName;
    private String address;
    private String phone;
    private String email;
    private String currencySymbol = "Rs.";
    private int reminderDaysBeforeExpiry = 3;
    private boolean smsEnabled = false;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public int getReminderDaysBeforeExpiry() {
        return reminderDaysBeforeExpiry;
    }

    public void setReminderDaysBeforeExpiry(int reminderDaysBeforeExpiry) {
        this.reminderDaysBeforeExpiry = reminderDaysBeforeExpiry;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }
}
