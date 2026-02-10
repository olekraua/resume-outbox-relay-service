package net.devstudy.resume.ms.outbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.devstudy.resume.profile.api.config.ProfileJpaConfig;
import net.devstudy.resume.auth.api.config.AuthJpaConfig;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "net.devstudy.resume")
@EnableConfigurationProperties(OutboxRelayProperties.class)
@Import({ProfileJpaConfig.class, AuthJpaConfig.class})
public class OutboxRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutboxRelayApplication.class, args);
    }
}
