/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.DoctorMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.TokenService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DoctorService {
    private final DoctorMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public DoctorService(DoctorMapper mapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(Integer pn, Integer size, String keyword) {
        int pageNumber = PageSupport.pageNumber(pn);
        int pageSize = PageSupport.pageSize(size);
        long total = mapper.count(keyword);
        return PageSupport.page(mapper.page(keyword, PageSupport.offset(pageNumber, pageSize), pageSize),
                total, pageNumber, pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> levelAndType() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("allLevel", mapper.findAllLevels());
        result.put("allTreatType", mapper.findAllTreatTypes());
        return result;
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        String pwd = PageSupport.stringValue(request.get("pwd")).orElse(null);
        if (pwd == null || pwd.length() < 6 || !pwd.matches(".*[A-Za-z].*") || !pwd.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "密码至少 6 位且需同时包含字母与数字");
        }
        String phone = PageSupport.stringValue(request.get("phoneNumber")).orElse(null);
        if (phone != null && mapper.countPhone(phone) > 0) {
            return -1;
        }
        String name = PageSupport.stringValue(request.get("name")).orElse(null);
        String username = uniqueUsername(name, phone);
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("realname", name);
        account.put("uname", username);
        account.put("pwd", passwordEncoder.encode(pwd));
        account.put("phoneNumber", phone);
        mapper.insertAccount(account);
        Object generatedAccountId = account.get("id");
        Long accountId = generatedAccountId instanceof Number
                ? ((Number) generatedAccountId).longValue() : null;
        if (accountId == null || mapper.bindDoctorRole(accountId) == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "医生角色初始化失败");
        }

        Map<String, Object> doctor = new LinkedHashMap<>(request);
        doctor.put("name", name);
        doctor.put("phoneNumber", phone);
        doctor.put("accountId", accountId);
        mapper.insertDoctor(doctor);
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public boolean update(Long id, Map<String, Object> request) {
        Long accountId = mapper.findAccountId(id);
        if (accountId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "医生信息不存在");
        }
        String phone = PageSupport.stringValue(request.get("phoneNumber")).orElse(null);
        if (phone != null && mapper.countPhoneExcept(phone, accountId) > 0) {
            return false;
        }
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("id", id);
        values.put("accountId", accountId);
        values.put("phoneNumber", phone);
        values.put("name", PageSupport.stringValue(request.get("name")).orElse(null));
        mapper.updateDoctor(values);
        mapper.updateAccount(values);
        return true;
    }

    @Transactional
    public void delete(Long id) {
        Long accountId = mapper.findAccountId(id);
        mapper.deleteDoctor(id);
        if (accountId != null) {
            // 账号删除前先失效其所有会话,防止旧 token 在 TTL 内继续访问
            tokenService.invalidateByAccountId(accountId);
            mapper.deleteAccount(accountId);
        }
    }

    @Transactional
    public String resetPassword(Long accountId, Long operatorAccountId) {
        String tempPassword = generateTempPassword();
        if (mapper.resetPassword(accountId, passwordEncoder.encode(tempPassword)) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "医生账号不存在");
        }
        mapper.insertPasswordResetAudit(accountId, operatorAccountId);
        // 密码已变更,失效该账号所有现有会话,强制重新登录
        tokenService.invalidateByAccountId(accountId);
        return tempPassword;
    }

    /**
     * 生成 8 位随机临时密码（数字+字母，去除易混淆字符），避免使用固定弱口令 "123456"。
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private String uniqueUsername(String name, String phone) {
        String suffix = phone != null && phone.length() >= 4 ? phone.substring(phone.length() - 4) : "0000";
        String base = (name == null || name.isBlank() ? "医生" : name) + suffix;
        String username = base;
        int sequence = 1;
        while (mapper.countUsername(username) > 0) {
            username = base + sequence++;
        }
        return username;
    }
}
