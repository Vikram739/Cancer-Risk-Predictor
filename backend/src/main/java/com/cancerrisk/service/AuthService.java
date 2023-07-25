package com.cancerrisk.service;

import com.cancerrisk.domain.Role;
import com.cancerrisk.domain.RoleName;
import com.cancerrisk.domain.User;
import com.cancerrisk.dto.AuthDtos.AuthResponse;
import com.cancerrisk.dto.AuthDtos.LoginRequest;
import com.cancerrisk.dto.AuthDtos.RefreshRequest;
import com.cancerrisk.dto.AuthDtos.RegisterRequest;
import com.cancerrisk.dto.AuthDtos.UserResponse;
import com.cancerrisk.exception.ApiException;
import com.cancerrisk.exception.BadRequestException;
import com.cancerrisk.exception.ConflictException;
import com.cancerrisk.exception.ResourceNotFoundException;
import com.cancerrisk.mapper.UserMapper;
import com.cancerrisk.repository.RoleRepository;
import com.cancerrisk.repository.UserRepository;
import com.cancerrisk.security.AppUserDetailsService;
import com.cancerrisk.security.JwtService;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<RoleName> SELF_SERVICE_ROLES = Set.of(RoleName.DOCTOR, RoleName.ANALYST);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        RoleName roleName = request.role() == null ? RoleName.ANALYST : request.role();
        if (!SELF_SERVICE_ROLES.contains(roleName)) {
            throw new BadRequestException("Role " + roleName + " cannot be self assigned");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        auditService.record(email, "REGISTER", "USER", saved.getId(), "role=" + roleName);
        return tokensFor(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        auditService.record(email, "LOGIN", "USER", user.getId(), null);
        return tokensFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String email = jwtService.validate(request.refreshToken(), JwtService.REFRESH)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
        return tokensFor(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    private AuthResponse tokensFor(User user) {
        UserDetails details = AppUserDetailsService.toUserDetails(user);
        return new AuthResponse(
                jwtService.generateAccessToken(details),
                jwtService.generateRefreshToken(details),
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                userMapper.toResponse(user));
    }
}
