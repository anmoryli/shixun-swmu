/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 审计支持工具:从当前 SecurityContext 取出登录账号 id,供软删除 deleted_by、
 * 审计 create_by/update_by 等字段填充。无登录上下文(定时任务/测试)时返回 null。
 */
public final class AuditSupport {

    private AuditSupport() {
    }

    public static Long currentAccountId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof AuthSession ? ((AuthSession) principal).getUserId() : null;
    }
}
