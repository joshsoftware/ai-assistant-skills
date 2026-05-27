package com.joshsoftware.mockbank.service;

import com.joshsoftware.mockbank.dto.request.CreateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.response.CustomerResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    // Creates a new customer from the given request payload.
    CustomerResponseDTO create(CreateCustomerRequestDTO request);

    // Returns a paginated list of all customers.
    Page<CustomerResponseDTO> findAll(Pageable pageable);

    // Updates an existing customer by UUID.
    CustomerResponseDTO update(UUID id, UpdateCustomerRequestDTO request);
}
