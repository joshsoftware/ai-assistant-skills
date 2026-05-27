package com.joshsoftware.mockbank.controller;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.constants.CustomerConstants;
import com.joshsoftware.mockbank.dto.ApiResponse;
import com.joshsoftware.mockbank.dto.request.CreateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.response.CustomerResponseDTO;
import com.joshsoftware.mockbank.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> create(
            @Valid @RequestBody CreateCustomerRequestDTO request) {

        log.info("[CustomerController#create] ENTRY - email={}", request.getEmail());
        CustomerResponseDTO response = customerService.create(request);
        log.info("[CustomerController#create] EXIT  - customerId={}", response.getId());
        return ResponseEntity.ok(ApiResponse.success(CustomerConstants.CUSTOMER_CREATED, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponseDTO>>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        log.info("[CustomerController#findAll] ENTRY - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<CustomerResponseDTO> page = customerService.findAll(pageable);
        log.info("[CustomerController#findAll] EXIT  - totalElements={}", page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(CustomerConstants.CUSTOMERS_FETCHED, page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequestDTO request) {

        log.info("[CustomerController#update] ENTRY - customerId={}", id);
        CustomerResponseDTO response = customerService.update(id, request);
        log.info("[CustomerController#update] EXIT  - customerId={}", id);
        return ResponseEntity.ok(ApiResponse.success(CustomerConstants.CUSTOMER_UPDATED, response));
    }
}
