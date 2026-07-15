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
import com.medicine.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PermissionService permissionService;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this(accountMapper, passwordEncoder, tokenService, null);
    }

    @Autowired
    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, TokenService tokenService,
                       PermissionService permissionService) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.permissionService = permissionService;
    }

    public LoginResult login(String username, String password) {
        Account account = accountMapper.findByUsername(username.trim());
        if (account == null || !isActive(account) || account.getPwd() == null
                || !passwordEncoder.matches(password, account.getPwd())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        int userType = toNumericUserType(account.getUtype());
        if (permissionService != null) {
            List<String> roles = permissionService.findRoleCodes(account.getId());
            if (roles == null || roles.isEmpty()) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
            }
        }
        AuthSession session = new AuthSession(
                account.getId(), account.getUname(), account.getRealname(), account.getUtype(), userType,
                account.getPhonenumber()
        );
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
        if (roleName != null && roleName.matches("ROLE_[1-3]")) {
            return roleName.charAt(roleName.length() - 1) - '0';
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "账号角色配置错误");
    }
}
