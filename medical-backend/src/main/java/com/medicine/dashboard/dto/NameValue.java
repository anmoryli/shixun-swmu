package com.medicine.dashboard.dto;

public class NameValue {

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
