package com.foody.communications.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="support_tickets") public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="business_id",nullable=false) private Long businessId;
 @Column(name="owner_user_id",nullable=false) private Long ownerUserId;
 @Column(nullable=false,length=160) private String subject;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private TicketStatus status=TicketStatus.OPEN;
 @Column(name="owner_last_read_at") private Instant ownerLastReadAt;
 @Column(name="admin_last_read_at") private Instant adminLastReadAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist void create(){Instant now=Instant.now();createdAt=now;updatedAt=now;}
 @PreUpdate void update(){updatedAt=Instant.now();}
 public Long getId(){return id;} public Long getBusinessId(){return businessId;} public void setBusinessId(Long v){businessId=v;} public Long getOwnerUserId(){return ownerUserId;} public void setOwnerUserId(Long v){ownerUserId=v;} public String getSubject(){return subject;} public void setSubject(String v){subject=v;} public TicketStatus getStatus(){return status;} public void setStatus(TicketStatus v){status=v;} public Instant getOwnerLastReadAt(){return ownerLastReadAt;} public void setOwnerLastReadAt(Instant v){ownerLastReadAt=v;} public Instant getAdminLastReadAt(){return adminLastReadAt;} public void setAdminLastReadAt(Instant v){adminLastReadAt=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
