package com.movietickets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movietickets.domain.Show;
import com.movietickets.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "booking.hold-minutes=0")
class ExpiredHoldIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalog;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void confirmingExpiredHoldReturnsGone() throws Exception {
        Show show = TestCatalog.seedShow(catalog);
        MvcResult seats = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/shows/{id}/seats", show.getId()))
                .andReturn();
        long seatId = objectMapper.readTree(seats.getResponse().getContentAsString()).get(0).get("id").asLong();

        MvcResult hold = mvc.perform(post("/api/shows/{id}/holds", show.getId())
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"sam@example.com","showSeatIds":[%d]}
                                """.formatted(seatId)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(hold.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(post("/api/holds/{token}/confirm", token)
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\":\"sam@example.com\"}"))
                .andExpect(status().isGone());
    }
}
