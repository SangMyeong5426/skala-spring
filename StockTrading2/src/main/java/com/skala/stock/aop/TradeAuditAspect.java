package com.skala.stock.aop;

import com.skala.stock.dto.TradeRequestDto;
import com.skala.stock.service.TradeAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * TransactionService.executeTrade(..) 호출을 가로채 실행시간을 측정하고,
 * 성공/실패 여부와 무관하게 거래 감사 로그(TradeAuditLog)를 남긴다.
 * 실제 저장은 TradeAuditLogService에서 REQUIRES_NEW 트랜잭션으로 처리하여
 * 매매 트랜잭션의 롤백 여부와 별개로 감사 로그가 남도록 한다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TradeAuditAspect {

    private final TradeAuditLogService tradeAuditLogService;

    @Around("execution(* com.skala.stock.service.TransactionService.executeTrade(..))")
    public Object auditTrade(ProceedingJoinPoint joinPoint) throws Throwable {
        TradeRequestDto request = (TradeRequestDto) joinPoint.getArgs()[0];
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[거래 성공] userId={}, stockId={}, type={}, quantity={}, {}ms",
                    request.getUserId(), request.getStockId(), request.getType(), request.getQuantity(), elapsed);
            tradeAuditLogService.recordSuccess(request);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[거래 실패] userId={}, stockId={}, type={}, quantity={}, {}ms, reason={}",
                    request.getUserId(), request.getStockId(), request.getType(), request.getQuantity(), elapsed, e.getMessage());
            tradeAuditLogService.recordFailure(request, e.getMessage());
            throw e;
        }
    }
}
