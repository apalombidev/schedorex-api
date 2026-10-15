package com.apalombidev.schedorex.config;

import com.apalombidev.schedorex.auth.AdminUser;
import com.apalombidev.schedorex.auth.AdminUserRepository;
import com.apalombidev.schedorex.auth.JwtService;
import com.apalombidev.schedorex.auth.dto.LoginRequest;
import com.apalombidev.schedorex.service.ServiceCurrency;
import com.apalombidev.schedorex.service.ServiceLanguage;
import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class SecurityIntegrationTest {

	private static final String RAW_PASSWORD = "TestPassword123!";
	private static final String ADMIN_EMAIL = "security-test-admin@example.com";

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

		adminUserRepository.saveAndFlush(
				AdminUser.builder().email(ADMIN_EMAIL).passwordHash(passwordEncoder.encode(RAW_PASSWORD)).build());
	}

	@Test
	void publicEndpointIsReachableWithoutAnyCookie() throws Exception {
		mockMvc.perform(get("/api/public/service")).andExpect(status().isOk());
	}

	@Test
	void adminEndpointWithoutCookieReturns401WithErrorResponseShape() throws Exception {
		mockMvc.perform(get("/api/admin/service")).andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void loginWithCorrectCredentialsSetsHttpOnlySameSiteStrictCookie() throws Exception {
		LoginRequest request = new LoginRequest(ADMIN_EMAIL, RAW_PASSWORD);

		mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(cookie().exists("jwt")).andExpect(cookie().httpOnly("jwt", true))
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")));
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		LoginRequest request = new LoginRequest(ADMIN_EMAIL, "wrong-password");

		mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isUnauthorized());
	}

	@Test
	void adminEndpointWithValidCookieReturns200() throws Exception {
		String jwt = performLoginAndExtractJwt();

		mockMvc.perform(get("/api/admin/service").cookie(new Cookie("jwt", jwt))).andExpect(status().isOk());
	}

	@Test
	void adminEndpointWithTamperedCookieReturns401() throws Exception {
		String jwt = performLoginAndExtractJwt();
		String tampered = jwt.substring(0, jwt.length() - 5) + "AAAAA";

		mockMvc.perform(get("/api/admin/service").cookie(new Cookie("jwt", tampered)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminEndpointWithGarbageCookieValueReturns401() throws Exception {
		mockMvc.perform(get("/api/admin/service").cookie(new Cookie("jwt", "not-a-jwt-at-all")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicBookingEndpointsRemainOpenEvenWithNoCookie() throws Exception {
		mockMvc.perform(get("/api/public/bookings/" + java.util.UUID.randomUUID()))
				.andExpect(status().is(org.hamcrest.Matchers.not(401)));
	}

	@Test
	void adminMutatingEndpointWithInvalidRequestReturns400() throws Exception {
		String jwt = performLoginAndExtractJwt();

		mockMvc.perform(put("/api/admin/service").with(csrf()).cookie(new Cookie("jwt", jwt))
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
	}

	@Test
	void adminMutatingEndpointWithCsrfTokenIsAccepted() throws Exception {
		String jwt = performLoginAndExtractJwt();

		ServiceSetupRequest request = new ServiceSetupRequest("Dr. Mario Rossi", "Consultazione", 60, 5000,
				ServiceCurrency.EUR, "Europe/Rome", ServiceLanguage.IT, 24, true);

		mockMvc.perform(put("/api/admin/service").with(csrf()).cookie(new Cookie("jwt", jwt))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());
	}

	@Test
	void adminMutatingEndpointWithoutCsrfTokenReturns403() throws Exception {
		String jwt = performLoginAndExtractJwt();

		mockMvc.perform(put("/api/admin/service").cookie(new Cookie("jwt", jwt)).contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isForbidden());
	}

	@Test
	void adminMutatingEndpointWithCsrfTokenIsNotBlockedByCsrf() throws Exception {
		String jwt = performLoginAndExtractJwt();

		mockMvc.perform(put("/api/admin/service").with(csrf()).cookie(new Cookie("jwt", jwt))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().is(org.hamcrest.Matchers.not(403)));
	}

	@Test
	void loginItselfDoesNotRequireCsrfToken() throws Exception {
		LoginRequest request = new LoginRequest(ADMIN_EMAIL, RAW_PASSWORD);

		mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
	}

	@Test
	void authenticatedButWrongRoleReturns403() throws Exception {
		String tokenWithWrongRole = jwtService.generateToken("someone@example.com", List.of("STAFF")).token();

		mockMvc.perform(get("/api/admin/service").cookie(new Cookie("jwt", tokenWithWrongRole)))
				.andExpect(status().isForbidden());
	}

	@Test
	void logoutClearsCookieWithMaxAgeZeroEvenWithoutValidJwt() throws Exception {
		mockMvc.perform(post("/api/admin/auth/logout")).andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
	}

	@Test
	void logoutClearsCookieForAuthenticatedUser() throws Exception {
		String jwt = performLoginAndExtractJwt();

		mockMvc.perform(post("/api/admin/auth/logout").cookie(new Cookie("jwt", jwt))).andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
	}

	private String performLoginAndExtractJwt() throws Exception {
		LoginRequest request = new LoginRequest(ADMIN_EMAIL, RAW_PASSWORD);
		var result = mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andReturn();
		Cookie jwtCookie = result.getResponse().getCookie("jwt");
		org.junit.jupiter.api.Assertions.assertNotNull(jwtCookie);
		return jwtCookie.getValue();
	}
}