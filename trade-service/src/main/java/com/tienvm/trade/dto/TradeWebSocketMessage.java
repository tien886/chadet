package com.tienvm.trade.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tienvm.trade.entity.Trade;
import com.tienvm.trade.entity.TradeStatus;

public record TradeWebSocketMessage(
		String type,
		UUID tradeId,
		UUID conversationId,
		UUID senderId,
		UUID receiverId,
		BigDecimal amount,
		TradeStatus status,
		boolean senderConfirmed,
		boolean receiverConfirmed,
		Instant completedAt,
		Instant timestamp,
		String message
) {

	public static TradeWebSocketMessage of(Trade trade, String type, String message) {
		return new TradeWebSocketMessage(
				type,
				trade.getId(),
				trade.getConversationId(),
				trade.getSenderId(),
				trade.getReceiverId(),
				trade.getAmount(),
				trade.getStatus(),
				trade.isSenderConfirmed(),
				trade.isReceiverConfirmed(),
				trade.getCompletedAt(),
				Instant.now(),
				message
		);
	}

}
