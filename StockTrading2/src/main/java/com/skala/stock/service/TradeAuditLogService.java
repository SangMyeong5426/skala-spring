package com.skala.stock.service;

import com.skala.stock.dto.TradeRequestDto;
import com.skala.stock.dto.TradeSnapshotDto;
import com.skala.stock.entity.TradeAuditLog;
import com.skala.stock.mapper.StockMapper;
import com.skala.stock.repository.TradeAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 감사 로그를 REQUIRES_NEW(별도 트랜잭션)로 기록한다.
 * 매매 트랜잭션이 롤백되더라도 감사 로그는 독립적으로 남아야 하기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeAuditLogService {

    private final TradeAuditLogRepository tradeAuditLogRepository;
    private final StockMapper stockMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(TradeRequestDto request) {
        TradeSnapshotDto snapshot = stockMapper.getTradeSnapshot(request.getUserId());

        TradeAuditLog auditLog = TradeAuditLog.builder()
                .userId(request.getUserId())
                .stockId(request.getStockId())
                .type(request.getType())
                .message("거래가 정상적으로 처리되었습니다.")
                .totalAssets(snapshot.getTotalAssets())
                .totalReturnRate(snapshot.getTotalReturnRate())
                .build();
        tradeAuditLogRepository.save(auditLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(TradeRequestDto request, String reason) {
        TradeAuditLog auditLog = TradeAuditLog.builder()
                .userId(request.getUserId())
                .stockId(request.getStockId())
                .type(request.getType())
                .message("거래 실패: " + reason)
                .build();
        tradeAuditLogRepository.save(auditLog);
    }
}
