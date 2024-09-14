package catoritech.bonus.domain.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BonusEvent {

    private UUID userId;

    private BigDecimal bonusAmount;

    private UUID eventId;
}
