package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="ticket_code", nullable=false, unique=true, length=40) private String ticketCode;
    @Column(name="requester_user_id") private Long requesterUserId;
    @Column(name="requester_name", nullable=false, length=150) private String requesterName;
    @Column(name="requester_email", nullable=false, length=150) private String requesterEmail;
    @Column(name="requester_role", nullable=false, length=20) private String requesterRole="NO_AUTENTICADO";
    @Column(nullable=false, length=30) private String category="OTHER";
    @Column(nullable=false, length=180) private String subject;
    @Column(nullable=false, columnDefinition="TEXT") private String description;
    @Column(nullable=false, length=10) private String priority="NORMAL";
    @Column(nullable=false, length=20) private String status="NEW";
    @Column(name="assigned_to") private Long assignedTo;
    @Column(name="escalated_to") private Long escalatedTo;
    @Column(name="escalation_reason", columnDefinition="TEXT") private String escalationReason;
    @Column(columnDefinition="TEXT") private String resolution;
    @Column(name="resolved_at") private LocalDateTime resolvedAt;
    @Column(name="closed_at") private LocalDateTime closedAt;
    @Column(name="created_at", insertable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", insertable=false, updatable=false) private LocalDateTime updatedAt;
    public Long getId(){return id;} public String getTicketCode(){return ticketCode;} public void setTicketCode(String v){ticketCode=v;}
    public Long getRequesterUserId(){return requesterUserId;} public void setRequesterUserId(Long v){requesterUserId=v;}
    public String getRequesterName(){return requesterName;} public void setRequesterName(String v){requesterName=v;}
    public String getRequesterEmail(){return requesterEmail;} public void setRequesterEmail(String v){requesterEmail=v;}
    public String getRequesterRole(){return requesterRole;} public void setRequesterRole(String v){requesterRole=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;}
    public String getDescription(){return com.ritechsas.backend.util.SupportTextSanitizer.redactCredentials(description);} public void setDescription(String v){description=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Long getAssignedTo(){return assignedTo;} public void setAssignedTo(Long v){assignedTo=v;}
    public Long getEscalatedTo(){return escalatedTo;} public void setEscalatedTo(Long v){escalatedTo=v;}
    public String getEscalationReason(){return escalationReason;} public void setEscalationReason(String v){escalationReason=v;}
    public String getResolution(){return com.ritechsas.backend.util.SupportTextSanitizer.redactCredentials(resolution);} public void setResolution(String v){resolution=v;}
    public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime v){resolvedAt=v;}
    public LocalDateTime getClosedAt(){return closedAt;} public void setClosedAt(LocalDateTime v){closedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
