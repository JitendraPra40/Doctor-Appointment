package com.app.appointment_service;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that need a full Spring context + embedded Kafka.
 *
 * Usage:
 * ──────
 * @SpringBootTest
 * class MyServiceIntegrationTest extends BaseIntegrationTest {
 *     @Autowired MyService service;
 *     // ...tests...
 * }
 *
 * Topics defined here cover all services in the system.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:19092",
                "port=19092"
        },
        topics = {
                "appointment-events",
                "appointment-cancelled-events",
                "payment-events",
                "payment-failed-events",
                "doctor-events"
        }
)
public abstract class BaseIntegrationTest {
    // Extend this class to get embedded Kafka + full Spring context
    // Override @BeforeEach / @AfterEach as needed in subclasses
}
