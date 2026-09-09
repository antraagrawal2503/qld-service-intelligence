package com.antra.qldserviceintelligence.service;

import java.io.IOException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentRecord;
import com.antra.qldserviceintelligence.repository.EmergencyDepartmentRepository;

@Component
public class EmergencyDepartmentImporter implements ApplicationRunner {

	private final EmergencyDepartmentService service;
	private final EmergencyDepartmentRepository repository;

	public EmergencyDepartmentImporter(EmergencyDepartmentService service, EmergencyDepartmentRepository repository) {
		this.service = service;
		this.repository = repository;
	}

	@Override
	@Transactional(rollbackFor = IOException.class)
	public void run(ApplicationArguments args) throws IOException {
		// Reuse the API parser's ALL-only selection and statewide aggregate exclusion.
		for (var row : service.getEmergencyDepartments()) {
			EmergencyDepartmentRecord record = repository
					.findByFacilityCodeAndQuarter(row.facilityCode().strip(), row.quarter().strip())
					.orElseGet(() -> new EmergencyDepartmentRecord(row));
			record.updateFrom(row);
			repository.save(record);
		}
	}
}
