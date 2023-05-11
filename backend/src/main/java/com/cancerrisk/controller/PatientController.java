package com.cancerrisk.controller;

import com.cancerrisk.dto.AssessmentDtos.AssessmentResponse;
import com.cancerrisk.dto.PageResponse;
import com.cancerrisk.dto.PatientDtos.PatientRequest;
import com.cancerrisk.dto.PatientDtos.PatientResponse;
import com.cancerrisk.service.AssessmentService;
import com.cancerrisk.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;
    private final AssessmentService assessmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    @Operation(summary = "List patients with paging, sorting and free text search")
    public PageResponse<PatientResponse> list(
            @Parameter(description = "matches name or MRN") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return patientService.list(search, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    public PatientResponse get(@PathVariable Long id) {
        return patientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public PatientResponse create(@Valid @RequestBody PatientRequest request) {
        return patientService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        patientService.delete(id);
    }

    @GetMapping("/{id}/assessments")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','ANALYST')")
    @Operation(summary = "Assessment history for one patient, newest first by default")
    public PageResponse<AssessmentResponse> history(
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return assessmentService.historyForPatient(id, pageable);
    }
}
