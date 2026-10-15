package com.apalombidev.schedorex;

import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.auth.dto.LoginResponse;
import com.apalombidev.schedorex.availability.dto.AdminSlotResponse;
import com.apalombidev.schedorex.availability.dto.AvailabilityGenerationRequest;
import com.apalombidev.schedorex.availability.dto.AvailabilityGenerationResponse;
import com.apalombidev.schedorex.availability.dto.AvailableSlotResponse;
import com.apalombidev.schedorex.booking.dto.BookingCreatedResponse;
import com.apalombidev.schedorex.booking.dto.BookingRequest;
import com.apalombidev.schedorex.dashboard.dto.DashboardSummaryResponse;
import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;
import com.apalombidev.schedorex.service.dto.ServiceResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndFlowTest extends AbstractIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	private static class CookieJar {
		private final Map<String, String> cookies = new LinkedHashMap<>();

		void absorb(ResponseEntity<?> response) {
			List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
			if (setCookies == null) {
				return;
			}
			for (String header : setCookies) {
				String nameValue = header.split(";", 2)[0];
				String[] parts = nameValue.split("=", 2);
				if (parts.length == 2) {
					cookies.put(parts[0].trim(), parts[1].trim());
				}
			}
		}

		String asCookieHeader() {
			return cookies.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).reduce((a, b) -> a + "; " + b)
					.orElse("");
		}

		String xsrfToken() {
			return cookies.get("XSRF-TOKEN");
		}
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	private HttpHeaders mutatingHeaders(CookieJar jar) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.COOKIE, jar.asCookieHeader());

		if (jar.xsrfToken() != null) {
			headers.set("X-XSRF-TOKEN", jar.xsrfToken());
		}
		return headers;
	}

	private HttpHeaders readHeaders(CookieJar jar) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.COOKIE, jar.asCookieHeader());
		return headers;
	}

	@Test
	void fullBookingLifecycle_withSecurityActive() {
		CookieJar jar = new CookieJar();

		// ---- 1. Login admin ------------------------------------------------
		LoginRequest loginRequest = new LoginRequest("admin@test.local", "admin123");
		ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(url("/api/admin/auth/login"),
				loginRequest, LoginResponse.class);

		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		jar.absorb(loginResponse);
		assertThat(jar.asCookieHeader()).contains("jwt=");

		assertThat(jar.xsrfToken()).isNotNull();

		// ---- 2. Setup service (admin) --------------------------------------
		ServiceSetupRequest setupRequest = new ServiceSetupRequest("Mario Rossi", "Consulenza", 30, 5000,
				ServiceCurrency.EUR, "Europe/Rome", ServiceLanguage.IT, 24, false);
		ResponseEntity<ServiceResponse> setupResponse = restTemplate.exchange(url("/api/admin/service"), HttpMethod.PUT,
				new HttpEntity<>(setupRequest, mutatingHeaders(jar)), ServiceResponse.class);

		assertThat(setupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(setupResponse.getBody()).isNotNull();
		assertThat(setupResponse.getBody().stripeEnabled()).isFalse();

		// ---- 3. Slot creation (admin) — finestra oltre la soglia di ----
		LocalDate startDate = LocalDate.now().plusDays(10);
		LocalDate endDate = LocalDate.now().plusDays(11);
		AvailabilityGenerationRequest generationRequest = new AvailabilityGenerationRequest(startDate, endDate,
				EnumSet.allOf(DayOfWeek.class), LocalTime.of(9, 0), LocalTime.of(17, 0), null, null);
		ResponseEntity<AvailabilityGenerationResponse> generationResponse = restTemplate.exchange(
				url("/api/admin/availability/generate"), HttpMethod.POST,
				new HttpEntity<>(generationRequest, mutatingHeaders(jar)), AvailabilityGenerationResponse.class);

		assertThat(generationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(generationResponse.getBody()).isNotNull();
		assertThat(generationResponse.getBody().created()).isGreaterThan(0);

		// ---- 4. Public query free slots -----------------------------------
		Instant from = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
		Instant to = endDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

		List<AvailableSlotResponse> freeSlots = queryPublicAvailability(jar, from, to);
		assertThat(freeSlots).isNotEmpty();
		UUID slotId = freeSlots.get(0).id();

		// ---- 5. Public booking creation ------------------------------------
		BookingRequest bookingRequest = new BookingRequest(slotId, "Cliente Test", "cliente@test.local",
				"Prima visita");

		ResponseEntity<BookingCreatedResponse> bookingResponse = restTemplate.exchange(url("/api/public/bookings"),
				HttpMethod.POST, new HttpEntity<>(bookingRequest, mutatingHeaders(jar)), BookingCreatedResponse.class);

		assertThat(bookingResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		BookingCreatedResponse created = bookingResponse.getBody();
		assertThat(created).isNotNull();
		assertThat(created.status().name()).isEqualTo("CONFIRMED"); // stripeEnabled = false
		UUID cancellationToken = created.cancellationToken();

		// ---- 6. Double attempt creation on same slot → 409 ----------------------
		BookingRequest duplicateRequest = new BookingRequest(slotId, "Altro Cliente", "altro@test.local", null);

		ResponseEntity<String> duplicateResponse = restTemplate.exchange(url("/api/public/bookings"), HttpMethod.POST,
				new HttpEntity<>(duplicateRequest, mutatingHeaders(jar)), String.class);

		assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		// ---- 7. Admin slot --------------
		ResponseEntity<AdminSlotResponse[]> adminSlotsResponse = restTemplate.exchange(
				UriComponentsBuilder.fromUriString(url("/api/admin/availability")).queryParam("from", from)
						.queryParam("to", to).toUriString(),
				HttpMethod.GET, new HttpEntity<>(readHeaders(jar)), AdminSlotResponse[].class);

		assertThat(adminSlotsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		AdminSlotResponse bookedSlot = List.of(adminSlotsResponse.getBody()).stream().filter(s -> s.id().equals(slotId))
				.findFirst().orElseThrow();
		assertThat(bookedSlot.bookingStatus().name()).isEqualTo("CONFIRMED");
		assertThat(bookedSlot.bookingId()).isEqualTo(created.id());

		// ---- 8. Dashboard summary -------
		ResponseEntity<DashboardSummaryResponse> summaryResponse = restTemplate.exchange(
				url("/api/admin/dashboard/summary"), HttpMethod.GET, new HttpEntity<>(readHeaders(jar)),
				DashboardSummaryResponse.class);

		assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(summaryResponse.getBody()).isNotNull();
		assertThat(summaryResponse.getBody().lowAvailabilityWarning()).isFalse();

		// ---- 9. Self-service cancellation via token ---------------------------
		ResponseEntity<String> cancelResponse = restTemplate.exchange(
				url("/api/public/bookings/" + cancellationToken + "/cancel"), HttpMethod.POST,
				new HttpEntity<>(mutatingHeaders(jar)), String.class);

		assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		// ---- 10. Slot has to be recreated as free ---
		List<AvailableSlotResponse> freeSlotsAfterCancel = queryPublicAvailability(jar, from, to);
		assertThat(freeSlotsAfterCancel).extracting(AvailableSlotResponse::id).contains(slotId);

		// ---- 11. Same slot re-booking -----------
		BookingRequest rebookingRequest = new BookingRequest(slotId, "Cliente Nuovo", "nuovo@test.local", null);

		ResponseEntity<BookingCreatedResponse> rebookingResponse = restTemplate.exchange(url("/api/public/bookings"),
				HttpMethod.POST, new HttpEntity<>(rebookingRequest, mutatingHeaders(jar)),
				BookingCreatedResponse.class);

		assertThat(rebookingResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		UUID secondBookingId = rebookingResponse.getBody().id();

		// ---- 12. Override admin --------------------
		ResponseEntity<String> adminCancelResponse = restTemplate.exchange(
				url("/api/admin/bookings/" + secondBookingId + "/cancel"), HttpMethod.POST,
				new HttpEntity<>(mutatingHeaders(jar)), String.class);

		assertThat(adminCancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		// ---- 13. Logout ----------------------------------------------------------
		ResponseEntity<String> logoutResponse = restTemplate.exchange(url("/api/admin/auth/logout"), HttpMethod.POST,
				new HttpEntity<>(mutatingHeaders(jar)), String.class);

		assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	private List<AvailableSlotResponse> queryPublicAvailability(CookieJar jar, Instant from, Instant to) {
		String uri = UriComponentsBuilder.fromUriString(url("/api/public/availability")).queryParam("from", from)
				.queryParam("to", to).toUriString();

		ResponseEntity<AvailableSlotResponse[]> response = restTemplate.exchange(uri, HttpMethod.GET,
				new HttpEntity<>(readHeaders(jar)), AvailableSlotResponse[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return List.of(response.getBody());
	}
}
