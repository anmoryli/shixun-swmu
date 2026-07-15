/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.mapper;

import com.medicine.auth.model.PermissionRecord;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PermissionMapper {

    @Select("SELECT DISTINCT p.id, p.pid, p.name, p.path, p.component, p.level, p.title, "
            + "p.sort_order AS sortOrder "
            + "FROM account a "
            + "JOIN account_role ar ON ar.account_id = a.id "
            + "JOIN rbac_role r ON r.id = ar.role_id AND r.enabled = 1 "
            + "JOIN rbac_role_permission rrp ON rrp.role_id = r.id "
            + "JOIN permission p ON p.id = rrp.permission_id "
            + "AND p.permission_type = 'MENU' AND p.enabled = 1 "
            + "WHERE a.id = #{accountId} AND a.status = 1 "
            + "ORDER BY p.sort_order, p.id")
    List<PermissionRecord> findByAccountId(@Param("accountId") Long accountId);

    @Select("SELECT DISTINCT p.code "
            + "FROM account a "
            + "JOIN account_role ar ON ar.account_id = a.id "
            + "JOIN rbac_role r ON r.id = ar.role_id AND r.enabled = 1 "
            + "JOIN rbac_role_permission rrp ON rrp.role_id = r.id "
            + "JOIN permission p ON p.id = rrp.permission_id "
            + "AND p.permission_type = 'ACTION' AND p.enabled = 1 "
            + "WHERE a.id = #{accountId} AND a.status = 1 ORDER BY p.code")
    List<String> findPermissionCodes(@Param("accountId") Long accountId);

    @Select("SELECT DISTINCT r.code "
            + "FROM account a "
            + "JOIN account_role ar ON ar.account_id = a.id "
            + "JOIN rbac_role r ON r.id = ar.role_id AND r.enabled = 1 "
            + "WHERE a.id = #{accountId} AND a.status = 1 ORDER BY r.code")
    List<String> findRoleCodes(@Param("accountId") Long accountId);

    /** Legacy menu lookup retained for old unit-test/API compatibility. */
    @Select("SELECT p.id, p.pid, p.name, p.path, p.component, p.level, p.title "
            + "FROM permission p JOIN role_permission rp ON rp.per_id = p.id "
            + "WHERE rp.roleName = #{roleName} ORDER BY p.id")
    List<PermissionRecord> findByRoleName(@Param("roleName") String roleName);
}
