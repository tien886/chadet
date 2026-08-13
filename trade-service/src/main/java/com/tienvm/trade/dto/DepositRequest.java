package com.tienvm.trade.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DepositRequest(
		@NotNull(message = "Amount is required")
		@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
		BigDecimal amount
) {
}
