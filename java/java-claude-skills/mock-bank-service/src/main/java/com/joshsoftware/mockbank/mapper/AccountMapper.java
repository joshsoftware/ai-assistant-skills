package com.joshsoftware.mockbank.mapper;

import com.joshsoftware.mockbank.dto.request.CreateAccountRequestDTO;
import com.joshsoftware.mockbank.dto.response.AccountResponseDTO;
import com.joshsoftware.mockbank.entity.Account;
import com.joshsoftware.mockbank.entity.Customer;
import com.joshsoftware.mockbank.enums.AccountStatus;
import com.joshsoftware.mockbank.enums.AccountType;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = {AccountType.class, AccountStatus.class, LocalDateTime.class}
)
public interface AccountMapper {

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "customer",      source = "customer")
    @Mapping(target = "accountType",   expression = "java(AccountType.valueOf(request.getAccountType()))")
    @Mapping(target = "status",        expression = "java(AccountStatus.ACTIVE)")
    @Mapping(target = "createdAt",     expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt",     expression = "java(LocalDateTime.now())")
    @Mapping(target = "createdBy",     ignore = true)
    @Mapping(target = "updatedBy",     ignore = true)
    Account toEntity(CreateAccountRequestDTO request, Customer customer);

    @Mapping(target = "customerId",    source = "customer.id")
    @Mapping(target = "accountType",   expression = "java(account.getAccountType().name())")
    @Mapping(target = "status",        expression = "java(account.getStatus().name())")
    AccountResponseDTO toResponse(Account account);
}
