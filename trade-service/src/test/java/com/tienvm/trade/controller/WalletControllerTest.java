package com.tienvm.trade.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienvm.trade.dto.DepositRequest;
import com.tienvm.trade.dto.WalletResponse;
import com.tienvm.trade.service.WalletService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper;

	@Mock
	private WalletService walletService;

	@InjectMocks
	private WalletController walletController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(walletController).build();
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
	void getWalletDetails_authenticated_returnsOk() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		WalletResponse response = new WalletResponse(
				currentUserId, new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), Instant.now()
		);

		when(walletService.getWalletDetails(currentUserId)).thenReturn(response);

		mockMvc.perform(get("/api/wallet")
						.principal(mockJwt(currentUserId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(currentUserId.toString()))
				.andExpect(jsonPath("$.balance").value(1000.00));
	}

	@Test
	void deposit_authenticated_returnsUpdatedWallet() throws Exception {
		UUID currentUserId = UUID.randomUUID();
		DepositRequest request = new DepositRequest(new BigDecimal("250.00"));
		WalletResponse response = new WalletResponse(
				currentUserId, new BigDecimal("1250.00"), BigDecimal.ZERO, new BigDecimal("1250.00"), Instant.now()
		);

		when(walletService.deposit(currentUserId, new BigDecimal("250.00"))).thenReturn(response);

		mockMvc.perform(post("/api/wallet/deposit")
						.principal(mockJwt(currentUserId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(1250.00));
	}

}
