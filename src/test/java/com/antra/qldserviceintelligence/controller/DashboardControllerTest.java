package com.antra.qldserviceintelligence.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.datasource.url=jdbc:h2:mem:dashboard_test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class DashboardControllerTest {

	@LocalServerPort
	private int port;

	@Test
	void rootServesDashboardHtml() throws Exception {
		var response = get("/");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value ->
				assertThat(value).startsWith("text/html"));
		assertThat(response.body()).contains("QLD Service Intelligence", "id=\"pressure\"", "id=\"population\"",
				"id=\"methodology\"", "not an official Queensland Government product",
				"href=\"/css/dashboard.css\"", "src=\"/js/dashboard.js\"");
	}

	@Test
	void stylesheetIsAccessible() throws Exception {
		var response = get("/css/dashboard.css");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value ->
				assertThat(value).startsWith("text/css"));
		assertThat(response.body()).contains(".kpi-grid", "@media", ":focus-visible");
	}

	@Test
	void javascriptIsAccessible() throws Exception {
		var response = get("/js/dashboard.js");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(value ->
				assertThat(value).contains("javascript"));
		assertThat(response.body()).contains("fetch(", "/api/v1/emergency-departments/pressure",
				"/api/v1/population/growth");
	}

	@Test
	void dashboardServesLabelledExplorerControls() throws Exception {
		var response = get("/");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("placeholder=\"Search facility...\"", "placeholder=\"Search area...\"",
				"for=\"facility-search\"", "for=\"area-search\"", "for=\"pressure-level\"",
				"for=\"pressure-view\"", "for=\"population-view\"", "All facilities", "All areas", "Unknown");
	}

	private HttpResponse<String> get(String path) throws Exception {
		try (var client = HttpClient.newHttpClient()) {
			return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
					HttpResponse.BodyHandlers.ofString());
		}
	}
}
