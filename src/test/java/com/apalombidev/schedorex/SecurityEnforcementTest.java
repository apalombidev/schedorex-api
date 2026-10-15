package com.apalombidev.schedorex;

import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.auth.dto.LoginResponse;
import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;
import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityEnforcementTest extends AbstractIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	@Test
	void adminEndpointWithoutCookieReturns401() {
		ResponseEntity<String> response = restTemplate.getForEntity(url("/api/admin/service"), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void mutatingAdminEndpointWithoutCsrfTokenReturns403() {
		// Login to get valid cookie jwt...
		LoginRequest loginRequest = new LoginRequest("admin@test.local", "admin123");
		ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(url("/api/admin/auth/login"),
				loginRequest, LoginResponse.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		String jwtCookie = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
				.filter(c -> c.startsWith("jwt=")).findFirst().map(c -> c.split(";", 2)[0]).orElseThrow();

		// ...no header X-XSRF-TOKEN on mutant request.
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.COOKIE, jwtCookie);

		ServiceSetupRequest request = new ServiceSetupRequest("X", "Y", 30, 1000, ServiceCurrency.EUR, "Europe/Rome",
				ServiceLanguage.IT, 24, false);

		ResponseEntity<String> response = restTemplate.exchange(url("/api/admin/service"), HttpMethod.PUT,
				new HttpEntity<>(request, headers), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}