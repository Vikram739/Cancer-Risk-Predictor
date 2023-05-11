package com.cancerrisk.controller;

import com.cancerrisk.domain.AuditLog;
import com.cancerrisk.dto.AuthDtos.UserResponse;
import com.cancerrisk.dto.PageResponse;
import com.cancerrisk.mapper.UserMapper;
import com.cancerrisk.repository.AuditLogRepository;
import com.cancerrisk.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return userRepository.findAll(Sort.by("fullName")).stream().map(userMapper::toResponse).toList();
    }

    @GetMapping("/audit-log")
    @Transactional(readOnly = true)
    public PageResponse<AuditLog> auditLog(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable), a -> a);
    }
}
