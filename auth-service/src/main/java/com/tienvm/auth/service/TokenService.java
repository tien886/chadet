package com.tienvm.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.tienvm.auth.entity.User;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	private static final long EXPIRY_HOURS = 24;

	private final JwtEncoder jwtEncoder;

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("chadet")
				.issuedAt(now)
				.expiresAt(now.plus(EXPIRY_HOURS, ChronoUnit.HOURS))
				.subject(user.getId().toString())
				.claim("username", user.getUsername())
				.claim("gmail", user.getGmail())
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

}