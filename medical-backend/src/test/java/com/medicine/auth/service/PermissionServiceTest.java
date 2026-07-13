package com.medicine.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.mapper.PermissionMapper;
import com.medicine.auth.model.PermissionRecord;

import java.util.Arrays;
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
