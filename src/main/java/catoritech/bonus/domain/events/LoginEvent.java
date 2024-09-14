package catoritech.bonus.domain.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class LoginEvent {

    private UUID userId;

    private Instant timestamp;
}
