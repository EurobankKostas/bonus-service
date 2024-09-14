package catoritech.bonus.service;

import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.events.BonusEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import catoritech.bonus.domain.events.LoginEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class BonusServiceTest {

    @Inject
    BonusService bonusService;

    @Test
    void testCreateBonusEventValidInputs() {
        UUID mockUserId = UUID.randomUUID();
        LoginEvent loginEvent = new LoginEvent(mockUserId, Instant.now());
        PlayerBonus playerBonus = new PlayerBonus();
        playerBonus.setTotalBonus(BigDecimal.TEN);
        playerBonus.setId(mockUserId);

        Uni<BonusEvent> resultUni = bonusService.createBonusEvent(loginEvent, playerBonus.getTotalBonus());

        BonusEvent result = resultUni.await().indefinitely();
        assertThat(result.getBonusAmount(), is(playerBonus.getTotalBonus()));
    }
}
