package com.github.adetiamarhadi.sfg.beer.order.service.services.sm.actions;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.services.BeerOrderManagerImpl;
import com.github.adetiamarhadi.sfg.brewery.model.events.AllocationFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {

        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        this.jmsTemplate.convertAndSend(JmsConfig.ALLOCATION_FAILURE_QUEUE, AllocationFailureEvent.builder()
                .orderId(UUID.fromString(beerOrderId))
                .build());

        log.debug("sent allocation failure message to queue for order id {}", beerOrderId);
    }
}
