package catoritech.bonus.kafka.consumer;

import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.events.LoginEvent;
import catoritech.bonus.domain.events.LoginProcessEvent;
import catoritech.bonus.service.BonusService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.UUID;

@ApplicationScoped
@Slf4j
public class LoginEventConsumer {

    private final EventBus eventBus;

    public LoginEventConsumer(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Incoming("login-events-in")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public Uni<Void> processLoginEvent(KafkaRecord<UUID, LoginEvent> eventIdToLoginEvent) {
        log.debug("#processLoginEvent: eventId={}", eventIdToLoginEvent.getKey());
        LoginProcessEvent processEvent = new LoginProcessEvent(
                eventIdToLoginEvent.getPayload(),
                eventIdToLoginEvent.getKey()
        );
        eventBus.send("login-events-process", processEvent);
        return Uni.createFrom()
                .voidItem()
                .replaceWithVoid();
    }
}
