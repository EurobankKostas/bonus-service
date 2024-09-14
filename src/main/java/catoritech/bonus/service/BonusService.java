package catoritech.bonus.service;

import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.Event;
import catoritech.bonus.domain.events.BonusEvent;
import catoritech.bonus.domain.events.LoginEvent;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class BonusService {

    private final Mutiny.SessionFactory sessionFactory;

    public BonusService(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @WithTransaction
    public Uni<Integer> updateEventToProcessed(UUID eventId) {
        log.debug("persisting processed event for eventId={}", eventId);
        return Event.update("UPDATE Event SET processed = true, locked = false WHERE id = ?1", eventId)
                .onItem().invoke(count ->
                        log.debug("Updated {} rows for eventId={}", count, eventId));
    }

    //predefined criteria in every login will result to +1 bonus amount
    @WithTransaction
    public Uni<PlayerBonus> calculateBonus(UUID userId) {
        return PlayerBonus.<PlayerBonus>find("userId", userId)
                .firstResult()
                .onItem()
                .ifNull()
                .failWith(() -> new IllegalStateException("player not found for userId=" + userId))
                .invoke(playerBonus -> playerBonus.setTotalBonus(playerBonus.getTotalBonus().add(BigDecimal.ONE)));
    }

    public Uni<BonusEvent> createBonusEvent(LoginEvent loginEvent, PlayerBonus playerBonus) {
        log.debug("#createBonusEvent: {}", loginEvent);
        UUID userId = loginEvent.getUserId();
        if (Objects.isNull(playerBonus)) {
            throw new IllegalArgumentException(String.format("userId %s not found", userId));
        }
        return Uni.createFrom().item(new BonusEvent(userId, playerBonus.getTotalBonus(), UUID.randomUUID()));
    }

    public Uni<Void> persistProcessedEvent(UUID eventId, boolean locked, boolean processed) {
        log.debug("persisting processed event for eventId={}", eventId);
        Event event = new Event(eventId, locked, processed);
        return sessionFactory
                .withTransaction((session) -> session.persist(event)
                        .call(session::flush))
                .onFailure().invoke(th -> log.error("Failed to persist event: " + th.getMessage(), th));
    }

    public Uni<Boolean> checkIfLoginEventShouldBeSkipped(UUID eventId) {
        return sessionFactory.withTransaction((session) ->
                session.find(Event.class, eventId, LockModeType.PESSIMISTIC_WRITE)
                        .flatMap(event -> {
                            if (event == null) {
                                // No event found, create a new one.
                                return persistProcessedEvent(eventId, true, false)
                                        .onItem()
                                        .transform(inserted -> false);
                            } else {
                                // Log and skip if the event is either locked or processed.
                                log.debug("skipping event for eventId={}, Locked={}, Processed={}", eventId, event.isLocked(), event.isProcessed());
                                return Uni.createFrom().item(true);
                            }
                        })
        );
    }
}
