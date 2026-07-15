/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.service;

import com.medicine.auth.dto.LoginResult;
import com.medicine.auth.dto.UserInfo;
import com.medicine.auth.mapper.AccountMapper;
import com.medicine.auth.model.Account;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuthSession;
import com.medicine.security.LoginAttemptService;
import com.medicine.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    /**
     * 用于对不存在用户名执行一次恒定时间 BCrypt 比对，消除登录响应时延差异，防止用户名枚举。
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PermissionService permissionService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this(accountMapper, passwordEncoder, tokenService, null, null);
    }

    @Autowired
    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, TokenService tokenService,
                       PermissionService permissionService, LoginAttemptService loginAttemptService) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.permissionService = permissionService;
        this.loginAttemptService = loginAttemptService;
    }

    public LoginResult login(String username, String password) {
        String trimmed = username == null ? "" : username.trim();
        if (loginAttemptService != null && loginAttemptService.isLocked(trimmed)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号已锁定，请稍后再试");
        }
        Account account = accountMapper.findByUsername(trimmed);
        boolean matched = account != null && isActive(account) && account.getPwd() != null
                && passwordEncoder.matches(password, account.getPwd());
        if (!matched) {
            // 恒定时间防用户名枚举：对不存在用户也执行一次 BCrypt 比对，抹平响应时延。
            if (account == null) {
                passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            }
            if (loginAttemptService != null) {
                loginAttemptService.recordFailure(trimmed);
            }
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        int userType = toNumericUserType(account.getUtype());
        // 登录时预计算 authorities 并随会话持久化,认证过滤器直接读取,免去每请求多表 JOIN。
        // permissionService 为 null(测试/兼容构造)时跳过,过滤器将回退到按请求查询。
        List<String> authorities = permissionService != null
                ? permissionService.findAuthorities(account.getId())
                : null;
        if (permissionService != null && (authorities == null || authorities.isEmpty())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        if (loginAttemptService != null) {
            loginAttemptService.clear(trimmed);
        }
        AuthSession session = new AuthSession(
                account.getId(), account.getUname(), account.getRealname(), account.getUtype(), userType,
                account.getPhonenumber()
        );
        session.setAuthorities(authorities);
        String token = tokenService.create(session);
        UserInfo userInfo = new UserInfo(
                account.getId(), account.getRealname(), account.getUname(), account.getPhonenumber(), userType
        );
        return new LoginResult(token, userInfo);
    }

    private boolean isActive(Account account) {
        return account != null && Integer.valueOf(1).equals(account.getStatus());
    }

    static int toNumericUserType(String roleName) {
        if (roleName != null && roleName.startsWith("ROLE_")) {
            try {
                int value = Integer.parseInt(roleName.substring("ROLE_".length()));
                if (value >= 1 && value <= 3) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "账号角色配置错误");
    }
}
