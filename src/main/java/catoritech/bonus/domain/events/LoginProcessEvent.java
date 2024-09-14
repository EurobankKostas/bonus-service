package catoritech.bonus.domain.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.UUID;

@Getter
@Setter
@ToString
public class LoginProcessEvent extends LoginEvent {

    private UUID eventId;

    public LoginProcessEvent(LoginEvent loginEvent, UUID eventId) {
        super(loginEvent.getUserId(), loginEvent.getTimestamp());
        this.eventId = eventId;
    }
}
