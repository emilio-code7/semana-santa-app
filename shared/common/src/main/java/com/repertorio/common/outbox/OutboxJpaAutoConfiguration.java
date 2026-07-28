package com.repertorio.common.outbox;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration that registers the shared outbox JPA entity and repository
 * so they are discovered by the persistence layer.
 * Only activates when an {@link EntityManagerFactory} bean is available,
 * which means it is skipped in {@code @WebMvcTest} slice tests.
 */
@AutoConfiguration(after = DataJpaRepositoriesAutoConfiguration.class)
@ConditionalOnBean(EntityManagerFactory.class)
@EntityScan("com.repertorio.common.outbox")
@EnableJpaRepositories(basePackageClasses = OutboxEventJpaRepository.class)
public class OutboxJpaAutoConfiguration {
}
