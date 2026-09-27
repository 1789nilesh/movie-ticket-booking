package com.movietickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movietickets.domain.Show;
import com.movietickets.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalog;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void holdThenConfirmBooksSeats() throws Exception {
        Show show = TestCatalog.seedShow(catalog);
        long regularSeatId = firstSeatId(show.getId());

        MvcResult holdResult = mvc.perform(post("/api/shows/{id}/holds", show.getId())
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"sam@example.com","showSeatIds":[%d]}
                                """.formatted(regularSeatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(holdResult.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(post("/api/holds/{token}/confirm", token)
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\":\"sam@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.totalPriceCents").value(25000));

        mvc.perform(get("/api/shows/{id}/seats", show.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }

    @Test
    void secondHoldOnSameSeatConflicts() throws Exception {
        Show show = TestCatalog.seedShow(catalog);
        long seatId = firstSeatId(show.getId());
        mvc.perform(post("/api/shows/{id}/holds", show.getId())
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"sam@example.com","showSeatIds":[%d]}
                                """.formatted(seatId)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/shows/{id}/holds", show.getId())
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"alex@example.com","showSeatIds":[%d]}
                                """.formatted(seatId)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminRoleIsRequiredForCatalogWrites() throws Exception {
        mvc.perform(post("/api/admin/cities")
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chennai\"}"))
                .andExpect(status().isForbidden());
    }

    private long firstSeatId(Long showId) throws Exception {
        MvcResult result = mvc.perform(get("/api/shows/{id}/seats", showId)).andExpect(status().isOk()).andReturn();
        JsonNode seats = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(seats).isNotEmpty();
        return seats.get(0).get("id").asLong();
    }
}
