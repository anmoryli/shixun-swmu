package com.medicine.business.service;

import com.medicine.business.mapper.SaleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.LinkedHashMap;

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
        mapper.insert(values(null, request));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        mapper.update(values(id, request));
    }

    @Transactional
    public void delete(Long id) {
        mapper.deleteDrugRelations(id);
        mapper.delete(id);
    }

    private Map<String, Object> values(Long id, Map<String, Object> request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("saleId", id);
        values.put("saleName", PageSupport.stringValue(request.get("saleName")));
        values.put("salePhone", PageSupport.stringValue(request.get("salePhone")));
        values.put("address", PageSupport.stringValue(request.get("address")));
        values.put("longitude", PageSupport.decimalValue(request.get("longitude")));
        values.put("latitude", PageSupport.decimalValue(request.get("latitude")));
        return values;
    }
}
