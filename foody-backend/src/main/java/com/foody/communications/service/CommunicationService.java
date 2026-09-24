package com.foody.communications.service;
import com.foody.communications.dto.*; import com.foody.communications.entity.TicketRequesterType; import com.foody.communications.entity.TicketStatus;
public interface CommunicationService {
 CommunicationResponses.TicketDetail createTicket(Long ownerId,CommunicationRequests.CreateTicket request);
 CommunicationResponses.Page<CommunicationResponses.TicketSummary> ownerTickets(Long ownerId,int page,int limit);
 CommunicationResponses.TicketDetail ownerTicket(Long ownerId,Long ticketId);
 CommunicationResponses.TicketDetail ownerReply(Long ownerId,Long ticketId,CommunicationRequests.Reply request);
 CommunicationResponses.Page<CommunicationResponses.BusinessMessageView> ownerMessages(Long ownerId,int page,int limit);
 CommunicationResponses.BusinessMessageView ownerMessage(Long ownerId,Long messageId);
 CommunicationResponses.UnreadCount ownerUnread(Long ownerId);
 CommunicationResponses.TicketDetail createCustomerTicket(Long customerId,CommunicationRequests.CreateTicket request);
 CommunicationResponses.Page<CommunicationResponses.TicketSummary> customerTickets(Long customerId,int page,int limit);
 CommunicationResponses.TicketDetail customerTicket(Long customerId,Long ticketId);
 CommunicationResponses.TicketDetail customerReply(Long customerId,Long ticketId,CommunicationRequests.Reply request);
 CommunicationResponses.UnreadCount customerUnread(Long customerId);
 CommunicationResponses.Page<CommunicationResponses.TicketSummary> adminTickets(TicketStatus status,TicketRequesterType requesterType,int page,int limit);
 CommunicationResponses.TicketDetail adminTicket(Long adminId,Long ticketId);
 CommunicationResponses.TicketDetail adminReply(Long adminId,Long ticketId,CommunicationRequests.Reply request);
 CommunicationResponses.TicketDetail closeTicket(Long adminId,Long ticketId);
 CommunicationResponses.BusinessMessageView sendDirect(Long adminId,CommunicationRequests.DirectMessage request);
 CommunicationResponses.BusinessMessageView sendBroadcast(Long adminId,CommunicationRequests.Broadcast request);
 CommunicationResponses.Page<CommunicationResponses.BusinessMessageView> sentMessages(int page,int limit);
}
