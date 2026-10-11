package au.com.getkempt.configurations.web;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Fixes a latent, codebase-wide bug: every wire enum here (e.g. {@code
 * BookingStatus}, {@code PackageStatus}) uses lower-case snake_case values on the
 * wire (contract §3) via {@code @JsonValue}/{@code @JsonCreator} — which Jackson
 * honours for JSON request BODIES — but query/path params (e.g. {@code
 * ?status=confirmed} on {@code GET /bookings}) go through Spring's OWN conversion
 * service, not Jackson. Spring's default {@code StringToEnumConverterFactory}
 * matches by the enum constant's Java NAME ({@code CONFIRMED}), so without this,
 * every {@code @RequestParam SomeEnum} in the app 400s on exactly the lower-case
 * values the contract itself documents.
 *
 * <p>Reflectively delegates to each enum's existing {@code static T from(String)}
 * factory (the same one {@code @JsonCreator} already points at) so there is a
 * single source of truth for "how to parse this enum from the wire" — falling
 * back to {@link Enum#valueOf} for any enum that doesn't define one.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class WireEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToWireEnumConverter(targetType);
    }

    private static final class StringToWireEnumConverter<T extends Enum> implements Converter<String, T> {

        private final Class<T> enumType;
        private final Method fromFactory;

        private StringToWireEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
            this.fromFactory = resolveFromFactory(enumType);
        }

        private static Method resolveFromFactory(Class<?> enumType) {
            try {
                Method method = enumType.getMethod("from", String.class);
                return Modifier.isStatic(method.getModifiers()) ? method : null;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        @Override
        public T convert(String source) {
            if (source.isBlank()) {
                return null;
            }
            try {
                if (fromFactory != null) {
                    return (T) fromFactory.invoke(null, source);
                }
                return (T) Enum.valueOf(enumType, source);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(
                        "Invalid value '%s' for %s".formatted(source, enumType.getSimpleName()), e);
            }
        }
    }
}
