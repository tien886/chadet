package com.tienvm.chat.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_members")
public class ConversationMember {

	@EmbeddedId
	private ConversationMemberId id;

	@MapsId("conversationId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id", nullable = false)
	private Conversation conversation;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt = Instant.now();

	public ConversationMember() {
	}

	public ConversationMember(Conversation conversation, UUID userId) {
		this.conversation = conversation;
		this.id = new ConversationMemberId(conversation != null ? conversation.getId() : null, userId);
		this.joinedAt = Instant.now();
	}

	public ConversationMemberId getId() {
		return id;
	}

	public void setId(ConversationMemberId id) {
		this.id = id;
	}

	public Conversation getConversation() {
		return conversation;
	}

	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(Instant joinedAt) {
		this.joinedAt = joinedAt;
	}

}
