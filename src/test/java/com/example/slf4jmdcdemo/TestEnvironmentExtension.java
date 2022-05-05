package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.function.Supplier;

/**
 * Allows to temporarily override Environment properties at runtime for test purposes
 */
@SuppressWarnings("UnusedReturnValue")
public class TestEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {

    public static final String NAME = TestEnvironmentExtension.class + ".properties";

    private final Supplier<ConfigurableEnvironment> envSupplier;
    private final HashMap<String, Object> properties = new HashMap<>();

    public static TestEnvironmentExtension with(Supplier<ConfigurableEnvironment> envSupplier) {
        return new TestEnvironmentExtension(envSupplier);
    }

    private TestEnvironmentExtension(@NonNull Supplier<ConfigurableEnvironment> envSupplier) {
        this.envSupplier = envSupplier;
    }

    public TestEnvironmentExtension withProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        properties.clear();
        envSupplier.get().getPropertySources().remove(NAME);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        final MapPropertySource testPropertySource = new MapPropertySource(NAME, properties);
        envSupplier.get().getPropertySources().addFirst(testPropertySource);
    }
}
