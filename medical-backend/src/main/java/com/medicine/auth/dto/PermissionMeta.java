package com.medicine.auth.dto;

public class PermissionMeta {

    private String title;

    public PermissionMeta() {
    }

    public PermissionMeta(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
