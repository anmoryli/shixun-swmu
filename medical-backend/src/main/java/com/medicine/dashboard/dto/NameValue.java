/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.dto;

import java.io.Serializable;

public class NameValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private long value;

    public NameValue() {
    }

    public NameValue(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
