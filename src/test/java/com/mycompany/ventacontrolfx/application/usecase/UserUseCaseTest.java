package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserUseCaseTest {

    private IUserRepository userRepository;
    private IEmailSender emailSender;
    private UserUseCase userUseCase;

    @BeforeEach
    public void setUp() {
        userRepository = mock(IUserRepository.class);
        emailSender = mock(IEmailSender.class);
        com.mycompany.ventacontrolfx.util.AuthorizationService dummyAuth = new com.mycompany.ventacontrolfx.util.AuthorizationService(
                new com.mycompany.ventacontrolfx.util.UserSession()) {
            @Override
            public void checkPermission(String code) {
            }

            @Override
            public boolean hasPermission(String code) {
                return true;
            }
        };
        userUseCase = new UserUseCase(userRepository, emailSender, dummyAuth);
    }

    @Test
    public void testLoginSuccess() throws SQLException {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("1234");

        when(userRepository.findByUsername("admin")).thenReturn(user);

        User loggedUser = userUseCase.login("admin", "1234");
        assertNotNull(loggedUser);
        assertEquals("admin", loggedUser.getUsername());
    }

    @Test
    public void testLoginFailure() throws SQLException {
        when(userRepository.findByUsername("admin")).thenReturn(null);
        
        User loggedUser = userUseCase.login("admin", "wrong");
        assertNull(loggedUser);
    }

    @Test
    public void testRecoverPasswordSendsEmail() throws Exception {
        User user = new User();
        user.setFullName("Admin User");
        user.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(user);

        userUseCase.recoverPassword("admin@test.com");

        verify(emailSender, times(1)).send(eq("admin@test.com"), anyString(), anyString());
    }
}
