package com.github.adetiamarhadi.beer.order.service.web.mappers;

import com.github.adetiamarhadi.beer.order.service.domain.Customer;
import com.github.adetiamarhadi.brewery.model.CustomerDto;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
public interface CustomerMapper {

    CustomerDto customerToCustomerDto(Customer customer);

    Customer customerDtoToCustomer(CustomerDto customerDto);
}
