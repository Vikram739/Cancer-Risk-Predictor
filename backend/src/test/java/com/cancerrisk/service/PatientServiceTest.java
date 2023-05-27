package com.cancerrisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cancerrisk.domain.Patient;
import com.cancerrisk.domain.Sex;
import com.cancerrisk.dto.PatientDtos.PatientRequest;
import com.cancerrisk.dto.PatientDtos.PatientResponse;
import com.cancerrisk.exception.ConflictException;
import com.cancerrisk.exception.ResourceNotFoundException;
import com.cancerrisk.mapper.PatientMapperImpl;
import com.cancerrisk.repository.PatientRepository;
import com.cancerrisk.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository repository;
    @Mock
    private AuditService auditService;

    private PatientService service;

    @BeforeEach
    void setUp() {
        service = new PatientService(repository, new PatientMapperImpl(), auditService);
    }

    private static PatientRequest request(String mrn) {
        return new PatientRequest(mrn, "Kavya", "Menon", LocalDate.of(1980, 2, 11), Sex.FEMALE,
                "+91 98450 11223", "kavya@example.com", null);
    }

    @Test
    void createNormalisesMrnAndAudits() {
        when(repository.existsByMrnIgnoreCase("MRN-55")).thenReturn(false);
        when(repository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(55L);
            return p;
        });

        PatientResponse response = service.create(request(" mrn-55 "));

        assertThat(response.mrn()).isEqualTo("MRN-55");
        assertThat(response.fullName()).isEqualTo("Kavya Menon");
        assertThat(response.age()).isPositive();
        verify(auditService).record(eq("CREATE"), eq("PATIENT"), eq(55L), any());
    }

    @Test
    void createRejectsDuplicateMrn() {
        when(repository.existsByMrnIgnoreCase("MRN-1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("MRN-1"))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateChecksMrnAgainstOtherPatients() {
        when(repository.findById(3L)).thenReturn(Optional.of(TestData.patient(3L, "MRN-3")));
        when(repository.existsByMrnIgnoreCaseAndIdNot("MRN-4", 3L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(3L, request("MRN-4"))).isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAppliesChanges() {
        Patient existing = TestData.patient(3L, "MRN-3");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        PatientResponse response = service.update(3L, request("MRN-3"));

        assertThat(response.firstName()).isEqualTo("Kavya");
        assertThat(response.email()).isEqualTo("kavya@example.com");
    }

    @Test
    void listUsesSearchWhenTermGiven() {
        var pageable = PageRequest.of(0, 10);
        when(repository.search("%menon%", pageable))
                .thenReturn(new PageImpl<>(List.of(TestData.patient(1L, "MRN-1")), pageable, 1));

        var page = service.list("  Menon ", pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        verify(repository, never()).findAll(pageable);
    }

    @Test
    void deleteMissingPatientFails() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
