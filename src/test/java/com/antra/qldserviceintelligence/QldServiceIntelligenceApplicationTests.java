package com.antra.qldserviceintelligence;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class QldServiceIntelligenceApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void connectsToIsolatedTestDatabase() throws Exception {
		try (var connection = dataSource.getConnection();
				var statement = connection.createStatement();
				var result = statement.executeQuery("SELECT 1")) {
			assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isEqualTo(1);
		}
	}

}
