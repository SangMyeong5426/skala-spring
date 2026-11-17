package com.skala.stock.mapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTransactionDto {
    private LocalDate tradeDate;
    private Long buyCount;
    private Long sellCount;
    private Long buyAmount;
    private Long sellAmount;
    private Long totalAmount;
}
