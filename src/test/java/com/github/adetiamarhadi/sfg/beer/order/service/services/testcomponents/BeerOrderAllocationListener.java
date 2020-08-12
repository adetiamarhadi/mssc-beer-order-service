package com.github.adetiamarhadi.sfg.beer.order.service.services.testcomponents;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.brewery.model.events.AllocateOrderRequest;
import com.github.adetiamarhadi.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
        });

        this.jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                        .beerOrderDto(request.getBeerOrderDto())
                        .pendingInventory(false)
                        .allocationError(false)
                        .build());
    }
}
