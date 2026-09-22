package com.foody.communications.dto;
import com.foody.communications.entity.*; import java.time.Instant; import java.util.List;
public final class CommunicationResponses {private CommunicationResponses(){}
 public record TicketSummary(Long id,Long businessId,String businessName,String ownerDisplayName,String subject,TicketStatus status,boolean unread,Instant createdAt,Instant updatedAt){}
 public record TicketMessage(Long id,CommunicationSenderType senderType,String senderDisplayName,String body,Instant createdAt){}
 public record TicketDetail(TicketSummary ticket,List<TicketMessage> messages){}
 public record BusinessMessageView(Long id,Long targetBusinessId,String targetBusinessName,BusinessMessageType type,String subject,String body,boolean read,Instant createdAt){}
 public record Page<T>(List<T> items,int page,boolean hasMore){}
 public record UnreadCount(long unreadCount){}
}
