/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import java.util.List;

public class AuthSession {

    private Long userId;
    private String username;
    private String realname;
    private String roleName;
    private int userType;
    private String phonenumber;
    /**
     * 登录时预计算的 Spring authorities(ROLE_xxx + 权限码),序列化进 Redis 会话,
     * 认证过滤器直接读取,避免每个请求都做多表 JOIN 查权限。
     */
    private List<String> authorities;

    public AuthSession() {
    }

    public AuthSession(Long userId, String username, String realname, String roleName, int userType,
                       String phonenumber) {
        this.userId = userId;
        this.username = username;
        this.realname = realname;
        this.roleName = roleName;
        this.userType = userType;
        this.phonenumber = phonenumber;
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

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }
}
