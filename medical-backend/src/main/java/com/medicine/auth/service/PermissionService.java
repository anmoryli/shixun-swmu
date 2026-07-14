/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.service;

import com.medicine.auth.dto.PermissionMeta;
import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.mapper.PermissionMapper;
import com.medicine.auth.model.PermissionRecord;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<PermissionNode> findPermissionTree(String roleName) {
        List<PermissionRecord> records = permissionMapper.findByRoleName(roleName);
        Map<Integer, PermissionNode> nodes = new LinkedHashMap<>();
        for (PermissionRecord record : records) {
            PermissionNode node = new PermissionNode();
            node.setName(record.getName());
            node.setPath(record.getPath());
            node.setComponent(record.getComponent());
            node.setMeta(new PermissionMeta(record.getTitle()));
            nodes.put(record.getId(), node);
        }

        List<PermissionNode> roots = new ArrayList<>();
        for (PermissionRecord record : records) {
            PermissionNode node = nodes.get(record.getId());
            PermissionNode parent = nodes.get(record.getPid());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
