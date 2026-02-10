package net.devstudy.resume.ms.outbox;

import java.time.Instant;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.ProfileOutboxEvent;
import net.devstudy.resume.profile.api.model.ProfileOutboxEventType;
import net.devstudy.resume.profile.api.model.ProfileOutboxStatus;
import net.devstudy.resume.profile.internal.repository.storage.ProfileOutboxRepository;
import net.devstudy.resume.search.api.messaging.SearchIndexingMessaging;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.outbox.relay.mode", havingValue = "profile", matchIfMissing = true)
public class ProfileOutboxRelay {

    private static final int MAX_BACKOFF_MULTIPLIER = 10;

    private final ProfileOutboxRepository outboxRepository;
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
        for (ProfileOutboxEvent event : batch) {
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

    private void publish(ProfileOutboxEvent event) {
        String routingKey = resolveRoutingKey(event.getEventType());
        if (routingKey == null) {
            throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        }
        String payload = event.getPayload();
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Outbox payload is empty");
        }
        rabbitTemplate.convertAndSend(SearchIndexingMessaging.EXCHANGE, routingKey, payload);
    }

    private String resolveRoutingKey(ProfileOutboxEventType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PROFILE_INDEX -> SearchIndexingMessaging.ROUTING_KEY_INDEX;
            case PROFILE_REMOVE -> SearchIndexingMessaging.ROUTING_KEY_REMOVE;
        };
    }

    private void markSent(ProfileOutboxEvent event, Instant now) {
        event.setStatus(ProfileOutboxStatus.SENT);
        event.setSentAt(now);
        event.setAvailableAt(now);
        event.setLastError(null);
    }

    private void markFailed(ProfileOutboxEvent event, Exception ex, Instant now) {
        int attempts = Math.max(0, event.getAttempts()) + 1;
        event.setAttempts(attempts);
        event.setStatus(ProfileOutboxStatus.ERROR);
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
