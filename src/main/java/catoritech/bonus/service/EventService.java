package catoritech.bonus.service;

import catoritech.bonus.domain.Event;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@ApplicationScoped
@Slf4j
public class EventService {

    @WithTransaction
    public Uni<Void> persistEvent(UUID eventId, boolean locked, boolean processed) {
        log.debug("#persistEvent: eventId={}", eventId);
        Event event = new Event(eventId, locked, processed);
        return Event.persist(event);
    }

    @WithTransaction
    public Uni<Integer> updateEventToProcessed(UUID eventId) {
        log.debug("#updateEventToProcessed: eventId={}", eventId);
        return Event.update("UPDATE Event SET processed = true, locked = false WHERE id = ?1", eventId)
                .onItem().invoke(count ->
                        log.debug("updated {} rows for eventId={}", count, eventId));
    }

    /**
     * Determines if a login event should be skipped using pessimistic write locking.
     * This method ensures safe access to the pipeline, preventing concurrent computations.
     * It acquires a pessimistic write lock on the event's database record.
     * The use of pessimistic locking is acceptable here because the transition logic executed
     * under the lock is minimal, thus not significantly impacting performance.Also we can configure an appropriate lock timeout
     * Another alternative would be to rely on the primary key constraint to handle failures during insertion and remove the lock.
     * However, this approach introduces the overhead of exception, transaction rollback, and reduces predictability."
     * @param eventId the UUID of the event to check
     * @return a {@link Uni<Boolean>} that emits {@code true} if the event should be skipped,
     *         otherwise {@code false}
     */
    public Uni<Boolean> checkIfLoginEventShouldBeSkipped(UUID eventId) {
        return Panache
                .withTransaction(() ->
                        Event.<Event>find("id", eventId)
                                .withLock(LockModeType.PESSIMISTIC_WRITE)
                                .firstResult()
                                .flatMap(event -> {
                                    if (event == null) {
                                        // No event found, create a new one.
                                        return persistEvent(eventId, true, false)
                                                .onItem()
                                                .transform(inserted -> false);
                                    } else {
                                        // Log and skip if the event is either locked or processed.
                                        log.debug("skipping event for eventId={}, locked={}, processed={}", eventId, event.isLocked(), event.isProcessed());
                                        return Uni.createFrom().item(true);
                                    }
                                })
                )
                .onFailure()
                .invoke(throwable -> log.error("exception occurred in checkIfLoginEventShouldBeSkipped: {}", throwable.getMessage(), throwable));
    }
}
