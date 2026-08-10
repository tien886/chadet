package com.tienvm.gateway.config;

import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	ReactiveJwtDecoder jwtDecoder(@Value("${chadet.jwt.public-key}") String publicKeyB64) {
		RSAPublicKey key = RsaKeyConverters.x509().convert(
				new ByteArrayInputStream(Base64.getDecoder().decode(publicKeyB64.replaceAll("\\s", ""))));
		return NimbusReactiveJwtDecoder.withPublicKey(key).build();
	}

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder decoder) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/api/auth/**", "/ws/**").permitAll()
						.anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(decoder)))
				.build();
	}

}