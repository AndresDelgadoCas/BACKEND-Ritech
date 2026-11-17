package com.ritechsas.backend.entity;

import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_user_id") private Long actorUserId;
    @Column(nullable = false, length = 100) private String action;
    @Column(name = "entity_type", nullable = false, length = 100) private String entityType;
    @Column(name = "entity_id") private Long entityId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json") private Map<String, Object> details;
    @Generated(event = EventType.INSERT) @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public AuditLog() {}
    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long value) { actorUserId = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { entityType = value; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long value) { entityId = value; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> value) { details = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
