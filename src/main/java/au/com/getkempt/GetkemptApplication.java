package au.com.getkempt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = {
		"au.com.getkempt.repositories",
		"au.com.getkempt.common.idempotency"
})
public class GetkemptApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetkemptApplication.class, args);
	}

}
