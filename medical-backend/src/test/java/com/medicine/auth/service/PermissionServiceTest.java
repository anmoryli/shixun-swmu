/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.mapper.PermissionMapper;
import com.medicine.auth.model.PermissionRecord;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PermissionServiceTest {

    @Test
    void buildsFrontendRouteTree() {
        PermissionMapper mapper = mock(PermissionMapper.class);
        when(mapper.findByRoleName("ROLE_1")).thenReturn(Arrays.asList(
                record(1, 0, "Layout", "/", "Layout", "Layout"),
                record(2, 1, "Home", "/home", "Home", "首页"),
                record(11, 0, "error-404", "/*", "404", "404-页面不存在")
        ));

        List<PermissionNode> roots = new PermissionService(mapper).findPermissionTree("ROLE_1");

        assertThat(roots).hasSize(2);
        assertThat(roots.get(0).getName()).isEqualTo("Layout");
        assertThat(roots.get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getMeta().getTitle()).isEqualTo("首页");
    }

    @Test
    void loadsAccountPermissionsAndNormalizesAuthorities() {
        PermissionMapper mapper = mock(PermissionMapper.class);
        PermissionRecord child = record(2, 1, "Home", "/home", "Home", "首页");
        child.setSortOrder(20);
        PermissionRecord root = record(1, 0, "Layout", "/", "Layout", "Layout");
        root.setSortOrder(10);
        when(mapper.findByAccountId(7L)).thenReturn(Arrays.asList(child, root));
        when(mapper.findRoleCodes(7L)).thenReturn(Arrays.asList("ADMIN", "ROLE_AUDITOR", "", null));
        when(mapper.findPermissionCodes(7L)).thenReturn(Arrays.asList("doctor:read", "doctor:read"));

        PermissionService service = new PermissionService(mapper);

        assertThat(service.findPermissionTree(7L).get(0).getChildren()).hasSize(1);
        assertThat(service.findPermissionCodes(7L)).containsExactly("doctor:read", "doctor:read");
        assertThat(service.findRoleCodes(7L)).containsExactly("ADMIN", "ROLE_AUDITOR", "", null);
        assertThat(service.findAuthorities(7L))
                .containsExactly("ROLE_ADMIN", "ROLE_AUDITOR", "doctor:read");
    }

    @Test
    void treatsMissingMapperResultsAsEmpty() {
        PermissionMapper mapper = mock(PermissionMapper.class);
        when(mapper.findRoleCodes(8L)).thenReturn(Collections.emptyList());

        PermissionService service = new PermissionService(mapper);

        assertThat(service.findPermissionTree(8L)).isEmpty();
        assertThat(service.findPermissionCodes(8L)).isEmpty();
        assertThat(service.findRoleCodes(8L)).isEmpty();
        assertThat(service.findAuthorities(8L)).isEmpty();
    }

    private PermissionRecord record(int id, int pid, String name, String path, String component, String title) {
        PermissionRecord record = new PermissionRecord();
        record.setId(id);
        record.setPid(pid);
        record.setName(name);
        record.setPath(path);
        record.setComponent(component);
        record.setTitle(title);
        return record;
    }
}
