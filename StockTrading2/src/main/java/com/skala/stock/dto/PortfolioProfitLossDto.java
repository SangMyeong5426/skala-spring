package com.skala.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioProfitLossDto {
    private Long userId;
    private List<PortfolioDto> holdings;
    private Long totalInvestment; // 총 매입 원가
    private Long totalValue;      // 총 평가 금액
    private Long totalProfitLoss; // 총 평가 손익
}
