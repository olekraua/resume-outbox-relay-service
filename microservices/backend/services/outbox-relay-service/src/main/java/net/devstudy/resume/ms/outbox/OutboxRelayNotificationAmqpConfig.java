package net.devstudy.resume.ms.outbox;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.devstudy.resume.notification.api.messaging.NotificationMessaging;

@Configuration
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.outbox.relay.mode", havingValue = "auth")
public class OutboxRelayNotificationAmqpConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NotificationMessaging.EXCHANGE, true, false);
    }
}
