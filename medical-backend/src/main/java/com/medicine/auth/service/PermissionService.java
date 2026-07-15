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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public List<PermissionNode> findPermissionTree(Long accountId) {
        return buildTree(safeRecords(permissionMapper.findByAccountId(accountId)));
    }

    public List<String> findPermissionCodes(Long accountId) {
        return safeStrings(permissionMapper.findPermissionCodes(accountId));
    }

    public List<String> findRoleCodes(Long accountId) {
        return safeStrings(permissionMapper.findRoleCodes(accountId));
    }

    /** Spring authorities: ROLE_<code> plus the normalized ACTION permission codes. */
    public List<String> findAuthorities(Long accountId) {
        List<String> roles = findRoleCodes(accountId);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                authorities.add(role.startsWith("ROLE_") ? role : "ROLE_" + role);
            }
        }
        authorities.addAll(findPermissionCodes(accountId));
        return new ArrayList<>(authorities);
    }

    public List<PermissionNode> findPermissionTree(String roleName) {
        return buildTree(safeRecords(permissionMapper.findByRoleName(roleName)));
    }

    private List<PermissionNode> buildTree(List<PermissionRecord> records) {
        records.sort(Comparator
                .comparingInt((PermissionRecord record) -> record.getSortOrder() == null ? 0 : record.getSortOrder())
                .thenComparingInt(record -> record.getId() == null ? Integer.MAX_VALUE : record.getId()));
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

    private List<PermissionRecord> safeRecords(List<PermissionRecord> records) {
        return records == null ? new ArrayList<>() : new ArrayList<>(records);
    }

    private List<String> safeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(values);
    }
}
