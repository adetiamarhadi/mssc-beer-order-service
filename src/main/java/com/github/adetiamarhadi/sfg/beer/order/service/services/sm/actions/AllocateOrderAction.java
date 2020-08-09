package com.github.adetiamarhadi.sfg.beer.order.service.services.sm.actions;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.services.BeerOrderManagerImpl;
import com.github.adetiamarhadi.sfg.beer.order.service.web.mappers.BeerOrderMapper;
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
public class AllocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {

        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        BeerOrder beerOrder = this.beerOrderRepository.findOneById(UUID.fromString(beerOrderId));

        this.jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_QUEUE, this.beerOrderMapper.beerOrderToDto(beerOrder));

        log.debug("send allocation request for order id : " + beerOrderId);
    }
}
