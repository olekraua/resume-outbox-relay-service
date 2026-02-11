CREATE TABLE IF NOT EXISTS profile_outbox (
    id bigserial PRIMARY KEY,
    profile_id bigint,
    event_type varchar(32) NOT NULL,
    payload text NOT NULL,
    status varchar(16) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    available_at timestamp with time zone NOT NULL DEFAULT now(),
    sent_at timestamp with time zone,
    last_error text
);

CREATE INDEX IF NOT EXISTS profile_outbox_status_available_idx
    ON profile_outbox (status, available_at, id);
