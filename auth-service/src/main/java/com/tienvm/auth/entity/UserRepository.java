package com.tienvm.auth.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByGmail(String gmail);

	boolean existsByGmail(String gmail);

	boolean existsByUsername(String username);

	@Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.gmail) LIKE LOWER(CONCAT('%', :query, '%'))")
	List<User> searchUsers(@Param("query") String query);

}