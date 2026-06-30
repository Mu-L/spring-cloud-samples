package io.micrometer.observation;

/**
 * Compatibility shim to reactivate Dubbo's {@code ObservationSenderFilter} and
 * {@code ObservationReceiverFilter}.
 *
 * <p>Dubbo's observation filters are conditionally activated based on the presence
 * of this class on the classpath. Spring Boot 4.1.0 upgraded {@code micrometer-observation}
 * to a version that removed {@code NoopObservationRegistry}, causing those filters
 * to be silently disabled and breaking distributed tracing propagation across Dubbo calls.
 *
 * <p>This class is intentionally left empty — it exists solely so that the classpath
 * check succeeds and Dubbo's observation filters are activated. No other code in the
 * project references this class.
 *
 * @see <a href="https://github.com/apache/dubbo">Apache Dubbo Observation Filters</a>
 */
public class NoopObservationRegistry {
}
