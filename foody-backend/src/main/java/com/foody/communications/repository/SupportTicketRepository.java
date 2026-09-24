package com.foody.communications.repository;
import com.foody.communications.entity.*; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long>{
 Slice<SupportTicket> findByBusinessIdOrderByUpdatedAtDesc(Long businessId,Pageable pageable);
 Slice<SupportTicket> findByStatusOrderByUpdatedAtDesc(TicketStatus status,Pageable pageable);
 Slice<SupportTicket> findAllByOrderByUpdatedAtDesc(Pageable pageable);
 Optional<SupportTicket> findByIdAndBusinessId(Long id,Long businessId);
 Slice<SupportTicket> findByRequesterTypeOrderByUpdatedAtDesc(TicketRequesterType requesterType,Pageable pageable);
 Slice<SupportTicket> findByStatusAndRequesterTypeOrderByUpdatedAtDesc(TicketStatus status,TicketRequesterType requesterType,Pageable pageable);
 Slice<SupportTicket> findByRequesterTypeAndRequesterUserIdOrderByUpdatedAtDesc(TicketRequesterType requesterType,Long requesterUserId,Pageable pageable);
 Optional<SupportTicket> findByIdAndRequesterTypeAndRequesterUserId(Long id,TicketRequesterType requesterType,Long requesterUserId);
}
