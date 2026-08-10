package com.tienvm.auth.config;

import java.io.ByteArrayInputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

	private final RSAPrivateKey privateKey;
	private final RSAPublicKey publicKey;

	public JwtConfig(@Value("${chadet.jwt.private-key}") String privateKeyB64,
			@Value("${chadet.jwt.public-key}") String publicKeyB64) {
		this.privateKey = RsaKeyConverters.pkcs8().convert(pem(privateKeyB64));
		this.publicKey = RsaKeyConverters.x509().convert(pem(publicKeyB64));
	}

	private static ByteArrayInputStream pem(String base64) {
		return new ByteArrayInputStream(Base64.getDecoder().decode(base64.replaceAll("\\s", "")));
	}

	@Bean
	JwtEncoder jwtEncoder() {
		RSAKey jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("chadet").build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
	}

	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(publicKey).build();
	}

}