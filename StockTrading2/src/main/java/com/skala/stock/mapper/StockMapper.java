package com.skala.stock.mapper;

import com.skala.stock.dto.TradeSnapshotDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 집계/통계성 조회는 MyBatis SQL Mapper로 처리한다.
 * (단건 조회, CRUD는 JPA Repository를 사용한다)
 */
@Mapper
public interface StockMapper {

    // 총 자산 / 총 수익률 스냅샷 (현금잔고 + 보유종목 평가금액)
    TradeSnapshotDto getTradeSnapshot(@Param("userId") Long userId);

    // 종목별 매수/매도 거래 통계
    List<TransactionStatisticsDto> getTransactionStatistics(@Param("userId") Long userId);

    // 일별 거래 내역 통계
    List<DailyTransactionDto> getDailyTransactions(@Param("userId") Long userId);
}
