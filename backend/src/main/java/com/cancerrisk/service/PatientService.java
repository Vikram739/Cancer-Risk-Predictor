package com.cancerrisk.service;

import com.cancerrisk.domain.Patient;
import com.cancerrisk.dto.PageResponse;
import com.cancerrisk.dto.PatientDtos.PatientRequest;
import com.cancerrisk.dto.PatientDtos.PatientResponse;
import com.cancerrisk.exception.ConflictException;
import com.cancerrisk.exception.ResourceNotFoundException;
import com.cancerrisk.mapper.PatientMapper;
import com.cancerrisk.repository.PatientRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository repository;
    private final PatientMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> list(String search, Pageable pageable) {
        Page<Patient> page = StringUtils.hasText(search)
                ? repository.search("%" + search.trim().toLowerCase(Locale.ROOT) + "%", pageable)
                : repository.findAll(pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        String mrn = normaliseMrn(request.mrn());
        if (repository.existsByMrnIgnoreCase(mrn)) {
            throw new ConflictException("A patient with MRN " + mrn + " already exists");
        }
        Patient patient = mapper.toEntity(request);
        patient.setMrn(mrn);
        Patient saved = repository.save(patient);
        auditService.record("CREATE", "PATIENT", saved.getId(), "mrn=" + mrn);
        return mapper.toResponse(saved);
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = find(id);
        String mrn = normaliseMrn(request.mrn());
        if (repository.existsByMrnIgnoreCaseAndIdNot(mrn, id)) {
            throw new ConflictException("A patient with MRN " + mrn + " already exists");
        }
        mapper.update(request, patient);
        patient.setMrn(mrn);
        Patient saved = repository.saveAndFlush(patient);
        auditService.record("UPDATE", "PATIENT", id, null);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = find(id);
        repository.delete(patient);
        auditService.record("DELETE", "PATIENT", id, "mrn=" + patient.getMrn());
    }

    @Transactional(readOnly = true)
    public Patient find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient", id));
    }

    private static String normaliseMrn(String mrn) {
        return mrn.trim().toUpperCase(Locale.ROOT);
    }
}
