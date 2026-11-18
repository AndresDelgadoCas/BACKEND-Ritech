package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name="support_messages")
public class SupportMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="ticket_id", nullable=false) private Long ticketId;
    @Column(name="sender_user_id") private Long senderUserId;
    @Column(name="sender_type", nullable=false, length=20) private String senderType;
    @Column(nullable=false, columnDefinition="TEXT") private String message;
    @Column(name="is_internal", nullable=false) private boolean internal=false;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Transient private String senderName;
    public Long getId(){return id;} public Long getTicketId(){return ticketId;} public void setTicketId(Long v){ticketId=v;}
    public Long getSenderUserId(){return senderUserId;} public void setSenderUserId(Long v){senderUserId=v;}
    public String getSenderType(){return senderType;} public void setSenderType(String v){senderType=v;}
    public String getMessage(){return com.ritechsas.backend.util.SupportTextSanitizer.redactCredentials(message);} public void setMessage(String v){message=v;}
    public String getSenderName(){return senderName;} public void setSenderName(String v){senderName=v;}
    public boolean isInternal(){return internal;} public void setInternal(boolean v){internal=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
