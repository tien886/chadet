package com.tienvm.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	@Query("""
		SELECT c FROM Conversation c
		JOIN c.members m
		WHERE m.id.userId = :userId
		ORDER BY c.createdAt DESC
	""")
	List<Conversation> findAllByUserId(@Param("userId") UUID userId);

	@Query("""
		SELECT c FROM Conversation c
		WHERE c.groupChat IS NULL
		AND EXISTS (SELECT 1 FROM ConversationMember m1 WHERE m1.conversation = c AND m1.id.userId = :user1)
		AND EXISTS (SELECT 1 FROM ConversationMember m2 WHERE m2.conversation = c AND m2.id.userId = :user2)
		AND (SELECT COUNT(m) FROM ConversationMember m WHERE m.conversation = c) = 2
	""")
	Optional<Conversation> findDirectConversationBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

}
