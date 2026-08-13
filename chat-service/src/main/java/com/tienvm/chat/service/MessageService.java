package com.tienvm.chat.service;

import java.util.List;
import java.util.UUID;

import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.entity.Conversation;
import com.tienvm.chat.entity.Message;
import com.tienvm.chat.repository.ConversationMemberRepository;
import com.tienvm.chat.repository.ConversationRepository;
import com.tienvm.chat.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MessageService {

	private final MessageRepository messageRepository;
	private final ConversationRepository conversationRepository;
	private final ConversationMemberRepository conversationMemberRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public MessageService(
			MessageRepository messageRepository,
			ConversationRepository conversationRepository,
			ConversationMemberRepository conversationMemberRepository,
			SimpMessagingTemplate messagingTemplate) {
		this.messageRepository = messageRepository;
		this.conversationRepository = conversationRepository;
		this.conversationMemberRepository = conversationMemberRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@Transactional
	public MessageResponse sendMessage(UUID conversationId, UUID senderId, String content) {
		log.info("Sending message in conversation {} from sender {}", conversationId, senderId);
		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

		if (!conversationMemberRepository.existsByIdConversationIdAndIdUserId(conversationId, senderId)) {
			log.warn("Message rejected: user {} is not a member of conversation {}", senderId, conversationId);
			throw new IllegalArgumentException("You are not a member of this conversation");
		}

		Message message = new Message(conversation, senderId, content);
		Message saved = messageRepository.save(message);
		MessageResponse response = MessageResponse.fromEntity(saved);

		log.info("Message saved with id: {}. Broadcasting to /topic/conversations/{}", saved.getId(), conversationId);
		try {
			messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
		} catch (Exception e) {
			log.error("Failed to broadcast message to WebSocket topic: {}", e.getMessage(), e);
		}

		return response;
	}

	@Transactional(readOnly = true)
	public Page<MessageResponse> getMessageHistory(UUID conversationId, UUID userId, int page, int size) {
		log.info("Fetching message history for conversation {} by user {} (page: {}, size: {})", conversationId, userId,
				page, size);
		if (!conversationRepository.existsById(conversationId)) {
			throw new IllegalArgumentException("Conversation not found");
		}

		if (!conversationMemberRepository.existsByIdConversationIdAndIdUserId(conversationId, userId)) {
			log.warn("Message history access denied: user {} is not a member of conversation {}", userId,
					conversationId);
			throw new IllegalArgumentException("You are not a member of this conversation");
		}

		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Message> messagePage = messageRepository.findByConversation_IdOrderByCreatedAtDesc(conversationId,
				pageRequest);
		log.info("Returned {} messages for conversation {}", messagePage.getNumberOfElements(), conversationId);
		return messagePage.map(MessageResponse::fromEntity);
	}

}
