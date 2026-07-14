/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.dto;

import java.util.ArrayList;
import java.util.List;

public class PermissionNode {

    private String path;
    private String name;
    private String component;
    private PermissionMeta meta;
    private List<PermissionNode> children = new ArrayList<>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public PermissionMeta getMeta() {
        return meta;
    }

    public void setMeta(PermissionMeta meta) {
        this.meta = meta;
    }

    public List<PermissionNode> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionNode> children) {
        this.children = children;
    }
}
