package com.cancerrisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cancerrisk.domain.Role;
import com.cancerrisk.domain.RoleName;
import com.cancerrisk.domain.User;
import com.cancerrisk.dto.AuthDtos.AuthResponse;
import com.cancerrisk.dto.AuthDtos.LoginRequest;
import com.cancerrisk.dto.AuthDtos.RefreshRequest;
import com.cancerrisk.dto.AuthDtos.RegisterRequest;
import com.cancerrisk.exception.ApiException;
import com.cancerrisk.exception.BadRequestException;
import com.cancerrisk.exception.ConflictException;
import com.cancerrisk.mapper.UserMapperImpl;
import com.cancerrisk.repository.RoleRepository;
import com.cancerrisk.repository.UserRepository;
import com.cancerrisk.security.JwtProperties;
import com.cancerrisk.security.JwtService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuditService auditService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(new JwtProperties(
            "unit-test-secret-that-is-long-enough-for-hs256", "test", Duration.ofMinutes(5), Duration.ofDays(1)));
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, encoder, authenticationManager, jwtService,
                new UserMapperImpl(), auditService);
    }

    private static User user(String email, RoleName role) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setFullName("Sneha Rao");
        u.setPassword("hash");
        u.getRoles().add(new Role(1L, role));
        return u;
    }

    @Test
    void registerHashesPasswordAndDefaultsToAnalyst() {
        when(userRepository.existsByEmailIgnoreCase("sneha@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ANALYST)).thenReturn(Optional.of(new Role(3L, RoleName.ANALYST)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse response = service.register(
                new RegisterRequest("Sneha Rao", "Sneha@Example.com", "secret123", null));

        assertThat(response.user().email()).isEqualTo("sneha@example.com");
        assertThat(response.user().roles()).containsExactly(RoleName.ANALYST);
        assertThat(jwtService.validate(response.accessToken(), JwtService.ACCESS)).contains("sneha@example.com");
        assertThat(jwtService.validate(response.refreshToken(), JwtService.REFRESH)).contains("sneha@example.com");
    }

    @Test
    void registerRejectsAdminRole() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        assertThatThrownBy(() -> service.register(
                new RegisterRequest("X", "x@example.com", "secret123", RoleName.ADMIN)))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(
                new RegisterRequest("Dup", "dup@example.com", "secret123", RoleName.DOCTOR)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> service.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshIssuesNewTokens() {
        User u = user("doc@example.com", RoleName.DOCTOR);
        when(userRepository.findByEmailIgnoreCase("doc@example.com")).thenReturn(Optional.of(u));
        String refresh = service.refresh(new RefreshRequest(
                jwtService.generateRefreshToken(com.cancerrisk.security.AppUserDetailsService.toUserDetails(u))))
                .refreshToken();
        assertThat(refresh).isNotBlank();
    }

    @Test
    void refreshRejectsAccessToken() {
        User u = user("doc@example.com", RoleName.DOCTOR);
        String access = jwtService.generateAccessToken(com.cancerrisk.security.AppUserDetailsService.toUserDetails(u));
        assertThatThrownBy(() -> service.refresh(new RefreshRequest(access)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Refresh token");
    }
}
