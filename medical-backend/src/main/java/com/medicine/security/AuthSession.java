package com.medicine.security;

public class AuthSession {

    private Long userId;
    private String username;
    private String realname;
    private String roleName;
    private int userType;

    public AuthSession() {
    }

    public AuthSession(Long userId, String username, String realname, String roleName, int userType) {
        this.userId = userId;
        this.username = username;
        this.realname = realname;
        this.roleName = roleName;
        this.userType = userType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }
}
