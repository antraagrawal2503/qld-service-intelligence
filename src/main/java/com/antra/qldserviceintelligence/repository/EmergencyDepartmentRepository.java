package com.antra.qldserviceintelligence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentRecord;

public interface EmergencyDepartmentRepository extends JpaRepository<EmergencyDepartmentRecord, Long> {
	Optional<EmergencyDepartmentRecord> findByFacilityCodeAndQuarter(String facilityCode, String quarter);
}
