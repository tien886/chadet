package com.tienvm.gateway.config;

import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

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
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of("*"));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder decoder) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.cors(Customizer.withDefaults())
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers(HttpMethod.OPTIONS).permitAll()
						.pathMatchers("/api/auth/**", "/ws/**",
								"/auth/docs", "/auth/docs/**",
								"/auth/scalar", "/auth/scalar/**",
								"/auth/v3/api-docs", "/auth/v3/api-docs/**",
								"/auth/scalar.html", "/auth/api-docs", "/auth/api-docs/**")
						.permitAll()
						.anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(decoder)))
				.build();
	}

}