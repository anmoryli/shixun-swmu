/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.DrugMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;

@Service
public class DrugService {
    private final DrugMapper mapper;

    public DrugService(DrugMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> page(Integer pn, Integer size, String name) {
        int pageNumber = PageSupport.pageNumber(pn);
        int pageSize = PageSupport.pageSize(size);
        long total = mapper.count(name);
        List<Map<String, Object>> rows = mapper.page(name, PageSupport.offset(pageNumber, pageSize), pageSize);
        if (!rows.isEmpty()) {
            List<Long> ids = rows.stream().map(row -> {
                OptionalLong v = PageSupport.longValue(row.get("drugId"));
                return v.isPresent() ? v.getAsLong() : null;
            }).toList();
            Map<Long, List<Map<String, Object>>> sales = mapper.findSales(ids).stream()
                    .collect(Collectors.groupingBy(row -> {
                                OptionalLong v = PageSupport.longValue(row.remove("drugId"));
                                return v.isPresent() ? v.getAsLong() : null;
                            },
                            LinkedHashMap::new, Collectors.toList()));
            rows.forEach(row -> {
                OptionalLong v = PageSupport.longValue(row.get("drugId"));
                row.put("drugSales", sales.getOrDefault(v.isPresent() ? v.getAsLong() : null, List.of()));
            });
        }
        return PageSupport.page(rows, total, pageNumber, pageSize);
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        String drugName = PageSupport.stringValue(request.get("drugName")).orElse(null);
        if (drugName == null || drugName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "药品名称不能为空");
        }
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("drugName", drugName);
        values.put("drugInfo", PageSupport.stringValue(request.get("drugInfo")).orElse(null));
        values.put("drugEffect", PageSupport.stringValue(request.get("drugEffect")).orElse(null));
        values.put("drugImg", PageSupport.stringValue(request.get("drugImg")).orElse(null));
        values.put("drugPublisher", PageSupport.stringValue(request.get("drugPublisher")).orElse(null));
        mapper.insertDrug(values);
        OptionalLong drugIdOpt = PageSupport.longValue(values.get("drugId"));
        Long drugId = drugIdOpt.isPresent() ? drugIdOpt.getAsLong() : null;
        replaceSales(drugId, request.get("saleIds"));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("drugId", id);
        mapper.updateDrug(values);
        // 仅当请求显式携带 saleIds 字段时才重建销售关联,避免仅修改药品信息时误清空关联
        if (request.containsKey("saleIds")) {
            replaceSales(id, request.get("saleIds"));
        }
    }

    @Transactional
    public void delete(Long id) {
        mapper.deleteSaleRelations(id);
        mapper.deleteDrug(id);
    }

    private void replaceSales(Long drugId, Object rawSaleIds) {
        mapper.deleteSaleRelations(drugId);
        for (Long saleId : saleIds(rawSaleIds)) {
            mapper.insertSaleRelation(drugId, saleId);
        }
    }

    private List<Long> saleIds(Object value) {
        List<Long> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                OptionalLong idOpt = PageSupport.longValue(item);
                Long id = idOpt.isPresent() ? idOpt.getAsLong() : null;
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
