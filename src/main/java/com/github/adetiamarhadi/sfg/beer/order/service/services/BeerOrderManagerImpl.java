package com.github.adetiamarhadi.sfg.beer.order.service.services;

import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.services.sm.BeerOrderStateChangeInterceptor;
import com.github.adetiamarhadi.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = this.beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {

        log.debug("process validation result for beerOrderId: {} valid? {}", beerOrderId, isValid);

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (isValid) {

                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                BeerOrder validatedOrder = this.beerOrderRepository.findById(beerOrderId).get();

                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, () -> log.error("order not found. id: {}", beerOrderId));

    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocateQty(beerOrderDto);
        }, () -> log.error("order id not found: {}", beerOrderDto.getId()));
    }

    private void updateAllocateQty(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> allocatedOrderOptional = this.beerOrderRepository.findById(beerOrderDto.getId());
        allocatedOrderOptional.ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });
            this.beerOrderRepository.saveAndFlush(allocatedOrder);
        }, () -> log.error("order id not found: {}", beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            updateAllocateQty(beerOrderDto);
        }, () -> log.error("order id not found: {}", beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {

        Optional<BeerOrder> beerOrderOptional = this.beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
        }, () -> log.error("order id not found: {}", beerOrderDto.getId()));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);

        Message msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();

        sm.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm =
                this.stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(this.beerOrderStateChangeInterceptor);
                    sma.resetStateMachine(
                            new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null)
                    );
                });

        sm.start();

        return sm;
    }
}
