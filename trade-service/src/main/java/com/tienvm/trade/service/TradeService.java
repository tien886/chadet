package com.tienvm.trade.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tienvm.trade.config.RabbitMQConfig;
import com.tienvm.trade.dto.TradeResponse;
import com.tienvm.trade.dto.TradeWebSocketMessage;
import com.tienvm.trade.entity.Trade;
import com.tienvm.trade.entity.TradeStatus;
import com.tienvm.trade.event.TradeCompletedEvent;
import com.tienvm.trade.event.TradeCreatedEvent;
import com.tienvm.trade.event.TradeStatusChangedEvent;
import com.tienvm.trade.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TradeService {

	private final TradeRepository tradeRepository;
	private final WalletService walletService;
	private final RabbitTemplate rabbitTemplate;
	private final SimpMessagingTemplate messagingTemplate;

	public TradeService(
			TradeRepository tradeRepository,
			WalletService walletService,
			RabbitTemplate rabbitTemplate,
			SimpMessagingTemplate messagingTemplate) {
		this.tradeRepository = tradeRepository;
		this.walletService = walletService;
		this.rabbitTemplate = rabbitTemplate;
		this.messagingTemplate = messagingTemplate;
	}

	@Transactional
	public TradeResponse createTrade(UUID conversationId, UUID creatorId, UUID receiverId, BigDecimal amount) {
		if (creatorId.equals(receiverId)) {
			throw new IllegalArgumentException("Cannot create trade with yourself");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Trade amount must be greater than zero");
		}

		log.info("Initiating trade: conversationId={}, creator={}, receiver={}, amount={}", conversationId, creatorId, receiverId, amount);

		// Hold funds in escrow
		walletService.holdBalance(creatorId, amount);

		Trade trade = new Trade(conversationId, creatorId, creatorId, receiverId, amount);
		trade = tradeRepository.save(trade);
		log.info("Trade created with ID: {}", trade.getId());

		// Publish TradeCreatedEvent to RabbitMQ
		TradeCreatedEvent event = TradeCreatedEvent.of(
				trade.getId(),
				trade.getConversationId(),
				trade.getCreatorId(),
				trade.getSenderId(),
				trade.getReceiverId(),
				trade.getAmount(),
				trade.getStatus().name()
		);
		publishEvent(RabbitMQConfig.ROUTING_KEY_TRADE_CREATED, event);

		// Broadcast real-time WebSocket STOMP message
		broadcastTradeRealtime(trade, "TRADE_CREATED", "Trade created with amount " + trade.getAmount() + " (Pending confirmation)");

		return TradeResponse.from(trade);
	}

	@Transactional
	public TradeResponse confirmTrade(UUID tradeId, UUID userId) {
		Trade trade = tradeRepository.findById(tradeId)
				.orElseThrow(() -> new IllegalArgumentException("Trade not found with id: " + tradeId));

		if (trade.getStatus() == TradeStatus.COMPLETED || trade.getStatus() == TradeStatus.CANCELLED) {
			throw new IllegalArgumentException("Trade is already finalized with status: " + trade.getStatus());
		}

		if (userId.equals(trade.getSenderId())) {
			trade.setSenderConfirmed(true);
			log.info("Trade {} sender confirmed by {}", tradeId, userId);
		} else if (userId.equals(trade.getReceiverId())) {
			trade.setReceiverConfirmed(true);
			log.info("Trade {} receiver confirmed by {}", tradeId, userId);
		} else {
			throw new IllegalArgumentException("User is not a participant in this trade");
		}

		if (trade.isSenderConfirmed() && trade.isReceiverConfirmed()) {
			// Both parties confirmed -> Settle Escrow & Complete Trade
			walletService.settleTransfer(trade.getSenderId(), trade.getReceiverId(), trade.getAmount());
			trade.setStatus(TradeStatus.COMPLETED);
			trade.setCompletedAt(Instant.now());
			trade = tradeRepository.save(trade);

			log.info("Trade {} completed successfully. Funds transferred.", tradeId);

			TradeCompletedEvent event = TradeCompletedEvent.of(
					trade.getId(),
					trade.getConversationId(),
					trade.getSenderId(),
					trade.getReceiverId(),
					trade.getAmount()
			);
			publishEvent(RabbitMQConfig.ROUTING_KEY_TRADE_COMPLETED, event);

			// Broadcast real-time WebSocket STOMP message
			broadcastTradeRealtime(trade, "TRADE_COMPLETED", "Trade completed successfully! Funds transferred.");
		} else {
			trade.setStatus(trade.isSenderConfirmed() ? TradeStatus.CONFIRMED_BY_SENDER : TradeStatus.CONFIRMED_BY_RECEIVER);
			trade = tradeRepository.save(trade);

			TradeStatusChangedEvent event = TradeStatusChangedEvent.of(
					trade.getId(),
					trade.getConversationId(),
					trade.getStatus().name(),
					trade.isSenderConfirmed(),
					trade.isReceiverConfirmed()
			);
			publishEvent(RabbitMQConfig.ROUTING_KEY_TRADE_STATUS_CHANGED, event);

			// Broadcast real-time WebSocket STOMP message
			broadcastTradeRealtime(trade, "TRADE_STATUS_CHANGED", "Trade status updated: " + trade.getStatus());
		}

		return TradeResponse.from(trade);
	}

	@Transactional
	public TradeResponse cancelTrade(UUID tradeId, UUID userId) {
		Trade trade = tradeRepository.findById(tradeId)
				.orElseThrow(() -> new IllegalArgumentException("Trade not found with id: " + tradeId));

		if (trade.getStatus() == TradeStatus.COMPLETED || trade.getStatus() == TradeStatus.CANCELLED) {
			throw new IllegalArgumentException("Trade cannot be cancelled; status is: " + trade.getStatus());
		}

		if (!userId.equals(trade.getSenderId()) && !userId.equals(trade.getReceiverId())) {
			throw new IllegalArgumentException("User is not authorized to cancel this trade");
		}

		// Release escrow back to sender
		walletService.releaseHeldBalance(trade.getSenderId(), trade.getAmount());
		trade.setStatus(TradeStatus.CANCELLED);
		trade = tradeRepository.save(trade);

		log.info("Trade {} cancelled by user {}. Escrow released.", tradeId, userId);

		TradeStatusChangedEvent event = TradeStatusChangedEvent.of(
				trade.getId(),
				trade.getConversationId(),
				trade.getStatus().name(),
				trade.isSenderConfirmed(),
				trade.isReceiverConfirmed()
		);
		publishEvent(RabbitMQConfig.ROUTING_KEY_TRADE_STATUS_CHANGED, event);

		// Broadcast real-time WebSocket STOMP message
		broadcastTradeRealtime(trade, "TRADE_CANCELLED", "Trade cancelled. Escrow released.");

		return TradeResponse.from(trade);
	}

	@Transactional(readOnly = true)
	public TradeResponse getTrade(UUID tradeId, UUID userId) {
		Trade trade = tradeRepository.findById(tradeId)
				.orElseThrow(() -> new IllegalArgumentException("Trade not found with id: " + tradeId));
		if (!userId.equals(trade.getSenderId()) && !userId.equals(trade.getReceiverId())) {
			throw new IllegalArgumentException("Access denied: user is not a participant in this trade");
		}
		return TradeResponse.from(trade);
	}

	@Transactional(readOnly = true)
	public List<TradeResponse> getConversationTrades(UUID conversationId) {
		return tradeRepository.findByConversationIdOrderByCreatedAtDesc(conversationId)
				.stream()
				.map(TradeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TradeResponse> getUserTrades(UUID userId) {
		return tradeRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId)
				.stream()
				.map(TradeResponse::from)
				.toList();
	}

	private void broadcastTradeRealtime(Trade trade, String type, String message) {
		try {
			TradeWebSocketMessage wsMsg = TradeWebSocketMessage.of(trade, type, message);

			// 1. Specific Trade Topic
			String tradeTopic = "/topic/trades/" + trade.getId();
			messagingTemplate.convertAndSend(tradeTopic, wsMsg);
			log.info("Broadcasted trade message to WebSocket topic: {}", tradeTopic);

			// 2. Conversation Trades Topic
			if (trade.getConversationId() != null) {
				String convTopic = "/topic/trades/conversation/" + trade.getConversationId();
				messagingTemplate.convertAndSend(convTopic, wsMsg);
				log.info("Broadcasted trade message to WebSocket conversation topic: {}", convTopic);
			}

			// 3. Private User Destinations (for Sender & Receiver)
			messagingTemplate.convertAndSendToUser(trade.getSenderId().toString(), "/queue/trades", wsMsg);
			messagingTemplate.convertAndSendToUser(trade.getReceiverId().toString(), "/queue/trades", wsMsg);
			log.info("Sent trade message to user queues: sender={}, receiver={}", trade.getSenderId(), trade.getReceiverId());
		} catch (Exception e) {
			log.error("Failed to broadcast real-time WebSocket message for trade {}: {}", trade.getId(), e.getMessage(), e);
		}
	}

	private void publishEvent(String routingKey, Object event) {
		try {
			rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
			log.info("Published event to '{}' with key '{}': {}", RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
		} catch (Exception e) {
			log.error("Failed to publish event with routing key {}: {}", routingKey, e.getMessage());
		}
	}

}
