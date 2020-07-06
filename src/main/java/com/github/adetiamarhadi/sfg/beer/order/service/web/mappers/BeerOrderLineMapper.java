package com.github.adetiamarhadi.sfg.beer.order.service.web.mappers;

import com.github.adetiamarhadi.sfg.beer.order.service.web.model.BeerOrderLineDto;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderLine;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
public interface BeerOrderLineMapper {
    BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line);

    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto);
}
