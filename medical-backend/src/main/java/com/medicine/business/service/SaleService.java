/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.SaleMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuditSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SaleService {
    private final SaleMapper mapper;

    public SaleService(SaleMapper mapper) {
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
        String saleName = PageSupport.stringValue(request.get("saleName")).orElse(null);
        if (saleName == null || saleName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "销售地点名称不能为空");
        }
        mapper.insert(values(null, request));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        mapper.update(values(id, request));
    }

    @Transactional
    public void delete(Long id) {
        // 软删除销售点,保留 drug_sale 关联以便恢复;drug 查询侧过滤已删销售点。
        mapper.softDelete(id, AuditSupport.currentAccountId());
    }

    private Map<String, Object> values(Long id, Map<String, Object> request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("saleId", id);
        values.put("saleName", PageSupport.stringValue(request.get("saleName")).orElse(null));
        values.put("salePhone", PageSupport.stringValue(request.get("salePhone")).orElse(null));
        values.put("address", PageSupport.stringValue(request.get("address")).orElse(null));
        values.put("longitude", PageSupport.decimalValue(request.get("longitude")).orElse(null));
        values.put("latitude", PageSupport.decimalValue(request.get("latitude")).orElse(null));
        Long operator = AuditSupport.currentAccountId();
        if (id == null) {
            values.put("createBy", operator);
        } else {
            values.put("updateBy", operator);
        }
        return values;
    }
}
