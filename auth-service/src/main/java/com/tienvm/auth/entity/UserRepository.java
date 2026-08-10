package com.tienvm.auth.entity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGmail(String gmail);

	boolean existsByGmail(String gmail);

	boolean existsByUsername(String username);

}