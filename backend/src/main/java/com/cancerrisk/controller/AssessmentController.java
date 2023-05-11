package com.cancerrisk.controller;

import com.cancerrisk.domain.RiskBand;
import com.cancerrisk.dto.AssessmentDtos.AssessmentRequest;
import com.cancerrisk.dto.AssessmentDtos.AssessmentResponse;
import com.cancerrisk.dto.PageResponse;
import com.cancerrisk.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@Tag(name = "Assessments")
@SecurityRequirement(name = "bearerAuth")
public class AssessmentController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final AssessmentService assessmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Score a patient's measurements with the ML service and store the result")
    public AssessmentResponse create(@Valid @RequestBody AssessmentRequest request) {
        return assessmentService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    public PageResponse<AssessmentResponse> list(
            @RequestParam(required = false) RiskBand riskBand,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return assessmentService.list(riskBand, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    public AssessmentResponse get(@PathVariable Long id) {
        return assessmentService.get(id);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    @Operation(summary = "Download assessments as CSV, optionally filtered by date range and patient")
    public ResponseEntity<String> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long patientId) {
        String csv = assessmentService.exportCsv(from, to, patientId);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("assessments.csv").build().toString())
                .body(csv);
    }
}
