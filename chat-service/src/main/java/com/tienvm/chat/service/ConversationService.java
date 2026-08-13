package com.tienvm.chat.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.tienvm.chat.dto.ConversationResponse;
import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.entity.Conversation;
import com.tienvm.chat.entity.GroupChat;
import com.tienvm.chat.repository.ConversationMemberRepository;
import com.tienvm.chat.repository.ConversationRepository;
import com.tienvm.chat.repository.GroupChatRepository;
import com.tienvm.chat.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final GroupChatRepository groupChatRepository;
	private final ConversationMemberRepository conversationMemberRepository;
	private final MessageRepository messageRepository;

	public ConversationService(
			ConversationRepository conversationRepository,
			GroupChatRepository groupChatRepository,
			ConversationMemberRepository conversationMemberRepository,
			MessageRepository messageRepository) {
		this.conversationRepository = conversationRepository;
		this.groupChatRepository = groupChatRepository;
		this.conversationMemberRepository = conversationMemberRepository;
		this.messageRepository = messageRepository;
	}

	@Transactional
	public ConversationResponse getOrCreateDirectConversation(UUID currentUserId, UUID recipientId) {
		log.info("Request direct conversation between user {} and user {}", currentUserId, recipientId);
		if (currentUserId.equals(recipientId)) {
			log.warn("Direct conversation rejected: user cannot chat with themselves ({})", currentUserId);
			throw new IllegalArgumentException("Cannot start conversation with yourself");
		}

		return conversationRepository.findDirectConversationBetween(currentUserId, recipientId)
				.map(conv -> {
					log.debug("Found existing direct conversation: {}", conv.getId());
					MessageResponse lastMsg = messageRepository.findTop1ByConversation_IdOrderByCreatedAtDesc(conv.getId())
							.map(MessageResponse::fromEntity)
							.orElse(null);
					return ConversationResponse.fromEntity(conv, lastMsg);
				})
				.orElseGet(() -> {
					log.info("Creating new direct conversation between {} and {}", currentUserId, recipientId);
					Conversation conversation = new Conversation();
					conversation.addMember(currentUserId);
					conversation.addMember(recipientId);
					Conversation saved = conversationRepository.save(conversation);
					log.info("Created direct conversation {}", saved.getId());
					return ConversationResponse.fromEntity(saved, null);
				});
	}

	@Transactional
	public ConversationResponse createGroupChat(UUID creatorId, String name, List<UUID> memberIds) {
		log.info("User {} creating group chat '{}' with {} members", creatorId, name, memberIds.size());
		Conversation conversation = new Conversation();
		GroupChat groupChat = new GroupChat(conversation, name, creatorId);
		conversation.setGroupChat(groupChat);

		Set<UUID> allMemberIds = new HashSet<>(memberIds);
		allMemberIds.add(creatorId);

		for (UUID memberId : allMemberIds) {
			conversation.addMember(memberId);
		}

		Conversation saved = conversationRepository.save(conversation);
		log.info("Created group chat {} with id: {}", name, saved.getId());
		return ConversationResponse.fromEntity(saved, null);
	}

	@Transactional(readOnly = true)
	public List<ConversationResponse> getUserConversations(UUID userId) {
		log.debug("Fetching conversations for user {}", userId);
		List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
		List<ConversationResponse> responses = new ArrayList<>();

		for (Conversation conv : conversations) {
			MessageResponse lastMsg = messageRepository.findTop1ByConversation_IdOrderByCreatedAtDesc(conv.getId())
					.map(MessageResponse::fromEntity)
					.orElse(null);
			responses.add(ConversationResponse.fromEntity(conv, lastMsg));
		}

		log.info("Returned {} conversations for user {}", responses.size(), userId);
		return responses;
	}

	@Transactional(readOnly = true)
	public ConversationResponse getConversationDetails(UUID conversationId, UUID userId) {
		log.debug("Fetching conversation details {} for user {}", conversationId, userId);
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

		if (!isMember(conversationId, userId)) {
			log.warn("Access denied: user {} is not a member of conversation {}", userId, conversationId);
			throw new IllegalArgumentException("You are not a member of this conversation");
		}

		MessageResponse lastMsg = messageRepository.findTop1ByConversation_IdOrderByCreatedAtDesc(conversationId)
				.map(MessageResponse::fromEntity)
				.orElse(null);
		return ConversationResponse.fromEntity(conversation, lastMsg);
	}

	@Transactional
	public ConversationResponse addMembers(UUID conversationId, UUID requesterId, List<UUID> newMemberIds) {
		log.info("User {} adding members {} to conversation {}", requesterId, newMemberIds, conversationId);
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

		if (!conversation.isGroup()) {
			log.warn("Add members rejected: conversation {} is not a group chat", conversationId);
			throw new IllegalArgumentException("Cannot add members to a direct conversation");
		}

		if (!isMember(conversationId, requesterId)) {
			log.warn("Add members rejected: user {} is not a member of conversation {}", requesterId, conversationId);
			throw new IllegalArgumentException("You are not a member of this group chat");
		}

		for (UUID memberId : newMemberIds) {
			if (!isMember(conversationId, memberId)) {
				conversation.addMember(memberId);
			}
		}

		Conversation updated = conversationRepository.save(conversation);
		log.info("Updated conversation members for group {}", conversationId);
		MessageResponse lastMsg = messageRepository.findTop1ByConversation_IdOrderByCreatedAtDesc(conversationId)
				.map(MessageResponse::fromEntity)
				.orElse(null);
		return ConversationResponse.fromEntity(updated, lastMsg);
	}

	@Transactional
	public void leaveConversation(UUID conversationId, UUID userId) {
		log.info("User {} leaving conversation {}", userId, conversationId);
		if (!isMember(conversationId, userId)) {
			log.warn("Leave rejected: user {} is not a member of conversation {}", userId, conversationId);
			throw new IllegalArgumentException("You are not a member of this conversation");
		}

		conversationMemberRepository.deleteByIdConversationIdAndIdUserId(conversationId, userId);
		log.info("User {} left conversation {}", userId, conversationId);
	}

	public boolean isMember(UUID conversationId, UUID userId) {
		return conversationMemberRepository.existsByIdConversationIdAndIdUserId(conversationId, userId);
	}

}
