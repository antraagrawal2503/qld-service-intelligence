package com.antra.qldserviceintelligence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antra.qldserviceintelligence.model.PopulationRecord;

public interface PopulationRepository extends JpaRepository<PopulationRecord, Long> {
	Optional<PopulationRecord> findByLga(String lga);
}
