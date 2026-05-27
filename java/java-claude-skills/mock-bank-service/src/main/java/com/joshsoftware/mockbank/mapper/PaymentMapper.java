package com.joshsoftware.mockbank.mapper;

import com.joshsoftware.mockbank.dto.response.PaymentResponseDTO;
import com.joshsoftware.mockbank.entity.Payment;
import com.joshsoftware.mockbank.enums.PaymentMode;
import com.joshsoftware.mockbank.enums.PaymentStatus;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = {PaymentMode.class, PaymentStatus.class, LocalDateTime.class}
)
public interface PaymentMapper {

    @Mapping(target = "transactionId", source = "transaction.id")
    @Mapping(target = "paymentMode",   expression = "java(payment.getPaymentMode().name())")
    @Mapping(target = "status",        expression = "java(payment.getStatus().name())")
    PaymentResponseDTO toResponse(Payment payment);
}
