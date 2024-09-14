create table player_bonus
(
    id                uuid           not null,
    totalAchievements bigint,
    totalBonus        numeric(38, 2) not null,
    userId              uuid           not null,
    primary key (id)
);

CREATE TABLE event
(
    event_id     UUID NOT NULL,
    processed_at TIMESTAMP(6),
    locked       BOOLEAN DEFAULT FALSE, -- Indicates if the event is currently being processed //note for locking
    processed    BOOLEAN DEFAULT FALSE, -- Indicates if the event has been fully processed
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_player_bonus_userId ON player_bonus (userId);
