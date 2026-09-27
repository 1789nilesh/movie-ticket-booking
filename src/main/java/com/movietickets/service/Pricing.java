package com.movietickets.service;

import com.movietickets.domain.ShowSeat;

public final class Pricing {

    private Pricing() {
    }

    public static int priceCents(ShowSeat showSeat) {
        return showSeat.getSeat().isPremium()
                ? showSeat.getShow().getPremiumPriceCents()
                : showSeat.getShow().getRegularPriceCents();
    }
}
