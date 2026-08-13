package com.tienvm.chat.dto;

import java.time.Instant;
import java.util.UUID;

import com.tienvm.chat.entity.ConversationMember;

public record MemberResponse(
		UUID userId,
		Instant joinedAt
) {
	public static MemberResponse fromEntity(ConversationMember member) {
		return new MemberResponse(
				member.getUserId(),
				member.getJoinedAt()
		);
	}
}
