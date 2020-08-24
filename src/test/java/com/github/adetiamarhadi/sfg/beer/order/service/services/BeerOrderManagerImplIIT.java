package com.github.adetiamarhadi.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.adetiamarhadi.sfg.beer.order.service.config.JmsConfig;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrder;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderLine;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import com.github.adetiamarhadi.sfg.beer.order.service.domain.Customer;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.BeerOrderRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.repositories.CustomerRepository;
import com.github.adetiamarhadi.sfg.beer.order.service.services.beer.BeerServiceImpl;
import com.github.adetiamarhadi.sfg.brewery.model.BeerDto;

import com.github.adetiamarhadi.sfg.brewery.model.events.AllocationFailureEvent;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImplIIT {

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider {
        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = this.customerRepository.save(Customer.builder()
                .customerName("test customer")
                .build());
    }

    @Test
    void testNewToAllocated() throws Exception {

        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        this.wireMockServer.stubFor(
                get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                        .willReturn(okJson(this.objectMapper.writeValueAsString(beerDto)))
        );

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        BeerOrder savedBeerOrder2 = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(savedBeerOrder2);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());

        savedBeerOrder2.getBeerOrderLines().forEach(line -> {
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        this.wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345").willReturn(okJson(this.objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        this.beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

        BeerOrder pickedUpOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedUpOrder.getOrderStatus());
    }

    @Test
    void testFailedValidation() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        this.wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                .willReturn(okJson(this.objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-validation");

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, foundOrder.getOrderStatus());
        });
    }

    @Test
    void testAllocationFailure() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        this.wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                .willReturn(okJson(this.objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-allocation");

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, foundOrder.getOrderStatus());
        });

        AllocationFailureEvent allocationFailureEvent =
                (AllocationFailureEvent) this.jmsTemplate.receiveAndConvert(JmsConfig.ALLOCATION_FAILURE_QUEUE);

        assertNotNull(allocationFailureEvent);
        assertThat(allocationFailureEvent.getOrderId()).isEqualTo(savedBeerOrder.getId());
    }

    @Test
    void testPartialAllocation() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        this.wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + "12345")
                .willReturn(okJson(this.objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("partial-allocation");

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, foundOrder.getOrderStatus());
        });
    }

    private BeerOrder createBeerOrder() {

        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }
}
