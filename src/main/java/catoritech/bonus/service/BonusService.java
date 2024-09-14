package catoritech.bonus.service;

import catoritech.bonus.domain.PlayerBonus;
import catoritech.bonus.domain.Event;
import catoritech.bonus.domain.events.BonusEvent;
import catoritech.bonus.domain.events.LoginEvent;
import catoritech.bonus.exception.ApplicationException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
@Slf4j
public class BonusService {


    //predefined criteria in every login will result to +1 bonus amount
    @WithTransaction
    public Uni<BigDecimal> calculateBonus(UUID userId) {
        return PlayerBonus.<PlayerBonus>find("userId", userId)
                .firstResult()
                .onItem()
                .ifNull()
                .failWith(() -> new ApplicationException("player not found for userId=" + userId))
                .flatMap(playerBonus -> Uni.createFrom().item(playerBonus.getTotalBonus().add(BigDecimal.ONE)));
    }

    public Uni<BonusEvent> createBonusEvent(LoginEvent loginEvent, BigDecimal playerBonus) {
        log.debug("#createBonusEvent: {}", loginEvent);
        UUID userId = loginEvent.getUserId();
        if (Objects.isNull(playerBonus)) {
            throw new ApplicationException(String.format("userId %s not found", userId));
        }
        return Uni.createFrom().item(new BonusEvent(userId, playerBonus, UUID.randomUUID()));
    }
}

