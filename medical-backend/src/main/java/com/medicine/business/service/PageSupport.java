package com.medicine.business.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

final class PageSupport {
    static final int MAX_PAGE_SIZE = 200;

    private PageSupport() {
    }

    static int pageNumber(Integer pageNumber) {
        return pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
    }

    static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 5;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    static int offset(int pageNumber, int pageSize) {
        return (pageNumber - 1) * pageSize;
    }

    static int pages(long total, int pageSize) {
        return total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
    }

    static <T> Map<String, Object> page(List<T> list, long total, int pageNumber, int pageSize) {
        int pages = pages(total, pageSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pages", pages);
        result.put("pageNum", pageNumber);
        result.put("pageSize", pageSize);
        result.put("size", list.size());
        result.put("hasPreviousPage", pageNumber > 1);
        result.put("hasNextPage", pageNumber < pages);
        result.put("isFirstPage", pageNumber <= 1);
        result.put("isLastPage", pages == 0 || pageNumber >= pages);
        return result;
    }

    static <T> Map<String, Object> all(List<T> list) {
        return page(list, list.size(), 1, Math.max(list.size(), 1));
    }

    static Long longValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    static Integer intValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString());
    }

    static String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }

    static BigDecimal decimalValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
