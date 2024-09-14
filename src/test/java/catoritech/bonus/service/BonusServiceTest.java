package catoritech.bonus.service;

import catoritech.bonus.domain.Event;
import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.events.BonusEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import catoritech.bonus.domain.events.LoginEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
public class BonusServiceTest {

    @Inject
    BonusService bonusService;

    @Test
    void testProcessLoginEvent_SkippedEvent() {
        Mutiny.Session mockSession = Mockito.mock(Mutiny.Session.class);
        UUID mockEventId = UUID.randomUUID();
        Event event = new Event();
        event.setEventId(mockEventId);
        event.setProcessed(true);
        Mockito.when(mockSession.find(Event.class, mockEventId, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(Uni.createFrom().item(event));

        Uni<Boolean> result = bonusService.checkIfLoginEventShouldBeSkipped(mockEventId);

        result.subscribe().with(
                Assertions::assertTrue,
                failure -> fail("login process should have been skipped")
        );
    }

    @Test
    void testCreateBonusEventValidInputs() {
        UUID mockUserId = UUID.randomUUID();
        LoginEvent loginEvent = new LoginEvent(mockUserId, Instant.now());
        PlayerBonus playerBonus = new PlayerBonus();
        playerBonus.setTotalBonus(BigDecimal.TEN);
        playerBonus.setId(mockUserId);

        Uni<BonusEvent> resultUni = bonusService.createBonusEvent(loginEvent, playerBonus);

        BonusEvent result = resultUni.await().indefinitely();
        assertThat(result.getUserId(), is(mockUserId));
    }
}
