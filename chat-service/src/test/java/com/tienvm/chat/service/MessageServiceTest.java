package com.tienvm.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.entity.Conversation;
import com.tienvm.chat.entity.Message;
import com.tienvm.chat.repository.ConversationMemberRepository;
import com.tienvm.chat.repository.ConversationRepository;
import com.tienvm.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private ConversationMemberRepository conversationMemberRepository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private MessageService messageService;

	@BeforeEach
	void setUp() {
		messageService = new MessageService(
				messageRepository,
				conversationRepository,
				conversationMemberRepository,
				messagingTemplate
		);
	}

	@Test
	void sendMessage_throwsWhenConversationNotFound() {
		UUID convId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();

		when(conversationRepository.findById(convId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> messageService.sendMessage(convId, senderId, "Hello"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Conversation not found");
	}

	@Test
	void sendMessage_throwsWhenSenderNotMember() {
		UUID convId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		Conversation conv = new Conversation();
		conv.setId(convId);

		when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, senderId)).thenReturn(false);

		assertThatThrownBy(() -> messageService.sendMessage(convId, senderId, "Hello"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a member");
	}

	@Test
	void sendMessage_savesAndBroadcastsMessage() {
		UUID convId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID msgId = UUID.randomUUID();
		Conversation conv = new Conversation();
		conv.setId(convId);

		when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, senderId)).thenReturn(true);
		when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
			Message m = invocation.getArgument(0);
			m.setId(msgId);
			return m;
		});

		MessageResponse response = messageService.sendMessage(convId, senderId, "Hello World!");

		assertThat(response.id()).isEqualTo(msgId);
		assertThat(response.conversationId()).isEqualTo(convId);
		assertThat(response.senderId()).isEqualTo(senderId);
		assertThat(response.content()).isEqualTo("Hello World!");

		verify(messageRepository).save(any(Message.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/conversations/" + convId), any(MessageResponse.class));
	}

	@Test
	void getMessageHistory_throwsWhenNotMember() {
		UUID convId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		when(conversationRepository.existsById(convId)).thenReturn(true);
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, userId)).thenReturn(false);

		assertThatThrownBy(() -> messageService.getMessageHistory(convId, userId, 0, 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a member");
	}

	@Test
	void getMessageHistory_returnsPaginatedMessages() {
		UUID convId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Conversation conv = new Conversation();
		conv.setId(convId);

		Message m1 = new Message(conv, userId, "Msg 1");
		m1.setId(UUID.randomUUID());
		Message m2 = new Message(conv, userId, "Msg 2");
		m2.setId(UUID.randomUUID());

		when(conversationRepository.existsById(convId)).thenReturn(true);
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, userId)).thenReturn(true);
		when(messageRepository.findByConversation_IdOrderByCreatedAtDesc(eq(convId), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(m1, m2)));

		Page<MessageResponse> page = messageService.getMessageHistory(convId, userId, 0, 10);

		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent().get(0).content()).isEqualTo("Msg 1");
		assertThat(page.getContent().get(1).content()).isEqualTo("Msg 2");
	}

}
