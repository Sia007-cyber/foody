package com.foody.wallet.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record AmountRequest(@NotNull @DecimalMin(value="0.01") @Digits(integer=10,fraction=2) BigDecimal amount){}
