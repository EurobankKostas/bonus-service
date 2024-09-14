package catoritech.bonus.service;

import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.events.BonusEvent;
import catoritech.bonus.domain.events.LoginProcessEvent;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
@Slf4j
public class LoginEventProcessor {

    private final BonusService bonusService;

    private final Emitter<BonusEvent> bonusEventEmitter;

    public LoginEventProcessor(BonusService bonusService, @Channel("bonus-events-out") Emitter<BonusEvent> bonusEventEmitter) {
        this.bonusService = bonusService;
        this.bonusEventEmitter = bonusEventEmitter;
    }

    @ConsumeEvent("login-events-process")
    public Uni<Void> processLoginEvent(LoginProcessEvent loginProcessEvent) {
        UUID eventId = loginProcessEvent.getEventId();
        log.debug("#processLoginEvent: Processing login event for eventId={}", eventId);
        Context context = Vertx.currentContext();

        return bonusService.checkIfLoginEventShouldBeSkipped(eventId)
                .flatMap(skip -> {
                    if (skip) {
                        log.warn("eventId={} already processed, skipping...", eventId);
                        return Uni.createFrom().voidItem();
                    }
                    return processBonusEvent(loginProcessEvent, eventId, context);
                })
                .replaceWithVoid();
    }

    private Uni<Void> processBonusEvent(LoginProcessEvent loginProcessEvent, UUID eventId, Context context) {
        return bonusService.calculateBonus(loginProcessEvent.getUserId())
                .flatMap(playerBonus -> createAndSendBonusEvent(loginProcessEvent, playerBonus, eventId, context))
                .onFailure().invoke(ex -> log.error("error processing player bonus for eventId={}", eventId, ex));
    }

    private Uni<Void> createAndSendBonusEvent(
            LoginProcessEvent loginProcessEvent,
            PlayerBonus playerBonus,
            UUID eventId,
            Context context
    ) {
        return bonusService.createBonusEvent(loginProcessEvent, playerBonus)
                .flatMap(bonusEvent -> Uni.createFrom().emitter((emitter) -> {
                            OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
                                    .withKey(bonusEvent.getEventId())
                                    .build();
                            Message<BonusEvent> message = Message.of(bonusEvent).addMetadata(metadata);
                            //https://quarkus.io/guides/kafka#sending-messages-with-emitter check callbacks in case of ack
                            bonusEventEmitter.send(message
                                    .withAck(() -> {
                                        // Called when the message is acked
                                        emitter.complete(null);
                                        log.info("message successfully acknowledged for eventId={}", bonusEvent.getEventId());
                                        return CompletableFuture.completedFuture(null);
                                    })
                                    .withNack(throwable -> {
                                        log.error("failed to ack message for eventId={}", bonusEvent.getEventId(), throwable);
                                        emitter.fail(throwable);
                                        return CompletableFuture.failedFuture(throwable);
                                    }));
                        })
                        .flatMap(action -> updateEventToProcessed(eventId, context)))
                .onFailure().invoke(ex -> log.error("error sending bonus event for eventId={}", eventId, ex));
    }

    private Uni<Void> updateEventToProcessed(UUID eventId, Context context) {
        return Uni.createFrom().emitter(emitter ->
                context.runOnContext(v ->
                        bonusService.updateEventToProcessed(eventId)
                                .subscribe().with(
                                        action -> emitter.complete(null),
                                        emitter::fail
                                )
                )
        );
    }
}
