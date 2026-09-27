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
class DiscountAndWeekendPricingTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogService catalog;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void confirmWithDiscountCodeAppliesDiscount() throws Exception {
        Show show = TestCatalog.seedShow(catalog);
        long seatId = firstSeatId(show.getId());

        mvc.perform(post("/api/admin/discounts")
                        .header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"SAVE20","discountPercentage":20}
                                """))
                .andExpect(status().isCreated());

        MvcResult holdResult = mvc.perform(post("/api/shows/{id}/holds", show.getId())
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"disc@example.com","showSeatIds":[%d]}
                                """.formatted(seatId)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(holdResult.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(post("/api/holds/{token}/confirm", token)
                        .header("X-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail":"disc@example.com","discountCode":"SAVE20"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTotalPriceCents").value(25000))
                .andExpect(jsonPath("$.discountAmountCents").value(5000))
                .andExpect(jsonPath("$.totalPriceCents").value(20000))
                .andExpect(jsonPath("$.discountCode").value("SAVE20"));
    }

    private long firstSeatId(Long showId) throws Exception {
        MvcResult result = mvc.perform(get("/api/shows/{id}/seats", showId)).andExpect(status().isOk()).andReturn();
        JsonNode seats = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(seats).isNotEmpty();
        return seats.get(0).get("id").asLong();
    }
}
