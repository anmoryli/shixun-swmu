package com.medicine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"com.medicine.auth.mapper", "com.medicine.business.mapper", "com.medicine.dashboard.mapper"})
@SpringBootApplication
public class MedicalBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalBackendApplication.class, args);
    }
}
