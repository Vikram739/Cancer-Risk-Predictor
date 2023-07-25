package com.cancerrisk.mapper;

import com.cancerrisk.domain.Patient;
import com.cancerrisk.dto.PatientDtos.PatientRequest;
import com.cancerrisk.dto.PatientDtos.PatientResponse;
import java.time.LocalDate;
import java.time.Period;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PatientMapper {

    @Mapping(target = "age", expression = "java(ageOf(patient.getDateOfBirth()))")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(PatientRequest request, @MappingTarget Patient patient);

    default Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
