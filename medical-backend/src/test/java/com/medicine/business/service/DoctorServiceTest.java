/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medicine.business.mapper.DoctorMapper;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DoctorServiceTest {

    @Test
    void deleteRemovesDoctorAndAccountInOneTransaction() {
        DoctorMapper mapper = mock(DoctorMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DoctorService service = new DoctorService(mapper, encoder);
        when(mapper.findAccountId(51L)).thenReturn(91L);

        service.delete(51L);

        verify(mapper).deleteDoctor(51L);
        verify(mapper).deleteAccount(91L);
    }
}
