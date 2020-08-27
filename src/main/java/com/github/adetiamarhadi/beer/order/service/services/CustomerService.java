package com.github.adetiamarhadi.beer.order.service.services;

import com.github.adetiamarhadi.brewery.model.CustomerPagedList;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    CustomerPagedList listCustomers(Pageable pageable);
}
