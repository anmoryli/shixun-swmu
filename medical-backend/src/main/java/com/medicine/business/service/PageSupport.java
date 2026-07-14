/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

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

    static OptionalLong longValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value instanceof Number number ? number.longValue() : Long.valueOf(value.toString()));
    }

    static OptionalInt intValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(value instanceof Number number ? number.intValue() : Integer.valueOf(value.toString()));
    }

    static Optional<String> stringValue(Object value) {
        return value == null ? Optional.empty() : Optional.of(value.toString().trim());
    }

    static Optional<BigDecimal> decimalValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()));
    }
}
