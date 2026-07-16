package com.medicine.business.controller;

import com.medicine.amap.AmapController;
import com.medicine.amap.AmapService;
import com.medicine.business.service.*;
import com.medicine.dashboard.controller.DashboardController;
import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.service.DashboardService;
import com.medicine.security.AuthSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ControllerCoverageTest {
    @Test
    void businessResponseHelpersWrapValues() {
        assertEquals("value", BusinessResponses.wrapped("key", "value").getData().get("key"));
        assertEquals(3, BusinessResponses.pages(3).getData().get("pages"));
    }

    @Test
    void cityControllerCoversDuplicateAndSuccessPaths() {
        CityService service = mock(CityService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        when(service.all()).thenReturn(Map.of());
        when(service.add(any(), anyInt())).thenReturn(2);
        CityController controller = new CityController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.all());
        when(service.exists(510500)).thenReturn(true, false);
        assertNotNull(controller.add(510500));
        assertNotNull(controller.add(510500));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void companyControllerCoversEveryEndpoint() {
        CompanyService service = mock(CompanyService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        when(service.all()).thenReturn(Map.of());
        CompanyController controller = new CompanyController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.all());
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void companyPolicyControllerCoversEveryEndpoint() {
        CompanyPolicyService service = mock(CompanyPolicyService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        CompanyPolicyController controller = new CompanyPolicyController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void doctorControllerCoversDuplicateAndSuccessPaths() {
        DoctorService service = mock(DoctorService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        when(service.levelAndType()).thenReturn(Map.of());
        DoctorController controller = new DoctorController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.levelAndType());
        when(service.add(anyMap(), anyInt())).thenReturn(-1, 2);
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.add(Map.of()));
        when(service.update(anyLong(), anyMap())).thenReturn(false, true);
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
        AuthSession session = new AuthSession(9L, "admin", "Admin", "1", 1, "15900000000");
        assertNotNull(controller.reset(2L, session));
    }

    @Test
    void drugControllerCoversEveryEndpoint() {
        DrugService service = mock(DrugService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        DrugController controller = new DrugController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void materialControllerCoversEveryEndpoint() {
        MaterialService service = mock(MaterialService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        MaterialController controller = new MaterialController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void medicalPolicyControllerCoversEveryEndpoint() {
        MedicalPolicyService service = mock(MedicalPolicyService.class);
        when(service.page(any(), any(), any(), any(), any(), any())).thenReturn(Map.of());
        MedicalPolicyController controller = new MedicalPolicyController(service);
        assertNotNull(controller.page(1, 5, null, null, null, null));
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void saleControllerCoversEveryEndpoint() {
        SaleService service = mock(SaleService.class);
        when(service.page(any(), any(), any())).thenReturn(Map.of());
        when(service.all()).thenReturn(Map.of());
        SaleController controller = new SaleController(service);
        assertNotNull(controller.page(1, 5, null));
        assertNotNull(controller.all());
        assertNotNull(controller.add(Map.of()));
        assertNotNull(controller.update(1L, Map.of()));
        assertNotNull(controller.delete(1L));
    }

    @Test
    void uploadControllerBuildsRelativeImageUrl() {
        FileStorageService storage = mock(FileStorageService.class);
        MultipartFile file = mock(MultipartFile.class);
        when(storage.saveImage(file)).thenReturn("image.png");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("example.test");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            // 上传接口返回同源相对路径 /image/<file>，不依赖请求上下文，避免
            // https 前端加载 http 绝对 URL 触发混合内容拦截。
            assertEquals("/image/image.png",
                    new UploadController(storage).upload(file).getData().get("url").toString());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void amapAndDashboardControllersDelegate() {
        AmapService amap = mock(AmapService.class);
        when(amap.reverseGeocode(104, 30)).thenReturn("address");
        assertEquals("address", new AmapController(amap).reverseGeocode(104, 30).getData());

        DashboardService dashboard = mock(DashboardService.class);
        DashboardView view = mock(DashboardView.class);
        when(dashboard.getDashboard()).thenReturn(view);
        assertSame(view, new DashboardController(dashboard).dashboard().getData());
    }
}
