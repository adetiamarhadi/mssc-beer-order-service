package com.github.adetiamarhadi.sfg.beer.order.service.services.testcomponents;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.brewery.model.events.ValidateOrderRequest;
import com.github.adetiamarhadi.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void list(Message msg) {

        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();

        System.out.println("######### I Ran #########");

        this.jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderResult.builder()
                        .isValid(true)
                        .orderId(request.getBeerOrderDto().getId())
                        .build());
    }
}
