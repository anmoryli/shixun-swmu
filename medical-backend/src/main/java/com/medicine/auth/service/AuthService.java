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
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(AccountMapper accountMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResult login(String username, String password) {
        Account account = accountMapper.findByUsername(username.trim());
        if (account == null || account.getPwd() == null || !passwordEncoder.matches(password, account.getPwd())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        int userType = toNumericUserType(account.getUtype());
        AuthSession session = new AuthSession(
                account.getId(), account.getUname(), account.getRealname(), account.getUtype(), userType
        );
        String token = tokenService.create(session);
        UserInfo userInfo = new UserInfo(
                account.getId(), account.getRealname(), account.getUname(), account.getPhonenumber(), userType
        );
        return new LoginResult(token, userInfo);
    }

    static int toNumericUserType(String roleName) {
        if (roleName != null && roleName.startsWith("ROLE_")) {
            try {
                return Integer.parseInt(roleName.substring("ROLE_".length()));
            } catch (NumberFormatException ignored) {
                // handled below
            }
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "账号角色配置错误");
    }
}
