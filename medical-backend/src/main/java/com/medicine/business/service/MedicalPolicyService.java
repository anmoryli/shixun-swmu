/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.MedicalPolicyMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuditSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Service
public class MedicalPolicyService {
    private final MedicalPolicyMapper mapper;

    public MedicalPolicyService(MedicalPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(Integer pn, Integer size, Long id, Long cityId,
                                    String title, String updateTime) {
        int pageNumber = PageSupport.pageNumber(pn);
        int pageSize = PageSupport.pageSize(size);
        long total = mapper.count(id, cityId, title, updateTime);
        List<Map<String, Object>> rows = mapper.page(id, cityId, title, updateTime,
                PageSupport.offset(pageNumber, pageSize), pageSize);
        for (Map<String, Object> row : rows) {
            Map<String, Object> city = new LinkedHashMap<>();
            city.put("cityId", row.remove("cityId"));
            city.put("cityNumber", row.remove("cityNumber"));
            city.put("province", row.remove("province"));
            city.put("city", row.remove("city"));
            row.put("cityModel", city);
        }
        return PageSupport.page(rows, total, pageNumber, pageSize);
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        String title = PageSupport.stringValue(request.get("title")).orElse(null);
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "政策标题不能为空");
        }
        String updateTime = dateOrToday(request.get("updateTime"));
        OptionalLong cityId = PageSupport.longValue(request.get("cityId"));
        mapper.insert(cityId.isPresent() ? cityId.getAsLong() : null,
                title, updateTime,
                PageSupport.stringValue(request.get("message")).orElse(null),
                AuditSupport.currentAccountId());
        return PageSupport.pages(mapper.count(null, null, null, null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        OptionalLong cityId = PageSupport.longValue(request.get("cityId"));
        mapper.update(id, cityId.isPresent() ? cityId.getAsLong() : null,
                PageSupport.stringValue(request.get("title")).orElse(null), dateOrToday(request.get("updateTime")),
                PageSupport.stringValue(request.get("message")).orElse(null),
                AuditSupport.currentAccountId());
    }

    @Transactional
    public void delete(Long id) {
        mapper.softDelete(id, AuditSupport.currentAccountId());
    }

    private static String dateOrToday(Object value) {
        String date = PageSupport.stringValue(value).orElse(null);
        return date == null || date.isBlank() ? LocalDate.now().toString() : date;
    }
}
