package com.tienvm.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		Jwt jwt = (Jwt) authentication.getPrincipal();
		return new UserResponse(jwt.getSubject(), jwt.getClaimAsString("gmail"), jwt.getClaimAsString("username"));
	}

	public record UserResponse(String id, String gmail, String username) {
	}

}