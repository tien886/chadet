package com.tienvm.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tienvm.chat.entity.Conversation;

public record ConversationResponse(
		UUID id,
		boolean isGroup,
		String name,
		UUID creatorId,
		List<UUID> memberIds,
		MessageResponse lastMessage,
		Instant createdAt
) {
	public static ConversationResponse fromEntity(Conversation conversation, MessageResponse lastMessage) {
		boolean isGroup = conversation.isGroup();
		String name = isGroup && conversation.getGroupChat() != null ? conversation.getGroupChat().getName() : null;
		UUID creatorId = isGroup && conversation.getGroupChat() != null ? conversation.getGroupChat().getCreatorId() : null;
		List<UUID> memberIds = conversation.getMembers() != null
				? conversation.getMembers().stream().map(m -> m.getUserId()).toList()
				: List.of();

		return new ConversationResponse(
				conversation.getId(),
				isGroup,
				name,
				creatorId,
				memberIds,
				lastMessage,
				conversation.getCreatedAt()
		);
	}
}
