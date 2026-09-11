package ru.rozhi;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class BannerControllerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0.0") // Укажите актуальную версию образа
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bannerRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void shouldReturnFirstBanner() {
        bannerRepository.save(Banner.builder().name("NAME1").description("DESCRIPTION1").build());
        bannerRepository.save(Banner.builder().name("NAME2").description("DESCRIPTION2").build());
        BannerResponse expected = new BannerResponse("NAME1", "DESCRIPTION1");

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        BannerResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), BannerResponse.class);

        assertThat(response).isEqualTo(expected);
    }
}
