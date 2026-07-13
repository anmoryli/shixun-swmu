package com.medicine.auth.service;

import com.medicine.auth.dto.LoginResult;
import com.medicine.auth.mapper.AccountMapper;
import com.medicine.auth.model.Account;
import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuthSession;
import com.medicine.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void loginReturnsRawTokenAndNumericUserType() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        TokenService tokenService = mock(TokenService.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Account account = account(encoder.encode("secret"), "ROLE_1");
        when(accountMapper.findByUsername("admin_1")).thenReturn(account);
        when(tokenService.create(any(AuthSession.class))).thenReturn("raw-token");

        LoginResult result = new AuthService(accountMapper, encoder, tokenService).login(" admin_1 ", "secret");

        assertThat(result.getToken()).isEqualTo("raw-token");
        assertThat(result.getUserInfo().getUtype()).isEqualTo(1);
        assertThat(result.getUserInfo().getRealname()).isEqualTo("管理员");
        verify(tokenService).create(any(AuthSession.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        AccountMapper accountMapper = mock(AccountMapper.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(accountMapper.findByUsername("admin_1")).thenReturn(account(encoder.encode("secret"), "ROLE_1"));

        AuthService service = new AuthService(accountMapper, encoder, mock(TokenService.class));

        assertThatThrownBy(() -> service.login("admin_1", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    private Account account(String password, String roleName) {
        Account account = new Account();
        account.setId(1L);
        account.setRealname("管理员");
        account.setUname("admin_1");
        account.setPwd(password);
        account.setPhonenumber("15900000000");
        account.setUtype(roleName);
        return account;
    }
}
