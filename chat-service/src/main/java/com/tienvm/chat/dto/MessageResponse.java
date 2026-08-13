package com.tienvm.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.tienvm.chat.entity.Message;

public record MessageResponse(
		UUID id,
		UUID conversationId,
		UUID senderId,
		String content,
		Instant createdAt
) {
	public static MessageResponse fromEntity(Message message) {
		if (message == null) {
			return null;
		}
		return new MessageResponse(
				message.getId(),
				message.getConversationId(),
				message.getSenderId(),
				message.getContent(),
				message.getCreatedAt()
		);
	}
}
