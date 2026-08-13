package com.tienvm.chat.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_chats")
public class GroupChat {

	@Id
	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@MapsId
	@OneToOne
	@JoinColumn(name = "conversation_id")
	private Conversation conversation;

	@Column(nullable = false)
	private String name;

	@Column(name = "creator_id", nullable = false)
	private UUID creatorId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	public GroupChat() {
	}

	public GroupChat(Conversation conversation, String name, UUID creatorId) {
		this.conversation = conversation;
		this.name = name;
		this.creatorId = creatorId;
		this.createdAt = Instant.now();
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public Conversation getConversation() {
		return conversation;
	}

	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(UUID creatorId) {
		this.creatorId = creatorId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
