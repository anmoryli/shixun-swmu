package com.medicine.business.service;

import com.medicine.business.mapper.MedicalPolicyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String updateTime = dateOrToday(request.get("updateTime"));
        mapper.insert(PageSupport.longValue(request.get("cityId")),
                PageSupport.stringValue(request.get("title")), updateTime,
                PageSupport.stringValue(request.get("message")));
        return PageSupport.pages(mapper.count(null, null, null, null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        mapper.update(id, PageSupport.longValue(request.get("cityId")),
                PageSupport.stringValue(request.get("title")), dateOrToday(request.get("updateTime")),
                PageSupport.stringValue(request.get("message")));
    }

    @Transactional
    public void delete(Long id) {
        mapper.delete(id);
    }

    private static String dateOrToday(Object value) {
        String date = PageSupport.stringValue(value);
        return date == null || date.isBlank() ? LocalDate.now().toString() : date;
    }
}
