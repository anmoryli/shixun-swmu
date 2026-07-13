package com.medicine.business.service;

import com.medicine.business.mapper.DrugMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            List<Long> ids = rows.stream().map(row -> PageSupport.longValue(row.get("drugId"))).toList();
            Map<Long, List<Map<String, Object>>> sales = mapper.findSales(ids).stream()
                    .collect(Collectors.groupingBy(row -> PageSupport.longValue(row.remove("drugId")),
                            LinkedHashMap::new, Collectors.toList()));
            rows.forEach(row -> row.put("drugSales",
                    sales.getOrDefault(PageSupport.longValue(row.get("drugId")), List.of())));
        }
        return PageSupport.page(rows, total, pageNumber, pageSize);
    }

    @Transactional
    public int add(Map<String, Object> request, int pageSize) {
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("drugName", PageSupport.stringValue(request.get("drugName")));
        values.put("drugInfo", PageSupport.stringValue(request.get("drugInfo")));
        values.put("drugEffect", PageSupport.stringValue(request.get("drugEffect")));
        values.put("drugImg", PageSupport.stringValue(request.get("drugImg")));
        values.put("drugPublisher", PageSupport.stringValue(request.get("drugPublisher")));
        mapper.insertDrug(values);
        Long drugId = PageSupport.longValue(values.get("drugId"));
        replaceSales(drugId, request.get("saleIds"));
        return PageSupport.pages(mapper.count(null), PageSupport.pageSize(pageSize));
    }

    @Transactional
    public void update(Long id, Map<String, Object> request) {
        Map<String, Object> values = new LinkedHashMap<>(request);
        values.put("drugId", id);
        mapper.updateDrug(values);
        replaceSales(id, request.get("saleIds"));
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
                Long id = PageSupport.longValue(item);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
