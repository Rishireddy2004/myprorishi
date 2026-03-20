package com.ridesharing.dto;

public class NotificationPreferencesDTO {

    private boolean emailNotificationsEnabled;

    public NotificationPreferencesDTO() {}

    public NotificationPreferencesDTO(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }
}
