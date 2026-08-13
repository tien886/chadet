package com.tienvm.trade.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Value("${chadet.jwt.public-key}")
	private String publicKeyBase64;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/trade-ws/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/docs/**", "/scalar/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
				.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		try {
			byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
			String keyStr = new String(keyBytes)
					.replace("-----BEGIN PUBLIC KEY-----", "")
					.replace("-----END PUBLIC KEY-----", "")
					.replaceAll("\\s+", "");
			byte[] decoded = Base64.getDecoder().decode(keyStr);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(spec);
			return NimbusJwtDecoder.withPublicKey(publicKey).build();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to configure RSA Public Key decoder for trade-service", e);
		}
	}

}
