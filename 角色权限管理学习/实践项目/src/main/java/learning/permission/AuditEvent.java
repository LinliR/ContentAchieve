package learning.permission;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Instant occurredAt = Instant.now();
    @Column(nullable = false) String actor;
    @Column(nullable = false) String action;
    @Column(nullable = false) String target;
    @Column(nullable = false, length = 2000) String detail;
    protected AuditEvent() {}
    AuditEvent(String actor, String action, String target, String detail) {
        this.actor = actor; this.action = action; this.target = target; this.detail = detail;
    }
}
