package catoritech.bonus.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class Event extends PanacheEntityBase {

    public Event(UUID eventId, boolean processed, boolean locked) {
        this.eventId = eventId;
        this.processed = processed;
        this.locked = locked;
    }

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime processedAt;

    private boolean processed;

    private boolean locked;
}
