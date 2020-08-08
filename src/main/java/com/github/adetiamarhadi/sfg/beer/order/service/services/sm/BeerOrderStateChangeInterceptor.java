package com.github.adetiamarhadi.sfg.beer.order.service.services.sm;

import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderEventEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeInterceptor
        extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message, Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.ORDER_ID_HEADER, " ")))
                .ifPresent(orderId -> {
                    log.debug("saving state for order id : " + orderId + " status : " + state.getId());

                    BeerOrder beerOrder = this.beerOrderRepository.getOne(UUID.fromString(orderId));
                    beerOrder.setOrderStatus(state.getId());
                    this.beerOrderRepository.saveAndFlush(beerOrder);
                });
    }
}