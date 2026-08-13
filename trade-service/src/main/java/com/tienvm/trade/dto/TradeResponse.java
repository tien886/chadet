package com.tienvm.trade.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tienvm.trade.entity.Trade;
import com.tienvm.trade.entity.TradeStatus;

public record TradeResponse(
		UUID id,
		UUID conversationId,
		UUID creatorId,
		UUID senderId,
		UUID receiverId,
		BigDecimal amount,
		TradeStatus status,
		boolean senderConfirmed,
		boolean receiverConfirmed,
		Instant createdAt,
		Instant completedAt
) {
	public static TradeResponse from(Trade trade) {
		return new TradeResponse(
				trade.getId(),
				trade.getConversationId(),
				trade.getCreatorId(),
				trade.getSenderId(),
				trade.getReceiverId(),
				trade.getAmount(),
				trade.getStatus(),
				trade.isSenderConfirmed(),
				trade.isReceiverConfirmed(),
				trade.getCreatedAt(),
				trade.getCompletedAt()
		);
	}
}
