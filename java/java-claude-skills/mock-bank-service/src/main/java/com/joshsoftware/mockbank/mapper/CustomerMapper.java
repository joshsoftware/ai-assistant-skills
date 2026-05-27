package com.joshsoftware.mockbank.mapper;

import com.joshsoftware.mockbank.dto.request.CreateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateCustomerRequestDTO;
import com.joshsoftware.mockbank.dto.response.CustomerResponseDTO;
import com.joshsoftware.mockbank.entity.Customer;
import com.joshsoftware.mockbank.enums.KycStatus;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = {KycStatus.class, LocalDateTime.class}
)
public interface CustomerMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "kycStatus", expression = "java(KycStatus.PENDING)")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toEntity(CreateCustomerRequestDTO request);

    @Mapping(target = "kycStatus", expression = "java(customer.getKycStatus().name())")
    CustomerResponseDTO toResponse(Customer customer);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "kycStatus", expression = "java(request.getKycStatus() != null ? KycStatus.valueOf(request.getKycStatus()) : customer.getKycStatus())")
    void updateEntityFromRequest(UpdateCustomerRequestDTO request, @MappingTarget Customer customer);
}
