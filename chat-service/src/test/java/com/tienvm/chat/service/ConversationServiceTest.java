package com.tienvm.chat.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.chat.dto.ConversationResponse;
import com.tienvm.chat.entity.Conversation;
import com.tienvm.chat.entity.GroupChat;
import com.tienvm.chat.repository.ConversationMemberRepository;
import com.tienvm.chat.repository.ConversationRepository;
import com.tienvm.chat.repository.GroupChatRepository;
import com.tienvm.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private GroupChatRepository groupChatRepository;

	@Mock
	private ConversationMemberRepository conversationMemberRepository;

	@Mock
	private MessageRepository messageRepository;

	private ConversationService conversationService;

	@BeforeEach
	void setUp() {
		conversationService = new ConversationService(
				conversationRepository,
				groupChatRepository,
				conversationMemberRepository,
				messageRepository
		);
	}

	@Test
	void getOrCreateDirectConversation_throwsWhenSameUser() {
		UUID userId = UUID.randomUUID();
		assertThatThrownBy(() -> conversationService.getOrCreateDirectConversation(userId, userId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("yourself");
	}

	@Test
	void getOrCreateDirectConversation_returnsExistingConversation() {
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		Conversation existing = new Conversation();
		existing.setId(UUID.randomUUID());
		existing.addMember(user1);
		existing.addMember(user2);

		when(conversationRepository.findDirectConversationBetween(user1, user2))
				.thenReturn(Optional.of(existing));
		when(messageRepository.findTop1ByConversation_IdOrderByCreatedAtDesc(existing.getId()))
				.thenReturn(Optional.empty());

		ConversationResponse response = conversationService.getOrCreateDirectConversation(user1, user2);

		assertThat(response.id()).isEqualTo(existing.getId());
		assertThat(response.isGroup()).isFalse();
		assertThat(response.memberIds()).containsExactlyInAnyOrder(user1, user2);
	}

	@Test
	void getOrCreateDirectConversation_createsNewConversationWhenNotFound() {
		UUID user1 = UUID.randomUUID();
		UUID user2 = UUID.randomUUID();
		UUID newConvId = UUID.randomUUID();

		when(conversationRepository.findDirectConversationBetween(user1, user2))
				.thenReturn(Optional.empty());
		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation conv = invocation.getArgument(0);
			conv.setId(newConvId);
			return conv;
		});

		ConversationResponse response = conversationService.getOrCreateDirectConversation(user1, user2);

		assertThat(response.id()).isEqualTo(newConvId);
		assertThat(response.isGroup()).isFalse();
		verify(conversationRepository).save(any(Conversation.class));
	}

	@Test
	void createGroupChat_createsAndAddsAllMembers() {
		UUID creator = UUID.randomUUID();
		UUID member1 = UUID.randomUUID();
		UUID member2 = UUID.randomUUID();
		UUID newConvId = UUID.randomUUID();

		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation conv = invocation.getArgument(0);
			conv.setId(newConvId);
			return conv;
		});

		ConversationResponse response = conversationService.createGroupChat(creator, "Project Chat", List.of(member1, member2));

		assertThat(response.id()).isEqualTo(newConvId);
		assertThat(response.isGroup()).isTrue();
		assertThat(response.name()).isEqualTo("Project Chat");
		assertThat(response.creatorId()).isEqualTo(creator);
		assertThat(response.memberIds()).containsExactlyInAnyOrder(creator, member1, member2);
	}

	@Test
	void getConversationDetails_throwsWhenNotMember() {
		UUID convId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Conversation conv = new Conversation();
		conv.setId(convId);

		when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, userId)).thenReturn(false);

		assertThatThrownBy(() -> conversationService.getConversationDetails(convId, userId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a member");
	}

	@Test
	void addMembers_throwsWhenNotGroup() {
		UUID convId = UUID.randomUUID();
		UUID requester = UUID.randomUUID();
		Conversation conv = new Conversation();
		conv.setId(convId);

		when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

		assertThatThrownBy(() -> conversationService.addMembers(convId, requester, List.of(UUID.randomUUID())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("direct conversation");
	}

	@Test
	void addMembers_addsMembersSuccessfully() {
		UUID convId = UUID.randomUUID();
		UUID requester = UUID.randomUUID();
		UUID newMember = UUID.randomUUID();

		Conversation conv = new Conversation();
		conv.setId(convId);
		GroupChat group = new GroupChat(conv, "Team", requester);
		conv.setGroupChat(group);
		conv.addMember(requester);

		when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, requester)).thenReturn(true);
		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, newMember)).thenReturn(false);
		when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

		ConversationResponse response = conversationService.addMembers(convId, requester, List.of(newMember));

		assertThat(response.isGroup()).isTrue();
		verify(conversationRepository).save(conv);
	}

	@Test
	void leaveConversation_removesMember() {
		UUID convId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();

		when(conversationMemberRepository.existsByIdConversationIdAndIdUserId(convId, userId)).thenReturn(true);

		conversationService.leaveConversation(convId, userId);

		verify(conversationMemberRepository).deleteByIdConversationIdAndIdUserId(convId, userId);
	}

}
