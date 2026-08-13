package com.tienvm.chat.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "conversations")
public class Conversation {

	@Id
	@UuidGenerator
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
	private GroupChat groupChat;

	@OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<ConversationMember> members = new HashSet<>();

	public Conversation() {
	}

	public Conversation(Instant createdAt) {
		this.createdAt = createdAt != null ? createdAt : Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public GroupChat getGroupChat() {
		return groupChat;
	}

	public void setGroupChat(GroupChat groupChat) {
		this.groupChat = groupChat;
		if (groupChat != null) {
			groupChat.setConversation(this);
		}
	}

	public Set<ConversationMember> getMembers() {
		return members;
	}

	public void setMembers(Set<ConversationMember> members) {
		this.members = members;
	}

	public void addMember(UUID userId) {
		ConversationMember member = new ConversationMember(this, userId);
		this.members.add(member);
	}

	public boolean isGroup() {
		return groupChat != null;
	}

}
