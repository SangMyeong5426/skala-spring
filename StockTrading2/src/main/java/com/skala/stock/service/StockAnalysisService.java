package com.skala.stock.service;

import com.skala.stock.dto.PortfolioDto;
import com.skala.stock.dto.PortfolioProfitLossDto;
import com.skala.stock.dto.TradeSnapshotDto;
import com.skala.stock.dto.TransactionDto;
import com.skala.stock.entity.Portfolio;
import com.skala.stock.entity.Stock;
import com.skala.stock.entity.Transaction;
import com.skala.stock.mapper.DailyTransactionDto;
import com.skala.stock.mapper.StockMapper;
import com.skala.stock.mapper.TransactionStatisticsDto;
import com.skala.stock.repository.PortfolioRepository;
import com.skala.stock.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 분석/고급 기능을 제공한다.
 * - 포트폴리오 평가손익, 종목별 거래내역: JPA (엔티티 그래프 기반 조회)
 * - 총자산/총수익률, 거래통계, 일별 거래내역: MyBatis (집계 SQL)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAnalysisService {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final StockMapper stockMapper;

    // 포트폴리오 평가 손익 조회 (JPA)
    public PortfolioProfitLossDto getPortfolioProfitLoss(Long userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

        List<PortfolioDto> holdings = portfolios.stream()
                .map(this::convertToPortfolioDto)
                .collect(Collectors.toList());

        long totalInvestment = portfolios.stream()
                .mapToLong(p -> p.getQuantity() * p.getAveragePrice())
                .sum();
        long totalValue = holdings.stream()
                .mapToLong(PortfolioDto::getTotalValue)
                .sum();

        return PortfolioProfitLossDto.builder()
                .userId(userId)
                .holdings(holdings)
                .totalInvestment(totalInvestment)
                .totalValue(totalValue)
                .totalProfitLoss(totalValue - totalInvestment)
                .build();
    }

    // 특정 주식 거래 내역 조회 - 전체 사용자 기준 (JPA)
    public List<TransactionDto> getStockTransactionHistory(Long stockId) {
        List<Transaction> transactions = transactionRepository.findByStockIdOrderByTransactionDateDesc(stockId);
        return transactions.stream()
                .map(this::convertToTransactionDto)
                .collect(Collectors.toList());
    }

    // 총 자산 조회 / 총 수익률 조회 (MyBatis)
    public TradeSnapshotDto getTradeSnapshot(Long userId) {
        return stockMapper.getTradeSnapshot(userId);
    }

    // 거래 통계 조회 (MyBatis)
    public List<TransactionStatisticsDto> getTransactionStatistics(Long userId) {
        return stockMapper.getTransactionStatistics(userId);
    }

    // 일별 거래 내역 조회 (MyBatis)
    public List<DailyTransactionDto> getDailyTransactions(Long userId) {
        return stockMapper.getDailyTransactions(userId);
    }

    private PortfolioDto convertToPortfolioDto(Portfolio portfolio) {
        Stock stock = portfolio.getStock();
        Long currentPrice = stock.getCurrentPrice();
        Long totalValue = portfolio.getQuantity() * currentPrice;
        Long profitLoss = totalValue - (portfolio.getQuantity() * portfolio.getAveragePrice());

        return PortfolioDto.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUser().getId())
                .username(portfolio.getUser().getUsername())
                .stockId(stock.getId())
                .stockCode(stock.getCode())
                .stockName(stock.getName())
                .quantity(portfolio.getQuantity())
                .averagePrice(portfolio.getAveragePrice())
                .currentPrice(currentPrice)
                .totalValue(totalValue)
                .profitLoss(profitLoss)
                .build();
    }

    private TransactionDto convertToTransactionDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .username(transaction.getUser().getUsername())
                .stockId(transaction.getStock().getId())
                .stockCode(transaction.getStock().getCode())
                .stockName(transaction.getStock().getName())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .totalAmount(transaction.getTotalAmount())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
