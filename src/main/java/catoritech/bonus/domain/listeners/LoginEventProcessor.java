package catoritech.bonus.domain.listeners;

import catoritech.bonus.domain.events.BonusEvent;
import catoritech.bonus.domain.events.LoginProcessEvent;
import catoritech.bonus.service.BonusService;
import catoritech.bonus.service.EventService;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
@Slf4j
public class LoginEventProcessor {

    private final BonusService bonusService;
    private final EventService eventService;
    private final Emitter<BonusEvent> bonusEventEmitter;

    public LoginEventProcessor(
            BonusService bonusService,
            EventService eventService,
            @Channel("bonus-events-out") Emitter<BonusEvent> bonusEventEmitter
    ) {
        this.bonusService = bonusService;
        this.eventService = eventService;
        this.bonusEventEmitter = bonusEventEmitter;
    }

    @ConsumeEvent("login-events-process")
    public Uni<Void> processLoginEvent(LoginProcessEvent loginProcessEvent) {
        UUID eventId = loginProcessEvent.getEventId();
        log.debug("#processLoginEvent: loginProcessEvent={}", loginProcessEvent);
        Context context = Vertx.currentContext();

        return eventService.checkIfLoginEventShouldBeSkipped(eventId)
                .flatMap(skip -> {
                    if (skip) {
                        log.warn("eventId={} already processed, skipping...", eventId);
                        return Uni.createFrom().voidItem();
                    }

                    return bonusService.calculateBonus(loginProcessEvent.getUserId())
                            .onFailure()
                            .invoke(throwable -> log.error(
                                    "error calculating bonus for userId={}",
                                    loginProcessEvent.getUserId()
                            ))
                            .flatMap(playerBonus -> createAndSendBonusEvent(loginProcessEvent, playerBonus))
                            .onFailure()
                            .invoke(throwable -> log.error(
                                    "error sending to Kafka for eventId={}, userId={}",
                                    eventId,
                                    loginProcessEvent.getUserId()
                            ))
                            .eventually(() -> updateEventToProcessed(eventId, context));
                })
                .replaceWithVoid();
    }

    private Uni<Void> createAndSendBonusEvent(
            LoginProcessEvent loginProcessEvent,
            BigDecimal playerBonus
    ) {
        return bonusService.createBonusEvent(loginProcessEvent, playerBonus)
                .flatMap(bonusEvent -> {
                    OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                            .withKey(bonusEvent.getEventId())
                            .build();
                    Message<BonusEvent> message = Message.of(bonusEvent).addMetadata(metadata);

                    return Uni.createFrom().emitter(emitter -> bonusEventEmitter.send(message
                            .withAck(() -> {
                                // Called when the message is acked
                                emitter.complete(null);
                                log.info(
                                        "message successfully acknowledged for eventId={}, userId={}",
                                        bonusEvent.getEventId(),
                                        loginProcessEvent.getUserId()
                                );
                                return CompletableFuture.completedFuture(null);
                            })
                            .withNack(throwable -> {
                                log.error(
                                        "failed to ack message for eventId={}, userId={}",
                                        bonusEvent.getEventId(),
                                        loginProcessEvent.getUserId()
                                );
                                emitter.fail(throwable);
                                return CompletableFuture.failedFuture(throwable);
                            })));
                });
    }

    private Uni<Void> updateEventToProcessed(UUID eventId, Context context) {
        return Uni.createFrom().emitter(emitter ->
                context.runOnContext(v ->
                        eventService.updateEventToProcessed(eventId)
                                .subscribe().with(
                                        action -> emitter.complete(null),
                                        emitter::fail
                                )
                )
        );
    }
}
