package com.aurelia.grand.repository;

import com.aurelia.grand.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserEmailOrderByCreatedAtAsc(String email);

    @Query("SELECT c.userEmail, c.userName, MAX(c.createdAt), " +
           "SUM(CASE WHEN c.messageType = 'customer' AND c.isRead = 0 THEN 1 ELSE 0 END) " +
           "FROM ChatMessage c GROUP BY c.userEmail, c.userName ORDER BY MAX(c.createdAt) DESC")
    List<Object[]> findInboxList();

    @Transactional
    @Modifying
    @Query("UPDATE ChatMessage c SET c.isRead = 1 WHERE c.userEmail = :email AND c.messageType = 'customer'")
    void markAsReadForEmail(@Param("email") String email);
}
