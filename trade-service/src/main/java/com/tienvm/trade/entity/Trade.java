package com.tienvm.trade.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "trades")
public class Trade {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(name = "creator_id", nullable = false)
	private UUID creatorId;

	@Column(name = "sender_id", nullable = false)
	private UUID senderId;

	@Column(name = "receiver_id", nullable = false)
	private UUID receiverId;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private TradeStatus status = TradeStatus.CREATED;

	@Column(name = "sender_confirmed", nullable = false)
	private boolean senderConfirmed = false;

	@Column(name = "receiver_confirmed", nullable = false)
	private boolean receiverConfirmed = false;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "completed_at")
	private Instant completedAt;

	public Trade(UUID conversationId, UUID creatorId, UUID senderId, UUID receiverId, BigDecimal amount) {
		this.conversationId = conversationId;
		this.creatorId = creatorId;
		this.senderId = senderId;
		this.receiverId = receiverId;
		this.amount = amount;
		this.status = TradeStatus.CREATED;
		this.senderConfirmed = false;
		this.receiverConfirmed = false;
		this.createdAt = Instant.now();
	}

}
