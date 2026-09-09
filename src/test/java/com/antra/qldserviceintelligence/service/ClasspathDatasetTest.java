package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

class ClasspathDatasetTest {

	private static final String POPULATION = "qld_lga_population_2001_2025.csv";
	private static final String ED = "qld_emergency_department_sep2025.csv";

	@TempDir
	Path directory;

	@Test
	void bundledDatasetsPreserveOriginalSourceBytes() throws Exception {
		for (String name : new String[] { POPULATION, ED }) {
			try (var input = new ClassPathResource("data/" + name).getInputStream()) {
				assertThat(input.readAllBytes()).isEqualTo(Files.readAllBytes(Path.of("data/raw", name)));
			}
		}
	}

	@Test
	void defaultServicesReadClasspathDataWithUnchangedOutputs() throws Exception {
		var population = new PopulationService();
		var originalPopulation = new PopulationService(Path.of("data/raw", POPULATION));
		assertThat(population.getPopulation()).hasSize(78).isEqualTo(originalPopulation.getPopulation());
		assertThat(population.getPopulationGrowth()).isEqualTo(originalPopulation.getPopulationGrowth());
		assertThat(new EmergencyDepartmentService().getEmergencyDepartments()).hasSize(104)
				.isEqualTo(new EmergencyDepartmentService(Path.of("data/raw", ED)).getEmergencyDepartments());
	}

	@Test
	void parsersReadJarResourcesWithoutFilesystemExtractionAndCanReadAgain() throws Exception {
		Path archive = directory.resolve("datasets.jar");
		try (var jar = new JarOutputStream(Files.newOutputStream(archive))) {
			for (String name : new String[] { POPULATION, ED }) {
				jar.putNextEntry(new JarEntry("data/" + name));
				try (var input = new ClassPathResource("data/" + name).getInputStream()) {
					input.transferTo(jar);
				}
				jar.closeEntry();
			}
		}
		// No parent loader: resources must come from this JAR, not target/classes.
		try (var loader = new URLClassLoader(new URL[] { archive.toUri().toURL() }, null)) {
			var populationResource = new ClassPathResource("data/" + POPULATION, loader);
			var edResource = new ClassPathResource("data/" + ED, loader);
			assertThat(populationResource.getURL().getProtocol()).isEqualTo("jar");
			assertThat(edResource.getURL().getProtocol()).isEqualTo("jar");
			var population = new PopulationService(populationResource);
			var ed = new EmergencyDepartmentService(edResource);
			for (int attempt = 0; attempt < 2; attempt++) {
				assertThat(population.getPopulation()).isEqualTo(new PopulationService().getPopulation());
				assertThat(population.getPopulationGrowth()).isEqualTo(new PopulationService().getPopulationGrowth());
				assertThat(ed.getEmergencyDepartments()).isEqualTo(new EmergencyDepartmentService().getEmergencyDepartments());
			}
		}
	}
}
