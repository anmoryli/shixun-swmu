package com.medicine.business.service;

import com.medicine.business.mapper.CityMapper;
import com.medicine.business.mapper.CompanyMapper;
import com.medicine.business.mapper.CompanyPolicyMapper;
import com.medicine.business.mapper.DrugMapper;
import com.medicine.business.mapper.MaterialMapper;
import com.medicine.business.mapper.MedicalPolicyMapper;
import com.medicine.business.mapper.SaleMapper;
import com.medicine.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessServicesTest {
    @Test
    void cityServiceCoversPagingLookupAndMutations() {
        CityMapper mapper = mock(CityMapper.class);
        when(mapper.count(any())).thenReturn(6L);
        when(mapper.page(any(), anyInt(), anyInt())).thenReturn(List.of(Map.of("cityId", 1)));
        when(mapper.findAll()).thenReturn(List.of(Map.of("cityId", 1)));
        when(mapper.countByNumber(510500)).thenReturn(1L);
        CityService service = new CityService(mapper);

        assertEquals(2, service.page(2, 5, "泸州").get("pages"));
        assertEquals(1L, service.all().get("total"));
        assertFalse(service.exists(null));
        assertTrue(service.exists(510500));
        assertEquals(2, service.add(510500, 5));
        service.delete(1L);
        verify(mapper).insert(510500);
        verify(mapper).delete(1L);
    }

    @Test
    void companyServiceCoversAllOperations() {
        CompanyMapper mapper = mock(CompanyMapper.class);
        when(mapper.count(any())).thenReturn(6L);
        when(mapper.page(any(), anyInt(), anyInt())).thenReturn(List.of(Map.of("companyId", 1)));
        when(mapper.findAll()).thenReturn(List.of(Map.of("companyId", 1)));
        CompanyService service = new CompanyService(mapper);
        Map<String, Object> request = Map.of("companyName", "  华西  ", "companyPhone", "15900000000");

        assertEquals(2, service.page(null, null, null).get("pages"));
        assertEquals(1L, service.all().get("total"));
        assertEquals(2, service.add(request, 5));
        service.update(1L, request);
        service.delete(1L);
        verify(mapper).insert("华西", "15900000000");
        verify(mapper).update(1L, "华西", "15900000000");
        verify(mapper).delete(1L);
    }

    @Test
    void companyPolicyServiceNestsCompanyAndMutatesRows() {
        CompanyPolicyMapper mapper = mock(CompanyPolicyMapper.class);
        when(mapper.count(any())).thenReturn(6L);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("companyId", 3L);
        row.put("companyName", "华西");
        row.put("title", "policy");
        when(mapper.page(any(), anyInt(), anyInt())).thenReturn(new ArrayList<>(List.of(row)));
        CompanyPolicyService service = new CompanyPolicyService(mapper);
        Map<String, Object> request = Map.of("companyId", "3", "title", " t ", "message", " m ");

        Map<String, Object> page = service.page(1, 5, null);
        assertTrue(((Map<?, ?>) ((List<?>) page.get("list")).get(0)).containsKey("drugCompanyModel"));
        assertEquals(2, service.add(request, 5));
        service.update(1L, request);
        service.delete(1L);
        assertThrows(BusinessException.class, () -> service.add(Map.of(), 5));
        service.update(2L, Map.of());
        verify(mapper).delete(1L);
    }

    @Test
    void materialServiceCoversAllOperations() {
        MaterialMapper mapper = mock(MaterialMapper.class);
        when(mapper.count(any())).thenReturn(1L);
        when(mapper.page(any(), anyInt(), anyInt())).thenReturn(List.of(Map.of("id", 1)));
        MaterialService service = new MaterialService(mapper);
        Map<String, Object> request = Map.of("title", " title ", "message", " body ");

        assertEquals(1, service.page(1, 5, null).get("pages"));
        assertEquals(1, service.add(request, 5));
        service.update(1L, request);
        service.delete(1L);
        verify(mapper).insert("title", "body");
        verify(mapper).delete(1L);
    }

    @Test
    void medicalPolicyServiceNestsCityAndDefaultsDates() {
        MedicalPolicyMapper mapper = mock(MedicalPolicyMapper.class);
        when(mapper.count(any(), any(), any(), any())).thenReturn(1L);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cityId", 2L);
        row.put("cityNumber", 510500);
        row.put("province", "四川");
        row.put("city", "泸州");
        when(mapper.page(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(new ArrayList<>(List.of(row)));
        MedicalPolicyService service = new MedicalPolicyService(mapper);
        Map<String, Object> request = Map.of("cityId", 2, "title", " t ", "message", " m ", "updateTime", "2026-07-15");

        Map<String, Object> page = service.page(1, 5, null, null, null, null);
        assertTrue(((Map<?, ?>) ((List<?>) page.get("list")).get(0)).containsKey("cityModel"));
        assertEquals(1, service.add(request, 5));
        service.update(1L, request);
        assertThrows(BusinessException.class, () -> service.add(Map.of(), 5));
        service.update(2L, Map.of("updateTime", ""));
        service.delete(1L);
        verify(mapper).delete(1L);
    }

    @Test
    void saleServiceConvertsCoordinatesAndCoversAllOperations() {
        SaleMapper mapper = mock(SaleMapper.class);
        when(mapper.count(any())).thenReturn(1L);
        when(mapper.page(any(), anyInt(), anyInt())).thenReturn(List.of(Map.of("saleId", 1)));
        when(mapper.findAll()).thenReturn(List.of(Map.of("saleId", 1)));
        SaleService service = new SaleService(mapper);
        Map<String, Object> request = Map.of(
                "saleName", " shop ", "salePhone", "15900000000", "address", " road ",
                "longitude", "104.1", "latitude", new BigDecimal("30.2"));

        assertEquals(1, service.page(1, 5, null).get("pages"));
        assertEquals(1L, service.all().get("total"));
        assertEquals(1, service.add(request, 5));
        service.update(1L, request);
        service.delete(1L);
        assertThrows(BusinessException.class, () -> service.add(Map.of(), 5));
        verify(mapper).deleteDrugRelations(1L);
        verify(mapper).delete(1L);
    }

    @Test
    void drugServiceCoversRelationsPagingAndMutations() {
        DrugMapper mapper = mock(DrugMapper.class);
        when(mapper.count(any())).thenReturn(6L);
        Map<String, Object> drug = new LinkedHashMap<>();
        drug.put("drugId", 7L);
        when(mapper.page(any(), anyInt(), anyInt()))
                .thenReturn(new ArrayList<>(), new ArrayList<>(List.of(drug)));
        Map<String, Object> sale = new LinkedHashMap<>();
        sale.put("drugId", 7L);
        sale.put("saleId", 9L);
        when(mapper.findSales(anyList())).thenReturn(new ArrayList<>(List.of(sale)));
        doAnswer(invocation -> {
            Map<String, Object> values = invocation.getArgument(0);
            values.put("drugId", 7L);
            return 1;
        }).when(mapper).insertDrug(anyMap());
        DrugService service = new DrugService(mapper);

        assertEquals(2, service.page(1, 5, null).get("pages"));
        Map<String, Object> populated = service.page(1, 5, null);
        assertEquals(1, ((List<?>) ((Map<?, ?>) ((List<?>) populated.get("list")).get(0)).get("drugSales")).size());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("drugName", " name ");
        request.put("drugInfo", " info ");
        request.put("drugEffect", " effect ");
        request.put("drugImg", " img ");
        request.put("drugPublisher", " publisher ");
        request.put("saleIds", List.of(9, "10", ""));
        assertEquals(2, service.add(request, 5));
        service.update(7L, request);
        service.update(8L, Map.of("saleIds", "not-an-iterable"));
        service.delete(7L);
        verify(mapper, atLeastOnce()).insertSaleRelation(7L, 9L);
        verify(mapper).deleteDrug(7L);
    }
}
