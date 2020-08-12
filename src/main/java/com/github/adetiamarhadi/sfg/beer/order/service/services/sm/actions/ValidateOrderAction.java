package com.github.adetiamarhadi.sfg.beer.order.service.services.sm.actions;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.services.BeerOrderManagerImpl;
import com.github.adetiamarhadi.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import com.github.adetiamarhadi.sfg.brewery.model.events.ValidateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(UUID.fromString(beerOrderId));
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE,
                    ValidateOrderRequest.builder()
                            .beerOrderDto(this.beerOrderMapper.beerOrderToDto(beerOrder))
                            .build());
        }, () -> log.error("order not found. id: {}", beerOrderId));
    }
}
