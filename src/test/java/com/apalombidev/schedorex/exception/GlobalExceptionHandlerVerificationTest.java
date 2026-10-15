package com.apalombidev.schedorex.exception;

import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import jakarta.validation.Valid;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

class GlobalExceptionHandlerVerificationTest {

	@RestController
	static class EchoController {
		@PostMapping("/test/service-setup")
		public ServiceSetupRequest echo(@Valid @RequestBody ServiceSetupRequest request) {
			return request;
		}
	}

	private MessageSource testMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasenames("i18n/messages");
		source.setDefaultEncoding(StandardCharsets.UTF_8.name());
		source.setDefaultLocale(Locale.ENGLISH);
		source.setUseCodeAsDefaultMessage(true);
		return source;
	}

	private MockMvc mockMvc() {
		return MockMvcBuilders.standaloneSetup(new EchoController())
				.setControllerAdvice(new GlobalExceptionHandler(testMessageSource())).build();
	}

	@Test
	void invalidCurrencyReturnsFieldSpecificMessage_notGenericFallback() throws Exception {
		String badJson = """
				{
				  "professionalName": "Mario Rossi",
				  "serviceName": "Consulenza",
				  "durationMinutes": 30,
				  "priceCents": 5000,
				  "currency": "USD",
				  "timezone": "Europe/Rome",
				  "language": "IT",
				  "cancellationDeadlineHours": 24,
				  "stripeEnabled": false
				}
				""";

		mockMvc().perform(post("/test/service-setup").contentType(MediaType.APPLICATION_JSON).content(badJson))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("currency")))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	@Test
	void invalidTimezoneReturnsCompactConstructorMessage_notGenericFallback() throws Exception {
		String badJson = """
				{
				  "professionalName": "Mario Rossi",
				  "serviceName": "Consulenza",
				  "durationMinutes": 30,
				  "priceCents": 5000,
				  "currency": "EUR",
				  "timezone": "Europa/Roma",
				  "language": "IT",
				  "cancellationDeadlineHours": 24,
				  "stripeEnabled": false
				}
				""";

		mockMvc().perform(post("/test/service-setup").contentType(MediaType.APPLICATION_JSON).content(badJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Fuso orario non valido")))
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	@Test
	void blankRequiredFieldTriggersBeanValidation_notMessageNotReadableHandler() throws Exception {
		String jsonWithBlankField = """
				{
				  "professionalName": "",
				  "serviceName": "Consulenza",
				  "durationMinutes": 30,
				  "priceCents": 5000,
				  "currency": "EUR",
				  "timezone": "Europe/Rome",
				  "language": "IT",
				  "cancellationDeadlineHours": 24,
				  "stripeEnabled": false
				}
				""";

		mockMvc()
				.perform(
						post("/test/service-setup").contentType(MediaType.APPLICATION_JSON).content(jsonWithBlankField))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("professionalName"));
	}
}