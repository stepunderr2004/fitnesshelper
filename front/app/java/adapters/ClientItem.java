package com.example.fitnesshelper.adapters;

import com.example.fitnesshelper.db.User;

public class ClientItem {
    public boolean isHeader;
    public User user;
    public String headerTitle;

    public ClientItem(boolean isHeader, User user, String headerTitle) {
        this.isHeader = isHeader;
        this.user = user;
        this.headerTitle = headerTitle;
    }
}