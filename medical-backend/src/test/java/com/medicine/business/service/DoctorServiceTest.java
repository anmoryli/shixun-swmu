/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medicine.business.mapper.DoctorMapper;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.TokenService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

class DoctorServiceTest {

    @Test
    void pageNormalizesBoundsAndReturnsMetadata() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        when(mapper.count("李")).thenReturn(11L);
        when(mapper.page("李", 0, 200)).thenReturn(List.of(Map.of("id", 1)));

        Map<String, Object> result = new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class))
                .page(0, 999, "李");

        assertThat(result).containsEntry("total", 11L)
                .containsEntry("pages", 1)
                .containsEntry("pageNum", 1)
                .containsEntry("pageSize", 200);
    }

    @Test
    void returnsDoctorDictionaryData() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        List<Map<String, Object>> levels = List.of(Map.of("id", 1));
        List<Map<String, Object>> types = List.of(Map.of("id", 2));
        when(mapper.findAllLevels()).thenReturn(levels);
        when(mapper.findAllTreatTypes()).thenReturn(types);

        assertThat(new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class)).levelAndType())
                .containsEntry("allLevel", levels)
                .containsEntry("allTreatType", types);
    }

    @Test
    void addRejectsDuplicatePhoneWithoutWriting() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        when(mapper.countPhone("15900000000")).thenReturn(1L);
        DoctorService service = new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class));

        assertThat(service.add(Map.of("phoneNumber", "15900000000", "pwd", "pass123", "name", "测试"), 5)).isEqualTo(-1);

        verify(mapper, never()).insertAccount(anyMap());
        verify(mapper, never()).insertDoctor(anyMap());
    }

    @Test
    void addCreatesUniqueAccountAndDoctorThenReturnsPages() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("initial123")).thenReturn("encoded-password");
        when(mapper.countUsername("李医生0000")).thenReturn(1L);
        when(mapper.countUsername("李医生00001")).thenReturn(0L);
        when(mapper.count(null)).thenReturn(6L);
        doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("id", 10L);
            return 1;
        }).when(mapper).insertAccount(anyMap());
        when(mapper.bindDoctorRole(anyLong())).thenReturn(1);
        DoctorService service = new DoctorService(mapper, encoder, mock(TokenService.class));
        Map<String, Object> request = Map.of(
                "name", " 李医生 ", "phoneNumber", "15900000000", "pwd", "initial123");

        assertThat(service.add(request, 5)).isEqualTo(2);

        ArgumentCaptor<Map<String, Object>> account = mapCaptor();
        verify(mapper).insertAccount(account.capture());
        assertThat(account.getValue()).containsEntry("realname", "李医生")
                .containsEntry("uname", "李医生00001")
                .containsEntry("pwd", "encoded-password")
                .containsEntry("phoneNumber", "15900000000");
        ArgumentCaptor<Map<String, Object>> doctor = mapCaptor();
        verify(mapper).insertDoctor(doctor.capture());
        assertThat(doctor.getValue()).containsEntry("name", "李医生")
                .containsEntry("phoneNumber", "15900000000");
    }

    @Test
    void updateRejectsMissingDoctorAndDuplicatePhone() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        DoctorService service = new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class));
        when(mapper.findAccountId(10L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(10L, Map.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND);

        when(mapper.findAccountId(10L)).thenReturn(20L);
        when(mapper.countPhoneExcept("15900000000", 20L)).thenReturn(1L);
        assertThat(service.update(10L, Map.of("phoneNumber", "15900000000"))).isFalse();
        verify(mapper, never()).updateDoctor(anyMap());
    }

    @Test
    void updateWritesNormalizedDoctorAndAccountData() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        when(mapper.findAccountId(10L)).thenReturn(20L);
        DoctorService service = new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class));

        assertThat(service.update(10L, Map.of("name", " 李医生 ", "phoneNumber", "15900000000")))
                .isTrue();

        ArgumentCaptor<Map<String, Object>> values = mapCaptor();
        verify(mapper).updateDoctor(values.capture());
        verify(mapper).updateAccount(values.getValue());
        assertThat(values.getValue()).containsEntry("id", 10L)
                .containsEntry("accountId", 20L)
                .containsEntry("name", "李医生");
    }

    @Test
    void updateKeepsOnlyProvidedFieldsSoMissingOnesAreNotNulled() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        when(mapper.findAccountId(10L)).thenReturn(20L);
        DoctorService service = new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class));

        // 仅改手机号,未携带 age/sex/levelId/typeId:动态 <set> 应跳过这些字段,不清空原值
        assertThat(service.update(10L, Map.of("phoneNumber", "13900000000"))).isTrue();

        ArgumentCaptor<Map<String, Object>> values = mapCaptor();
        verify(mapper).updateDoctor(values.capture());
        assertThat(values.getValue()).containsEntry("id", 10L)
                .containsEntry("accountId", 20L)
                .containsEntry("phoneNumber", "13900000000")
                .doesNotContainKey("age")
                .doesNotContainKey("sex")
                .doesNotContainKey("levelId")
                .doesNotContainKey("typeId");
    }

    @Test
    void deleteSoftDeletesDoctorAndDisablesAccount() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        DoctorService service = new DoctorService(mapper, encoder, tokenService);
        when(mapper.findAccountId(51L)).thenReturn(91L);

        service.delete(51L);

        verify(mapper).softDeleteDoctor(eq(51L), any());
        verify(tokenService).invalidateByAccountId(91L);
        verify(mapper).disableAccount(91L);
    }

    @Test
    void deleteSoftDeletesDoctorWithoutAccount() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        when(mapper.findAccountId(51L)).thenReturn((Long) null);
        new DoctorService(mapper, mock(PasswordEncoder.class), mock(TokenService.class)).delete(51L);

        verify(mapper).softDeleteDoctor(eq(51L), any());
        verify(mapper, never()).disableAccount(anyLong());
    }

    @Test
    void resetPasswordWritesAuditOnlyAfterSuccessfulUpdate() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("reset-hash");
        when(mapper.resetPassword(20L, "reset-hash")).thenReturn(1);
        DoctorService service = new DoctorService(mapper, encoder, mock(TokenService.class));

        service.resetPassword(20L, 1L);

        verify(mapper).insertPasswordResetAudit(20L, 1L);
    }

    @Test
    void resetPasswordRejectsMissingAccountWithoutAudit() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("reset-hash");
        DoctorService service = new DoctorService(mapper, encoder, mock(TokenService.class));

        assertThatThrownBy(() -> service.resetPassword(20L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(mapper, never()).insertPasswordResetAudit(20L, 1L);
    }

    @Test
    void resetPasswordReturnsEightCharTempPasswordFromSafeAlphabet() {
        // G.OTH.01:临时密码须由密码学安全随机数生成,长度 8 且取自去除易混淆字符的安全字母表
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("reset-hash");
        when(mapper.resetPassword(20L, "reset-hash")).thenReturn(1);
        DoctorService service = new DoctorService(mapper, encoder, mock(TokenService.class));

        String tempPassword = service.resetPassword(20L, 1L);

        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        assertThat(tempPassword).hasSize(8);
        for (char c : tempPassword.toCharArray()) {
            assertThat(alphabet.indexOf(c)).isGreaterThan(-1);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
