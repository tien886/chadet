package com.tienvm.trade.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienvm.trade.dto.CreateTradeRequest;
import com.tienvm.trade.dto.TradeResponse;
import com.tienvm.trade.entity.TradeStatus;
import com.tienvm.trade.service.TradeService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private TradeService tradeService;

	@InjectMocks
	private TradeController tradeController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(tradeController).build();
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
	void createTrade_authenticated_returnsCreated() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID convId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		CreateTradeRequest request = new CreateTradeRequest(convId, receiverId, new BigDecimal("100.00"));
		TradeResponse response = new TradeResponse(
				tradeId, convId, currentUserId, currentUserId, receiverId,
				new BigDecimal("100.00"), TradeStatus.CREATED, false, false, Instant.now(), null
		);

		when(tradeService.createTrade(convId, currentUserId, receiverId, new BigDecimal("100.00")))
				.thenReturn(response);

		mockMvc.perform(post("/api/trades")
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(tradeId.toString()))
				.andExpect(jsonPath("$.amount").value(100.00))
				.andExpect(jsonPath("$.status").value("CREATED"));
	}

	@Test
	void confirmTrade_authenticated_returnsOk() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		TradeResponse response = new TradeResponse(
				tradeId, UUID.randomUUID(), currentUserId, currentUserId, UUID.randomUUID(),
				new BigDecimal("100.00"), TradeStatus.CONFIRMED_BY_SENDER, true, false, Instant.now(), null
		);

		when(tradeService.confirmTrade(tradeId, currentUserId)).thenReturn(response);

		mockMvc.perform(post("/api/trades/{id}/confirm", tradeId)
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED_BY_SENDER"));
	}

	@Test
	void getUserTrades_authenticated_returnsList() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		TradeResponse response = new TradeResponse(
				tradeId, UUID.randomUUID(), currentUserId, currentUserId, UUID.randomUUID(),
				new BigDecimal("100.00"), TradeStatus.CREATED, false, false, Instant.now(), null
		);

		when(tradeService.getUserTrades(currentUserId)).thenReturn(List.of(response));

		mockMvc.perform(get("/api/trades")
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(tradeId.toString()));
	}

}
