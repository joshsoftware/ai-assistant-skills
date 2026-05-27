package com.joshsoftware.mockbank.mapper;

import com.joshsoftware.mockbank.dto.response.TransactionResponseDTO;
import com.joshsoftware.mockbank.entity.Transaction;
import com.joshsoftware.mockbank.enums.TransactionStatus;
import com.joshsoftware.mockbank.enums.TransactionType;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = {TransactionType.class, TransactionStatus.class, LocalDateTime.class}
)
public interface TransactionMapper {

    @Mapping(target = "fromAccountId", source = "fromAccount.id")
    @Mapping(target = "toAccountId",   source = "toAccount.id")
    @Mapping(target = "type",          expression = "java(transaction.getType().name())")
    @Mapping(target = "status",        expression = "java(transaction.getStatus().name())")
    TransactionResponseDTO toResponse(Transaction transaction);
}
