package com.tienvm.auth.controller;

import java.util.List;
import java.util.UUID;

import com.tienvm.auth.entity.User;
import com.tienvm.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	private MockMvc mockMvc;

	@Mock
	private AuthService authService;

	@InjectMocks
	private UserController userController;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
	}

	private JwtAuthenticationToken mockJwt(UUID userId, String username, String gmail) {
		Jwt jwt = Jwt.withTokenValue("mock-token")
				.header("alg", "none")
				.subject(userId.toString())
				.claim("username", username)
				.claim("gmail", gmail)
				.build();
		return new JwtAuthenticationToken(jwt);
	}

	@Test
	void me_returnsCurrentUserInfo() throws Exception {
		UUID userId = UUID.randomUUID();
		mockMvc.perform(get("/api/user/me")
						.principal(mockJwt(userId, "alice", "alice@gmail.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(userId.toString()))
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.gmail").value("alice@gmail.com"));
	}

	@Test
	void search_returnsMatchedUsersExcludingRequester() throws Exception {
		UUID requesterId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();

		User requester = new User("alice@gmail.com", "alice", "pass");
		org.springframework.test.util.ReflectionTestUtils.setField(requester, "id", requesterId);

		User otherUser = new User("bob@gmail.com", "bob", "pass");
		org.springframework.test.util.ReflectionTestUtils.setField(otherUser, "id", otherUserId);

		when(authService.searchUsers("b")).thenReturn(List.of(requester, otherUser));

		mockMvc.perform(get("/api/user/search")
						.param("query", "b")
						.principal(mockJwt(requesterId, "alice", "alice@gmail.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(otherUserId.toString()))
				.andExpect(jsonPath("$[0].username").value("bob"));
	}

}
