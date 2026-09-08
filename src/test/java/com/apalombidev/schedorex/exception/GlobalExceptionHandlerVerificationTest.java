package com.apalombidev.schedorex.exception;

import com.apalombidev.schedorex.service.dto.ServiceSetupRequest;

import jakarta.validation.Valid;

import org.junit.jupiter.api.Test;
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

/**
 * Verifica mirata dei due punti lasciati aperti nel checkpoint Step 6
 * (v7/v8/v9): che HttpMessageNotReadableException (enum/timezone non
 * validi) e MethodArgumentNotValidException (violazioni @Valid) non si
 * mascherino a vicenda, e che il messaggio specifico su un campo enum o
 * sul timezone venga davvero restituito invece del fallback generico.
 *
 * Deliberatamente NON usa @WebMvcTest/@SpringBootTest: MockMvcBuilders.
 * standaloneSetup() monta un solo controller-esca "a mano", collegato
 * al vero GlobalExceptionHandler, senza caricare il contesto Spring Boot
 * completo. Questo evita di dipendere da annotazioni di test-slice che in
 * Boot 4.1 sono state modularizzate/spostate (vedi v6, nota su
 * @DataJpaTest) — qui non servono, MockMvc "base" è sufficiente e stabile.
 *
 * Non è un sostituto del test manuale end-to-end via Postman/curl
 * sull'app reale (i filtri di sicurezza dello Step 8, il converter JSON
 * effettivamente configurato da Spring Boot a runtime potrebbero avere
 * sfumature diverse), ma verifica lo stesso identico percorso di codice
 * (Jackson 3.x → deserializzazione → GlobalExceptionHandler) con lo
 * stesso ObjectMapper che Spring MVC usa di default per @RequestBody.
 */
class GlobalExceptionHandlerVerificationTest {

    /**
     * Controller-esca, esiste solo per questo test. Non fa parte
     * dell'applicazione reale (Step 7 non è ancora scritto) — riceve lo
     * stesso ServiceSetupRequest reale dello Step 4, con lo stesso @Valid
     * che userà il controller admin definitivo.
     */
    @RestController
    static class EchoController {
        @PostMapping("/test/service-setup")
        public ServiceSetupRequest echo(@Valid @RequestBody ServiceSetupRequest request) {
            return request;
        }
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new EchoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * Scenario esplicito richiesto da v7/v8: "currency": "USD" non è nel
     * set chiuso {EUR, GBP} (v4). Deve fallire in DESERIALIZZAZIONE
     * (prima ancora che Bean Validation entri in gioco, dato che il body
     * non si riesce a costruire), quindi deve passare per
     * HttpMessageNotReadableException → messaggio specifico sul campo
     * "currency", NON per il fallback generico (500) né per
     * MethodArgumentNotValidException (400 con fieldErrors vuoti).
     */
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

        mockMvc().perform(post("/test/service-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("currency")))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    /**
     * Scenario esplicito richiesto da v8: "timezone": "Europa/Roma" (typo,
     * non un ID IANA valido). currency/language sono validi qui, quindi la
     * deserializzazione dell'enum passa senza problemi — l'unico punto di
     * fallimento è il compact constructor di ServiceSetupRequest, che
     * lancia IllegalArgumentException("Fuso orario non valido: ..."),
     * incapsulata da Jackson in una ValueInstantiationException e poi in
     * HttpMessageNotReadableException da Spring.
     *
     * Verifica che l'handler risalga la catena delle cause fino a trovare
     * questo IllegalArgumentException (ramo "else if" dell'handler, non il
     * ramo InvalidFormatException) e restituisca il messaggio ESATTO del
     * compact constructor, non un messaggio generico.
     */
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

        mockMvc().perform(post("/test/service-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Fuso orario non valido")))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    /**
     * Controllo di non-regressione richiesto implicitamente da v7: un body
     * sintatticamente valido (enum e timezone corretti) ma con un campo
     * @NotBlank vuoto deve invece attivare MethodArgumentNotValidException,
     * non essere "rubato" dall'handler di HttpMessageNotReadableException.
     * Conferma che i due handler coesistono senza mascherarsi a vicenda
     * nei DUE sensi (non solo che l'uno non ruba scenari dell'altro, ma
     * anche il viceversa).
     */
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

        mockMvc().perform(post("/test/service-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithBlankField))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("professionalName"));
    }
}