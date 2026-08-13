package com.tienvm.chat.config;

import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	JwtDecoder jwtDecoder(@Value("${chadet.jwt.public-key}") String publicKeyB64) {
		RSAPublicKey key = RsaKeyConverters.x509().convert(
				new ByteArrayInputStream(Base64.getDecoder().decode(publicKeyB64.replaceAll("\\s", ""))));
		return NimbusJwtDecoder.withPublicKey(key).build();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/ws/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/api-docs/**", "/docs/**", "/scalar/**", "/scalar.html")
						.permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.build();
	}

}
