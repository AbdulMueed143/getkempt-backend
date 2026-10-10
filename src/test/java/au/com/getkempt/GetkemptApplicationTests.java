package au.com.getkempt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full context load. Needs a reachable MongoDB because {@code auto-index-creation}
 * builds indexes at startup, so it only runs when {@code MONGODB_URI} is set
 * (locally via docker-compose, or in CI). Without it the build still passes.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
class GetkemptApplicationTests {

	@Test
	void contextLoads() {
	}

}
