package com.medicine.common;

import com.medicine.auth.dto.LoginRequest;
import com.medicine.auth.dto.LoginResult;
import com.medicine.auth.dto.PermissionMeta;
import com.medicine.auth.dto.PermissionNode;
import com.medicine.auth.dto.UserInfo;
import com.medicine.auth.model.Account;
import com.medicine.auth.model.PermissionRecord;
import com.medicine.dashboard.dto.DashboardCounts;
import com.medicine.dashboard.dto.DashboardNews;
import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.dto.NameValue;
import com.medicine.security.AuthSession;
import com.medicine.security.CookieProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PojoCoverageTest {
    private static final List<Class<?>> TYPES = List.of(
            LoginRequest.class, LoginResult.class, PermissionMeta.class, PermissionNode.class,
            UserInfo.class, Account.class, PermissionRecord.class, DashboardCounts.class,
            DashboardNews.class, DashboardView.class, NameValue.class, AuthSession.class,
            CookieProperties.class, PageView.class);

    @Test
    void invokesAllPojoConstructorsGettersAndSetters() throws Exception {
        for (Class<?> type : TYPES) {
            Object instance = null;
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                Object created = constructor.newInstance(arguments(constructor.getParameterTypes()));
                if (instance == null) instance = created;
            }
            assertNotNull(instance, type.getName());
            for (Method method : type.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
                method.setAccessible(true);
                if (method.getParameterCount() == 0) {
                    method.invoke(instance);
                } else if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                    method.invoke(instance, sample(method.getParameterTypes()[0]));
                    String getterName = (method.getParameterTypes()[0] == boolean.class ? "is" : "get")
                            + method.getName().substring(3);
                    try {
                        assertNotNull(type.getMethod(getterName).invoke(instance));
                    } catch (NoSuchMethodException ignored) {
                        // Some JavaBeans legitimately expose only the setter under test.
                    }
                }
            }
        }
    }

    @Test
    void pageViewNormalizesNullLists() {
        PageView<String> page = new PageView<>(1, null, 2, 5);
        assertTrue(page.getList().isEmpty());
        page.setList(List.of("value"));
        assertEquals("value", page.getList().get(0));
    }

    private static Object[] arguments(Class<?>[] types) {
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) values[i] = sample(types[i]);
        return values;
    }

    private static Object sample(Class<?> type) {
        if (type == String.class) return "value";
        if (type == long.class || type == Long.class) return 1L;
        if (type == int.class || type == Integer.class) return 1;
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == Duration.class) return Duration.ofHours(1);
        if (List.class.isAssignableFrom(type)) return List.of();
        if (type == UserInfo.class) return new UserInfo();
        if (type == PermissionMeta.class) return new PermissionMeta();
        if (type == DashboardCounts.class) return new DashboardCounts();
        return null;
    }
}
