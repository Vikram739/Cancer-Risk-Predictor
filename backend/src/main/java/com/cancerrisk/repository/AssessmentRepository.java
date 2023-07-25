package com.cancerrisk.repository;

import com.cancerrisk.domain.Assessment;
import com.cancerrisk.domain.RiskBand;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    @EntityGraph(attributePaths = "patient")
    @Query("select a from Assessment a")
    Page<Assessment> findAllWithPatient(Pageable pageable);

    @EntityGraph(attributePaths = "patient")
    Page<Assessment> findByRiskBand(RiskBand riskBand, Pageable pageable);

    @EntityGraph(attributePaths = "patient")
    Page<Assessment> findByPatientId(Long patientId, Pageable pageable);

    @EntityGraph(attributePaths = "patient")
    @Query("select a from Assessment a where a.id = :id")
    Optional<Assessment> findWithPatientById(@Param("id") Long id);

    @EntityGraph(attributePaths = "patient")
    @Query("""
            select a from Assessment a
            where a.createdAt >= :from and a.createdAt < :to
              and (:patientId is null or a.patient.id = :patientId)
            order by a.createdAt desc
            """)
    List<Assessment> findForExport(@Param("from") Instant from, @Param("to") Instant to,
                                   @Param("patientId") Long patientId);

    @Query("select a.riskBand as band, count(a) as total from Assessment a group by a.riskBand")
    List<RiskBandCount> countByRiskBand();

    @Query(value = """
            select to_char(date_trunc('month', created_at at time zone 'UTC'), 'YYYY-MM') as month,
                   count(*) as total,
                   count(*) filter (where risk_band = 'LOW') as low,
                   count(*) filter (where risk_band = 'MODERATE') as moderate,
                   count(*) filter (where risk_band = 'HIGH') as high,
                   avg(probability) as averageProbability
            from assessments
            group by 1
            order by 1
            """, nativeQuery = true)
    List<MonthlyCount> monthlyTrend();

    @Query("select avg(a.probability) from Assessment a")
    Double averageProbability();

    @Query("select max(a.createdAt) from Assessment a")
    Instant latestCreatedAt();

    /** Patients whose most recent assessment is in the given band. */
    @Query(value = """
            select count(*) from (
                select distinct on (patient_id) patient_id, risk_band
                from assessments
                order by patient_id, created_at desc
            ) latest
            where latest.risk_band = :band
            """, nativeQuery = true)
    long countPatientsWithLatestBand(@Param("band") String band);

    interface RiskBandCount {
        RiskBand getBand();

        long getTotal();
    }

    interface MonthlyCount {
        String getMonth();

        long getTotal();

        long getLow();

        long getModerate();

        long getHigh();

        Double getAverageProbability();
    }
}
