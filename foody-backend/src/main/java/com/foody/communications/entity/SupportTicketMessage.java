package com.foody.communications.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="support_ticket_messages") public class SupportTicketMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="ticket_id",nullable=false) private Long ticketId; @Column(name="sender_user_id",nullable=false) private Long senderUserId; @Enumerated(EnumType.STRING) @Column(name="sender_type",nullable=false) private CommunicationSenderType senderType; @Column(nullable=false,length=4000) private String body; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @PrePersist void create(){createdAt=Instant.now();}
 public Long getId(){return id;} public Long getTicketId(){return ticketId;} public void setTicketId(Long v){ticketId=v;} public Long getSenderUserId(){return senderUserId;} public void setSenderUserId(Long v){senderUserId=v;} public CommunicationSenderType getSenderType(){return senderType;} public void setSenderType(CommunicationSenderType v){senderType=v;} public String getBody(){return body;} public void setBody(String v){body=v;} public Instant getCreatedAt(){return createdAt;}
}
