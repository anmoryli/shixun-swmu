/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录 cookie 配置。token 不再返回前端，而是通过 httpOnly cookie 下发，
 * 浏览器自动携带、前端 JS 读不到，从而实现持久登录且前端不存储 token。
 */
@Component
@ConfigurationProperties(prefix = "app.auth.cookie")
public class CookieProperties {

    private String name = "medicine_token";
    private String domain = "";
    private String path = "/";
    private boolean secure = false;
    private String sameSite = "lax";
    private Duration maxAge = Duration.ofDays(7);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }
}
