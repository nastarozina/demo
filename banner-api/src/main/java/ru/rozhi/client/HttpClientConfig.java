package ru.rozhi.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import ru.rozhi.ApiErrorResponse;
import ru.rozhi.exception.ExternalException;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class HttpClientConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    RestClient categoryApiRestClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8001")
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            ApiErrorResponse errorResponse = objectMapper.readValue(
                                    response.getBody(),
                                    ApiErrorResponse.class
                            );
                            throw new ExternalException(
                                    errorResponse.code(),
                                    errorResponse.message()
                            );
                        }
                )
                .build();
    }

    @Bean
    CategoryClient categoryClient(RestClient categoryApiRestClient) {
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(categoryApiRestClient))
                        .build();
        return httpServiceProxyFactory.createClient(CategoryClient.class);
    }
}