package com.skala.stock.controller;

import com.skala.stock.dto.PortfolioProfitLossDto;
import com.skala.stock.dto.TradeSnapshotDto;
import com.skala.stock.dto.TransactionDto;
import com.skala.stock.mapper.DailyTransactionDto;
import com.skala.stock.mapper.TransactionStatisticsDto;
import com.skala.stock.service.StockAnalysisService;
import com.skala.stock.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "분석/고급 기능", description = "포트폴리오/거래 분석 API")
public class StockAnalysisController {

    private final StockAnalysisService stockAnalysisService;
    private final TransactionService transactionService;

    @GetMapping("/portfolio/{userId}/profit-loss")
    @Operation(summary = "포트폴리오 평가 손익 조회", description = "사용자 보유 종목의 평가 손익을 조회합니다")
    public ResponseEntity<PortfolioProfitLossDto> getPortfolioProfitLoss(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getPortfolioProfitLoss(userId));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "거래 내역 상세 조회", description = "거래 ID로 거래 내역을 상세 조회합니다")
    public ResponseEntity<TransactionDto> getTransactionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/transactions/stock/{stockId}")
    @Operation(summary = "특정 주식 거래 내역 조회", description = "특정 종목의 전체 사용자 거래 내역을 조회합니다")
    public ResponseEntity<List<TransactionDto>> getStockTransactionHistory(@PathVariable Long stockId) {
        return ResponseEntity.ok(stockAnalysisService.getStockTransactionHistory(stockId));
    }

    @GetMapping("/assets/{userId}")
    @Operation(summary = "총 자산 조회", description = "현금 잔고와 보유 종목 평가금액을 합산한 총 자산을 조회합니다")
    public ResponseEntity<TradeSnapshotDto> getTotalAssets(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getTradeSnapshot(userId));
    }

    @GetMapping("/return-rate/{userId}")
    @Operation(summary = "총 수익률 조회", description = "보유 종목의 평가손익률을 조회합니다")
    public ResponseEntity<Double> getTotalReturnRate(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getTradeSnapshot(userId).getTotalReturnRate());
    }

    @GetMapping("/statistics/{userId}")
    @Operation(summary = "거래 통계 조회", description = "종목별 매수/매도 거래 통계를 조회합니다")
    public ResponseEntity<List<TransactionStatisticsDto>> getTransactionStatistics(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getTransactionStatistics(userId));
    }

    @GetMapping("/transactions/{userId}/daily")
    @Operation(summary = "일별 거래 내역 조회", description = "사용자의 일자별 거래 통계를 조회합니다")
    public ResponseEntity<List<DailyTransactionDto>> getDailyTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(stockAnalysisService.getDailyTransactions(userId));
    }
}
