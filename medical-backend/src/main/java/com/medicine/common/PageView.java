/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

import java.util.Collections;
import java.util.List;

public class PageView<T extends Object> {

    private long total;
    private List<T> list;
    private int pageNum;
    private int pageSize;

    public PageView() {
        this(0, Collections.emptyList(), 0, 0);
    }

    public PageView(long total, List<T> list, int pageNum, int pageSize) {
        this.total = total;
        this.list = list == null ? Collections.emptyList() : list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
