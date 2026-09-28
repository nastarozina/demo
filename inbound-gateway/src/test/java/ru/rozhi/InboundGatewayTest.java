package ru.rozhi;

import com.github.tomakehurst.wiremock.WireMockServer;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.EnableWireMock;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableWireMock
@Testcontainers
public class InboundGatewayTest {

    @Container
    static final KeycloakContainer keycloak = new KeycloakContainer(
            DockerImageName.parse("keycloak/keycloak:26.7")
    ).withRealmImportFile("test-realm.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WireMockServer wireMockServer;

    private String validUserToken;
    private String validAdminToken;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Указываем Spring Security брать ключи из запущенного контейнера Keycloak
        String authServerUrl = keycloak.getAuthServerUrl();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> authServerUrl + "/realms/test-realm");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> authServerUrl + "/realms/test-realm/protocol/openid-connect/certs");
    }

    @BeforeEach
    void setUp() {
        validUserToken = obtainAccessToken("testuser", "password");
        validAdminToken = obtainAccessToken("adminuser", "password");
    }

    @Test
    void whenNoToken_thenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenInvalidToken_thenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-string"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenUserToken_thenReturns200AndForwardsUserIdHeader() throws Exception {
        // Настраиваем ожидание от WireMock: он должен получить запрос с конкретным заголовком
        wireMockServer.stubFor(get(urlEqualTo("/profile"))
                .withHeader("X-User-Id", equalTo("testuser-uuid")) // Subject из токена
                .withHeader("X-User-Email", equalTo("testuser@example.com"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Hello from user-service\"}")));

        // Выполняем запрос через Gateway
        mockMvc.perform(get("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from user-service"));

        // Проверяем, что WireMock действительно получил запрос (маршрутизация сработала)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/profile"))
                .withHeader("X-User-Id", equalTo("testuser-uuid")));
    }

    @Test
    void whenAdminTokenAccessingAdminEndpoint_thenReturns200() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/admin/data"))
                .willReturn(aResponse().withStatus(200).withBody("{\"role\": \"ADMIN\"}")));

        mockMvc.perform(get("/api/admin/data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void whenUserTokenAccessingAdminEndpoint_thenReturns403Forbidden() throws Exception {
        // У testuser нет роли ADMIN, только USER
        mockMvc.perform(get("/api/admin/data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validUserToken))
                .andExpect(status().isForbidden());
    }

    private String obtainAccessToken(String username, String password) {
        String tokenEndpoint = keycloak.getAuthServerUrl() + "/realms/test-realm/protocol/openid-connect/token";

        RestClient restClient = RestClient.builder()
                .baseUrl(tokenEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("Failed to obtain token: " + response.getStatusCode());
                })
                .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "gateway-client");
        formData.add("username", username);
        formData.add("password", password);

        Map<String, Object> response = restClient.post()
                .body(formData)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }
}
