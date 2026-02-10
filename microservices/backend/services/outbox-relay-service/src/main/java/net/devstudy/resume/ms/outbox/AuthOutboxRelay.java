package net.devstudy.resume.ms.outbox;

import java.time.Instant;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEvent;
import net.devstudy.resume.auth.internal.entity.AuthOutboxEventType;
import net.devstudy.resume.auth.internal.entity.AuthOutboxStatus;
import net.devstudy.resume.auth.internal.repository.storage.AuthOutboxRepository;
import net.devstudy.resume.notification.api.messaging.NotificationMessaging;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.outbox.relay.mode", havingValue = "auth")
public class AuthOutboxRelay {

    private static final int MAX_BACKOFF_MULTIPLIER = 10;

    private final AuthOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxRelayProperties properties;

    @Scheduled(fixedDelayString = "${app.outbox.relay.poll-interval-ms:2000}")
    @Transactional
    public void relay() {
        Instant now = Instant.now();
        int batchSize = Math.max(1, properties.getBatchSize());
        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        var batch = outboxRepository.lockNextBatch(now, batchSize, maxAttempts);
        if (batch.isEmpty()) {
            return;
        }
        for (AuthOutboxEvent event : batch) {
            if (event == null) {
                continue;
            }
            try {
                publish(event);
                markSent(event, now);
            } catch (Exception ex) {
                markFailed(event, ex, now);
            }
            outboxRepository.save(event);
        }
    }

    private void publish(AuthOutboxEvent event) {
        String routingKey = resolveRoutingKey(event.getEventType());
        if (routingKey == null) {
            throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        }
        String payload = event.getPayload();
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Outbox payload is empty");
        }
        rabbitTemplate.convertAndSend(NotificationMessaging.EXCHANGE, routingKey, payload);
    }

    private String resolveRoutingKey(AuthOutboxEventType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case RESTORE_ACCESS_MAIL -> NotificationMessaging.ROUTING_KEY_RESTORE;
        };
    }

    private void markSent(AuthOutboxEvent event, Instant now) {
        event.setStatus(AuthOutboxStatus.SENT);
        event.setSentAt(now);
        event.setAvailableAt(now);
        event.setLastError(null);
    }

    private void markFailed(AuthOutboxEvent event, Exception ex, Instant now) {
        int attempts = Math.max(0, event.getAttempts()) + 1;
        event.setAttempts(attempts);
        event.setStatus(AuthOutboxStatus.ERROR);
        event.setLastError(truncate(ex.getMessage(), 1000));
        long baseDelay = Math.max(1000L, properties.getRetryDelayMs());
        long multiplier = Math.min(attempts, MAX_BACKOFF_MULTIPLIER);
        event.setAvailableAt(now.plusMillis(baseDelay * multiplier));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
