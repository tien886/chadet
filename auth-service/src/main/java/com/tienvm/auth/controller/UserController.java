package com.tienvm.auth.controller;

import java.util.List;

import com.tienvm.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/api/users", "/api/user"})
@Tag(name = "User API", description = "User profile and search operations")
public class UserController {

	private final AuthService authService;

	public UserController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/me")
	@Operation(summary = "Get current authenticated user profile")
	public UserResponse me(Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		log.info("GET /api/user/me -> userId: {}, username: {}", jwt.getSubject(), jwt.getClaimAsString("username"));
		return new UserResponse(jwt.getSubject(), jwt.getClaimAsString("gmail"), jwt.getClaimAsString("username"));
	}

	@GetMapping("/search")
	@Operation(summary = "Search users by username or email")
	public List<UserResponse> search(@RequestParam(value = "query", defaultValue = "") String query, Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		String currentUserId = jwt.getSubject();
		log.info("GET /api/user/search?query='{}' -> requester: {}", query, currentUserId);

		return authService.searchUsers(query).stream()
				.filter(u -> !u.getId().toString().equals(currentUserId))
				.map(u -> new UserResponse(u.getId().toString(), u.getGmail(), u.getUsername()))
				.toList();
	}

	public record UserResponse(String id, String gmail, String username) {
	}

}