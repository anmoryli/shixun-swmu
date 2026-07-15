/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.CompanyMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CompanyService {
    private final CompanyMapper mapper;

    public CompanyService(CompanyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(Integer pn, Integer size, String name) {
        int pageNumber = PageSupport.pageNumber(pn);
        int pageSize = PageSupport.pageSize(size);
        long total = mapper.count(name);
        return PageSupport.page(mapper.page(name, PageSupport.offset(pageNumber, pageSize), pageSize),
                total, pageNumber, pageSize);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> all() {
        return PageSupport.all(mapper.findAll());
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        String companyName = PageSupport.stringValue(request.get("companyName")).orElse(null);
        if (companyName == null || companyName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "公司名称不能为空");
        }
        mapper.insert(companyName, PageSupport.stringValue(request.get("companyPhone")).orElse(null));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        mapper.update(id, PageSupport.stringValue(request.get("companyName")).orElse(null),
                PageSupport.stringValue(request.get("companyPhone")).orElse(null));
    }

    @Transactional
    public void delete(Long id) {
        mapper.delete(id);
    }
}
