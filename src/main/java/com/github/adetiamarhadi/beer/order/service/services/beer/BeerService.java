package com.github.adetiamarhadi.beer.order.service.services.beer;

import com.github.adetiamarhadi.brewery.model.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDto> getBeerById(UUID beerId);

    Optional<BeerDto> getBeerByUpc(String upc);
}
