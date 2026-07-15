/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class PageSupportTest {

    @Test
    void normalizesPageNumberAndSize() {
        assertThat(PageSupport.pageNumber(null)).isEqualTo(1);
        assertThat(PageSupport.pageNumber(0)).isEqualTo(1);
        assertThat(PageSupport.pageNumber(3)).isEqualTo(3);
        assertThat(PageSupport.pageSize(null)).isEqualTo(5);
        assertThat(PageSupport.pageSize(0)).isEqualTo(5);
        assertThat(PageSupport.pageSize(20)).isEqualTo(20);
        assertThat(PageSupport.pageSize(1000)).isEqualTo(PageSupport.MAX_PAGE_SIZE);
    }

    @Test
    void calculatesOffsetAndPageCount() {
        assertThat(PageSupport.offset(1, 5)).isZero();
        assertThat(PageSupport.offset(3, 5)).isEqualTo(10);
        assertThat(PageSupport.pages(0, 5)).isZero();
        assertThat(PageSupport.pages(1, 5)).isEqualTo(1);
        assertThat(PageSupport.pages(11, 5)).isEqualTo(3);
    }

    @Test
    void createsStablePaginationMetadata() {
        Map<String, Object> page = PageSupport.page(List.of("a", "b"), 12, 2, 5);

        assertThat(page).containsEntry("list", List.of("a", "b"))
                .containsEntry("total", 12L)
                .containsEntry("pages", 3)
                .containsEntry("pageNum", 2)
                .containsEntry("pageSize", 5)
                .containsEntry("size", 2)
                .containsEntry("hasPreviousPage", true)
                .containsEntry("hasNextPage", true)
                .containsEntry("isFirstPage", false)
                .containsEntry("isLastPage", false);
    }

    @Test
    void marksEmptyAndLastPagesCorrectly() {
        assertThat(PageSupport.page(List.of(), 0, 1, 5))
                .containsEntry("isFirstPage", true)
                .containsEntry("isLastPage", true)
                .containsEntry("hasNextPage", false);
        assertThat(PageSupport.page(List.of("last"), 11, 3, 5))
                .containsEntry("isLastPage", true)
                .containsEntry("hasPreviousPage", true);
    }

    @Test
    void createsAnAllItemsPageWithoutZeroPageSize() {
        assertThat(PageSupport.all(List.of()))
                .containsEntry("pageSize", 1)
                .containsEntry("total", 0L);
        assertThat(PageSupport.all(List.of("a", "b")))
                .containsEntry("pageSize", 2)
                .containsEntry("total", 2L);
    }

    @Test
    void convertsOptionalRequestValues() {
        assertThat(PageSupport.longValue(null)).isEmpty();
        assertThat(PageSupport.longValue(" ")).isEmpty();
        assertThat(PageSupport.longValue("42")).hasValue(42L);
        assertThat(PageSupport.longValue(7)).hasValue(7L);

        assertThat(PageSupport.intValue(9L)).hasValue(9);
        assertThat(PageSupport.intValue("10")).hasValue(10);
        assertThat(PageSupport.intValue("")).isEmpty();

        assertThat(PageSupport.stringValue(null)).isEmpty();
        assertThat(PageSupport.stringValue(" value ")).hasValue("value");

        assertThat(PageSupport.decimalValue(null)).isEmpty();
        assertThat(PageSupport.decimalValue("12.50")).hasValue(new BigDecimal("12.50"));
        BigDecimal decimal = new BigDecimal("3.14");
        assertThat(PageSupport.decimalValue(decimal)).hasValue(decimal);
    }
}
