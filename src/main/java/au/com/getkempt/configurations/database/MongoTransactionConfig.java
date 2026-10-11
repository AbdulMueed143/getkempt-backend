package au.com.getkempt.configurations.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Enables multi-document transactions, required for race-safe booking creation
 * ({@code docs/05-booking-lifecycle.md}): the overlap check + slot-lock acquisition
 * + insert must be atomic.
 *
 * <p>NOTE: MongoDB only supports multi-document transactions on a <b>replica set</b>
 * (or sharded cluster). For local development run mongod with {@code --replSet} and
 * initiate it once (e.g. {@code rs.initiate()}); a standalone instance will reject
 * transactions at runtime. Defining this bean is harmless either way.
 */
@Configuration
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
