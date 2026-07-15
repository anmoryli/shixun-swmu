package com.medicine;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class MedicalBackendApplicationTest {
    @Test
    void delegatesStartupToSpringBoot() {
        new MedicalBackendApplication();
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            MedicalBackendApplication.main(args);
            spring.verify(() -> SpringApplication.run(MedicalBackendApplication.class, args));
        }
    }
}
