package com.medicine.business.service;

import com.medicine.business.mapper.DoctorMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DoctorService {
    public static final String RESET_PASSWORD = "123456";

    private final DoctorMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorMapper mapper, PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
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
        String phone = PageSupport.stringValue(request.get("phoneNumber"));
        if (phone != null && mapper.countPhone(phone) > 0) {
            return -1;
        }
        String name = PageSupport.stringValue(request.get("name"));
        String username = uniqueUsername(name, phone);
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("realname", name);
        account.put("uname", username);
        account.put("pwd", passwordEncoder.encode(PageSupport.stringValue(request.get("pwd"))));
        account.put("phoneNumber", phone);
        mapper.insertAccount(account);

        Map<String, Object> doctor = new LinkedHashMap<>(request);
        doctor.put("name", name);
        doctor.put("phoneNumber", phone);
        doctor.put("accountId", account.get("id"));
        mapper.insertDoctor(doctor);
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public boolean update(Long id, Map<String, Object> request) {
        Long accountId = mapper.findAccountId(id);
        if (accountId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "医生信息不存在");
        }
        String phone = PageSupport.stringValue(request.get("phoneNumber"));
        if (phone != null && mapper.countPhoneExcept(phone, accountId) > 0) {
            return false;
        }
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("id", id);
        values.put("accountId", accountId);
        values.put("phoneNumber", phone);
        values.put("name", PageSupport.stringValue(request.get("name")));
        mapper.updateDoctor(values);
        mapper.updateAccount(values);
        return true;
    }

    @Transactional
    public void delete(Long id) {
        Long accountId = mapper.findAccountId(id);
        mapper.deleteDoctor(id);
        if (accountId != null) {
            mapper.deleteAccount(accountId);
        }
    }

    @Transactional
    public void resetPassword(Long accountId, Long operatorAccountId) {
        if (mapper.resetPassword(accountId, passwordEncoder.encode(RESET_PASSWORD)) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "医生账号不存在");
        }
        mapper.insertPasswordResetAudit(accountId, operatorAccountId);
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
