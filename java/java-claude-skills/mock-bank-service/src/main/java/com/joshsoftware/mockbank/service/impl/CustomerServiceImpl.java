package com.joshsoftware.mockbank.service.impl;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.dto.request.CreateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.response.CustomerResponseDTO;
import com.joshsoftware.mockbank.entity.Customer;
import com.joshsoftware.mockbank.exception.DuplicateResourceException;
import com.joshsoftware.mockbank.exception.ResourceNotFoundException;
import com.joshsoftware.mockbank.mapper.CustomerMapper;
import com.joshsoftware.mockbank.repository.CustomerRepository;
import com.joshsoftware.mockbank.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper     customerMapper;

    // Creates a new customer and persists it.
    @Override
    @Transactional
    public CustomerResponseDTO create(CreateCustomerRequestDTO request) {
        log.debug("[CustomerServiceImpl#create] ENTRY - email={}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer", "email", request.getEmail());
        }

        Customer customer = customerMapper.toEntity(request);
        Customer createdCustomer = customerRepository.save(customer);

        log.debug("[CustomerServiceImpl#create] EXIT - customerId={}", createdCustomer.getId());
        return customerMapper.toResponse(createdCustomer);
    }

    // Returns a paginated list of all customers.
    @Override
    public Page<CustomerResponseDTO> findAll(Pageable pageable) {
        log.debug("[CustomerServiceImpl#findAll] ENTRY - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<CustomerResponseDTO> page = customerRepository.findAll(pageable)
                .map(customerMapper::toResponse);

        log.debug("[CustomerServiceImpl#findAll] EXIT - totalElements={}", page.getTotalElements());
        return page;
    }

    // Updates an existing customer by UUID.
    @Override
    @Transactional
    public CustomerResponseDTO update(UUID id, UpdateCustomerRequestDTO request) {
        log.debug("[CustomerServiceImpl#update] ENTRY - customerId={}", id);

        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customerMapper.updateEntityFromRequest(request, existing);
        Customer updatedCustomer = customerRepository.save(existing);

        log.debug("[CustomerServiceImpl#update] EXIT - customerId={}", id);
        return customerMapper.toResponse(updatedCustomer);
    }
}
