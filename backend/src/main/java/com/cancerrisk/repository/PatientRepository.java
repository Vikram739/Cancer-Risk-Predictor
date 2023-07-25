package com.cancerrisk.repository;

import com.cancerrisk.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByMrnIgnoreCase(String mrn);

    boolean existsByMrnIgnoreCaseAndIdNot(String mrn, Long id);

    @Query("""
            select p from Patient p
            where lower(p.firstName) like :pattern
               or lower(p.lastName) like :pattern
               or lower(p.mrn) like :pattern
               or lower(concat(p.firstName, ' ', p.lastName)) like :pattern
            """)
    Page<Patient> search(@Param("pattern") String pattern, Pageable pageable);
}
