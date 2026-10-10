package au.com.getkempt.configurations;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfiguration {

    /**
     * Argon2id password hashing — per {@code docs/04-domain-model.md} (User.password_hash
     * is argon2id). Uses Spring Security's recommended defaults (backed by BouncyCastle).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
