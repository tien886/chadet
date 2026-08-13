package com.tienvm.chat.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.dto.SendMessageRequest;
import com.tienvm.chat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private MessageService messageService;

	@InjectMocks
	private MessageController messageController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(messageController).build();
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
	void sendMessage_unauthenticated_returnsBadRequest() throws Exception {
		UUID convId = UUID.randomUUID();
		SendMessageRequest request = new SendMessageRequest("Hello");

		mockMvc.perform(post("/api/conversations/{id}/messages", convId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sendMessage_authenticated_returnsOk() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		UUID msgId = UUID.randomUUID();
		SendMessageRequest request = new SendMessageRequest("Hello there!");

		MessageResponse response = new MessageResponse(msgId, convId, currentUserId, "Hello there!", Instant.now());

		when(messageService.sendMessage(convId, currentUserId, "Hello there!"))
				.thenReturn(response);

		mockMvc.perform(post("/api/conversations/{id}/messages", convId)
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(msgId.toString()))
				.andExpect(jsonPath("$.content").value("Hello there!"));
	}

	@Test
	void getMessageHistory_authenticated_returnsPage() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		UUID msgId = UUID.randomUUID();

		MessageResponse response = new MessageResponse(msgId, convId, currentUserId, "Hello there!", Instant.now());
		when(messageService.getMessageHistory(convId, currentUserId, 0, 50))
				.thenReturn(new PageImpl<>(List.of(response)));

		mockMvc.perform(get("/api/conversations/{id}/messages", convId)
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(msgId.toString()));
	}

}
