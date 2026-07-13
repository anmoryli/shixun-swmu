package com.medicine.business.service;

import com.medicine.business.mapper.CityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CityService {
    private final CityMapper mapper;

    public CityService(CityMapper mapper) {
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

    @Transactional(readOnly = true)
    public boolean exists(Integer cityNumber) {
        return cityNumber != null && mapper.countByNumber(cityNumber) > 0;
    }

    @Transactional
    public int add(Integer cityNumber, int pageSize) {
        mapper.insert(cityNumber);
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void delete(Long cityId) {
        mapper.delete(cityId);
    }
}
