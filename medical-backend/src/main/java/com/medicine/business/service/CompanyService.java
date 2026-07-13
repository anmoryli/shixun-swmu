package com.medicine.business.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medicine.business.mapper.CompanyMapper;

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
        mapper.insert(PageSupport.stringValue(request.get("companyName")),
                PageSupport.stringValue(request.get("companyPhone")));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        mapper.update(id, PageSupport.stringValue(request.get("companyName")),
                PageSupport.stringValue(request.get("companyPhone")));
    }

    @Transactional
    public void delete(Long id) {
        mapper.delete(id);
    }
}
