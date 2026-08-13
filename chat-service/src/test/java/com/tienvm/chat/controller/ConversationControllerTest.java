package com.tienvm.chat.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienvm.chat.dto.AddMembersRequest;
import com.tienvm.chat.dto.ConversationResponse;
import com.tienvm.chat.dto.CreateDirectConversationRequest;
import com.tienvm.chat.dto.CreateGroupConversationRequest;
import com.tienvm.chat.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private ConversationService conversationService;

	@InjectMocks
	private ConversationController conversationController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(conversationController).build();
		objectMapper = new ObjectMapper();
	}

	private JwtAuthenticationToken mockJwt(UUID userId) {
		Jwt jwt = Jwt.withTokenValue("mock-token")
				.header("alg", "none")
				.subject(userId.toString())
				.claim("username", "testuser")
				.build();
		return new JwtAuthenticationToken(jwt);
	}

	@Test
	void createDirectConversation_unauthenticated_returnsBadRequest() throws Exception {
		CreateDirectConversationRequest request = new CreateDirectConversationRequest(UUID.randomUUID());
		mockMvc.perform(post("/api/conversations/direct")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createDirectConversation_authenticated_returnsOk() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID recipientId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		CreateDirectConversationRequest request = new CreateDirectConversationRequest(recipientId);

		ConversationResponse response = new ConversationResponse(
				convId, false, null, null, List.of(currentUserId, recipientId), null, Instant.now()
		);

		when(conversationService.getOrCreateDirectConversation(currentUserId, recipientId))
				.thenReturn(response);

		mockMvc.perform(post("/api/conversations/direct")
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(convId.toString()))
				.andExpect(jsonPath("$.isGroup").value(false));
	}

	@Test
	void createGroupConversation_authenticated_returnsOk() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID member1 = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		CreateGroupConversationRequest request = new CreateGroupConversationRequest("Study Group", List.of(member1));

		ConversationResponse response = new ConversationResponse(
				convId, true, "Study Group", currentUserId, List.of(currentUserId, member1), null, Instant.now()
		);

		when(conversationService.createGroupChat(eq(currentUserId), eq("Study Group"), any()))
				.thenReturn(response);

		mockMvc.perform(post("/api/conversations/group")
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(convId.toString()))
				.andExpect(jsonPath("$.name").value("Study Group"))
				.andExpect(jsonPath("$.isGroup").value(true));
	}

	@Test
	void getUserConversations_authenticated_returnsList() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		ConversationResponse response = new ConversationResponse(
				convId, false, null, null, List.of(currentUserId), null, Instant.now()
		);

		when(conversationService.getUserConversations(currentUserId))
				.thenReturn(List.of(response));

		mockMvc.perform(get("/api/conversations")
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(convId.toString()));
	}

	@Test
	void addMembers_authenticated_returnsUpdatedConversation() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		UUID newMember = UUID.randomUUID();
		AddMembersRequest request = new AddMembersRequest(List.of(newMember));

		ConversationResponse response = new ConversationResponse(
				convId, true, "Group", currentUserId, List.of(currentUserId, newMember), null, Instant.now()
		);

		when(conversationService.addMembers(convId, currentUserId, List.of(newMember)))
				.thenReturn(response);

		mockMvc.perform(post("/api/conversations/{id}/members", convId)
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberIds[1]").value(newMember.toString()));
	}

	@Test
	void leaveConversation_authenticated_returnsNoContent() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();

		mockMvc.perform(post("/api/conversations/{id}/leave", convId)
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isNoContent());

		verify(conversationService).leaveConversation(convId, currentUserId);
	}

}
