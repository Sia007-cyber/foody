package com.foody.communications.repository;
import com.foody.communications.entity.SupportTicketMessage; import java.util.List; import org.springframework.data.jpa.repository.JpaRepository;
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage,Long>{List<SupportTicketMessage> findByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);}
