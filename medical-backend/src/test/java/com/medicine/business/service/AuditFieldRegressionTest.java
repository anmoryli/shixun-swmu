/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import com.medicine.business.mapper.DrugMapper;
import com.medicine.business.mapper.MaterialMapper;
import com.medicine.security.AuthSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试:登录操作者账号 id 应通过 AuditSupport 流入 create_by/update_by,
 * 防止审计字段被静默回归为空,导致数据改动无法追溯责任人。
 */
class AuditFieldRegressionTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long accountId) {
        AuthSession session = new AuthSession();
        session.setUserId(accountId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(session, null, Collections.emptyList()));
    }

    @Test
    void drugAddAndUpdateCarryOperatorAccountId() {
        DrugMapper mapper = mock(DrugMapper.class);
        doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("drugId", 7L);
            return 1;
        }).when(mapper).insertDrug(anyMap());
        when(mapper.count(any())).thenReturn(6L);
        DrugService service = new DrugService(mapper);

        loginAs(42L);
        Map<String, Object> addRequest = new LinkedHashMap<>();
        addRequest.put("drugName", "阿莫西林");
        service.add(addRequest, 5);

        ArgumentCaptor<Map<String, Object>> insertCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertDrug(insertCaptor.capture());
        assertThat(insertCaptor.getValue()).containsEntry("createBy", 42L);

        service.update(7L, Map.of("drugName", "阿莫西林胶囊"));
        ArgumentCaptor<Map<String, Object>> updateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateDrug(updateCaptor.capture());
        assertThat(updateCaptor.getValue()).containsEntry("updateBy", 42L);
    }

    @Test
    void materialAddAndUpdateCarryOperatorAccountId() {
        MaterialMapper mapper = mock(MaterialMapper.class);
        when(mapper.count(any())).thenReturn(1L);
        MaterialService service = new MaterialService(mapper);

        loginAs(99L);
        service.add(Map.of("title", " t ", "message", " m "), 5);
        verify(mapper).insert(eq("t"), eq("m"), eq(99L));

        service.update(1L, Map.of("title", " t2 ", "message", " m2 "));
        verify(mapper).update(eq(1L), eq("t2"), eq("m2"), eq(99L));
    }
}
