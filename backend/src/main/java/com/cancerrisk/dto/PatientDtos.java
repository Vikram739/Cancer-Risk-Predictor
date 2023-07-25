package com.cancerrisk.dto;

import com.cancerrisk.domain.Sex;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "letters, digits and hyphens only")
            String mrn,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Sex sex,
            @Size(max = 32) @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "invalid phone number") String phone,
            @Email @Size(max = 160) String email,
            @Size(max = 1000) String notes) {
    }

    public record PatientResponse(
            Long id,
            String mrn,
            String firstName,
            String lastName,
            String fullName,
            LocalDate dateOfBirth,
            Integer age,
            Sex sex,
            String phone,
            String email,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
    }
}
