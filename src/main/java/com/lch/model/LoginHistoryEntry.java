package com.lch.model;

import java.sql.Timestamp;

public class LoginHistoryEntry {
    private Timestamp loginTime;

    public LoginHistoryEntry(Timestamp loginTime) {
        this.loginTime = loginTime;
    }

    public Timestamp getLoginTime() {
        return loginTime;
    }
}
