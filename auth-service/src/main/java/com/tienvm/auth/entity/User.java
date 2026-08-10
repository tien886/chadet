package com.tienvm.auth.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class User {

	@Id
	@UuidGenerator
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, unique = true, length = 320)
	private String gmail;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false)
	private String password;

	protected User() {
	}

	public User(String gmail, String username, String password) {
		this.gmail = gmail;
		this.username = username;
		this.password = password;
	}

	public UUID getId() {
		return id;
	}

	public String getGmail() {
		return gmail;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

}