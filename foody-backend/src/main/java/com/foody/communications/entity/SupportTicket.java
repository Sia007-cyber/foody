package com.foody.communications.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="support_tickets") public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="business_id") private Long businessId;
 @Column(name="requester_user_id",nullable=false) private Long requesterUserId;
 @Enumerated(EnumType.STRING) @Column(name="requester_type",nullable=false) private TicketRequesterType requesterType=TicketRequesterType.BUSINESS_OWNER;
 @Column(nullable=false,length=160) private String subject;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private TicketStatus status=TicketStatus.OPEN;
 @Column(name="requester_last_read_at") private Instant requesterLastReadAt;
 @Column(name="admin_last_read_at") private Instant adminLastReadAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist void create(){Instant now=Instant.now();createdAt=now;updatedAt=now;}
 @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Long getBusinessId(){return businessId;} public void setBusinessId(Long v){businessId=v;} public Long getRequesterUserId(){return requesterUserId;} public void setRequesterUserId(Long v){requesterUserId=v;} public TicketRequesterType getRequesterType(){return requesterType;} public void setRequesterType(TicketRequesterType v){requesterType=v;} public String getSubject(){return subject;} public void setSubject(String v){subject=v;} public TicketStatus getStatus(){return status;} public void setStatus(TicketStatus v){status=v;} public Instant getRequesterLastReadAt(){return requesterLastReadAt;} public void setRequesterLastReadAt(Instant v){requesterLastReadAt=v;} public Instant getAdminLastReadAt(){return adminLastReadAt;} public void setAdminLastReadAt(Instant v){adminLastReadAt=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
