package au.com.getkempt.configurations.web;

import au.com.getkempt.common.idempotency.IdempotencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers cross-cutting MVC interceptors. The idempotency interceptor runs after
 * security (so it has the resolved tenant) and only acts on opted-in mutating requests.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // See WireEnumConverterFactory javadoc: registering this here takes
        // precedence over Spring's built-in StringToEnumConverterFactory for the
        // same (String -> Enum) pair, so every @RequestParam/@PathVariable enum in
        // the app parses the contract's lower-case wire values instead of 400ing.
        registry.addConverterFactory(new WireEnumConverterFactory());
    }
}
