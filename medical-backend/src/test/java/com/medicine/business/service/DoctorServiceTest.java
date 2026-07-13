package com.medicine.business.service;

import com.medicine.business.mapper.DoctorMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
