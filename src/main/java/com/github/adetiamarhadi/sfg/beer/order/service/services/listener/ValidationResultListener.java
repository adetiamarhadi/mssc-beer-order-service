package com.github.adetiamarhadi.sfg.beer.order.service.services.listener;

import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.beer.order.service.services.BeerOrderManager;
import com.github.adetiamarhadi.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateOrderResult validateOrderResult) {

        final UUID beerOrderId = validateOrderResult.getOrderId();

        log.debug("validation result for order id : " + beerOrderId);

        this.beerOrderManager.processValidationResult(beerOrderId, validateOrderResult.getIsValid());
    }
}
