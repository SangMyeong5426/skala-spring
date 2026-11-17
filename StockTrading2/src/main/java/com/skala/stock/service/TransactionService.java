package com.skala.stock.service;

import com.skala.stock.dto.TradeRequestDto;
import com.skala.stock.dto.TransactionDto;
import com.skala.stock.entity.Portfolio;
import com.skala.stock.entity.Stock;
import com.skala.stock.entity.Transaction;
import com.skala.stock.entity.User;
import com.skala.stock.repository.PortfolioRepository;
import com.skala.stock.repository.StockRepository;
import com.skala.stock.repository.TransactionRepository;
import com.skala.stock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<TransactionDto> getUserTransactions(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public TransactionDto getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("거래 내역을 찾을 수 없습니다: " + id));
        return convertToDto(transaction);
    }

    public List<TransactionDto> getUserStockTransactions(Long userId, Long stockId) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndStockIdOrderByTransactionDateDesc(userId, stockId);
        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 주식 매수/매도를 실행한다.
     * BUY: 잔고 확인 후 차감, 포트폴리오 가중평균 매입가 갱신
     * SELL: 보유수량 확인 후 잔고 증가, 포트폴리오 차감(전량 매도 시 삭제)
     */
    @Transactional
    public TransactionDto executeTrade(TradeRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다: " + request.getStockId()));

        Long quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("거래 수량은 1 이상이어야 합니다");
        }

        Long price = stock.getCurrentPrice();
        Long totalAmount = price * quantity;

        if (request.getType() == Transaction.TransactionType.BUY) {
            executeBuy(user, stock, quantity, price, totalAmount);
        } else {
            executeSell(user, stock, quantity, totalAmount);
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .stock(stock)
                .type(request.getType())
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        return convertToDto(savedTransaction);
    }

    private void executeBuy(User user, Stock stock, Long quantity, Long price, Long totalAmount) {
        if (user.getBalance() < totalAmount) {
            throw new RuntimeException("잔고가 부족합니다. 필요 금액: " + totalAmount + ", 보유 잔고: " + user.getBalance());
        }
        user.setBalance(user.getBalance() - totalAmount);

        Portfolio portfolio = portfolioRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElse(null);
        if (portfolio == null) {
            portfolio = Portfolio.builder()
                    .user(user)
                    .stock(stock)
                    .quantity(quantity)
                    .averagePrice(price)
                    .build();
        } else {
            long newQuantity = portfolio.getQuantity() + quantity;
            long newTotalCost = portfolio.getQuantity() * portfolio.getAveragePrice() + quantity * price;
            portfolio.setQuantity(newQuantity);
            portfolio.setAveragePrice(newTotalCost / newQuantity);
        }
        portfolioRepository.save(portfolio);
    }

    private void executeSell(User user, Stock stock, Long quantity, Long totalAmount) {
        Portfolio portfolio = portfolioRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseThrow(() -> new RuntimeException("보유하지 않은 종목입니다: " + stock.getCode()));

        if (portfolio.getQuantity() < quantity) {
            throw new RuntimeException("보유 수량이 부족합니다. 요청 수량: " + quantity + ", 보유 수량: " + portfolio.getQuantity());
        }

        user.setBalance(user.getBalance() + totalAmount);

        long remaining = portfolio.getQuantity() - quantity;
        if (remaining == 0) {
            portfolioRepository.delete(portfolio);
        } else {
            portfolio.setQuantity(remaining);
            portfolioRepository.save(portfolio);
        }
    }

    private TransactionDto convertToDto(Transaction transaction) {
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
