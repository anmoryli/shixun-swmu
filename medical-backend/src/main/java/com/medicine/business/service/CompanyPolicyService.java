/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.CompanyPolicyMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Service
public class CompanyPolicyService {
    private final CompanyPolicyMapper mapper;

    public CompanyPolicyService(CompanyPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(Integer pn, Integer size, String keyword) {
        int pageNumber = PageSupport.pageNumber(pn);
        int pageSize = PageSupport.pageSize(size);
        long total = mapper.count(keyword);
        List<Map<String, Object>> rows = mapper.page(keyword, PageSupport.offset(pageNumber, pageSize), pageSize);
        for (Map<String, Object> row : rows) {
            Map<String, Object> company = new LinkedHashMap<>();
            company.put("companyId", row.remove("companyId"));
            company.put("companyName", row.remove("companyName"));
            row.put("drugCompanyModel", company);
        }
        return PageSupport.page(rows, total, pageNumber, pageSize);
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        String title = PageSupport.stringValue(request.get("title")).orElse(null);
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "政策标题不能为空");
        }
        OptionalLong companyId = PageSupport.longValue(request.get("companyId"));
        mapper.insert(companyId.isPresent() ? companyId.getAsLong() : null,
                title, PageSupport.stringValue(request.get("message")).orElse(null));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        OptionalLong companyId = PageSupport.longValue(request.get("companyId"));
        mapper.update(id, companyId.isPresent() ? companyId.getAsLong() : null,
                PageSupport.stringValue(request.get("title")).orElse(null), PageSupport.stringValue(request.get("message")).orElse(null));
    }

    @Transactional
    public void delete(Long id) {
        mapper.delete(id);
    }
}
