package com.github.adetiamarhadi.beer.order.service.services.sm.actions;

import com.github.adetiamarhadi.brewery.model.events.DeallocateOrderRequest;
import com.github.adetiamarhadi.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.beer.order.service.services.BeerOrderManagerImpl;
import com.github.adetiamarhadi.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeallocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {

        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER);

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(UUID.fromString(beerOrderId));
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            this.jmsTemplate.convertAndSend(JmsConfig.DEALLOCATE_ORDER_QUEUE,
                    DeallocateOrderRequest.builder()
                            .beerOrderDto(this.beerOrderMapper.beerOrderToDto(beerOrder))
                            .build());
            log.debug("sent deallocation request for order id: {}", beerOrderId);
        }, () -> log.error("beer order not found"));
    }
}
