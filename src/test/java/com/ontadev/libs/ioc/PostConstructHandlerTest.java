package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.injection.Inject;
import com.ontadev.libs.ioc.annotation.lifecycle.PostConstruct;
import com.ontadev.libs.ioc.handlers.impl.InjectFieldHandler;
import com.ontadev.libs.ioc.handlers.impl.PostConstructHandler;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PostConstructHandlerTest {
    public static class Dependency {
        @Inject private String value;
    }

    public static class Bean {
        @Inject private Dependency dependency;
        private int calls;

        @PostConstruct
        private void initialize() {
            assertEquals("injected", dependency.value);
            calls++;
        }
    }

    public static class Broken {
        @PostConstruct
        private void initialize() { throw new IllegalArgumentException("boom"); }
    }

    public static class Invalid {
        @PostConstruct void arguments(String value) {}
        @PostConstruct static void staticMethod() {}
        @PostConstruct int result() { return 1; }
    }

    @Test
    void invokesPrivateHookAfterAllFieldInjectionOnlyOnceAcrossAliasesAndInitialization() {
        IoCContainer container = new IoCContainer();
        container.registerFieldHandler(new InjectFieldHandler());
        container.registerMethodHandler(new PostConstructHandler());
        Bean bean = new Bean();
        container.registerInstance(Bean.class, bean);
        container.registerInstance(Object.class, bean);
        container.registerInstance(Dependency.class, new Dependency());
        container.registerInstance(String.class, "injected");
        assertEquals(0, bean.calls);
        container.initialize(Set.of());
        assertEquals(1, bean.calls);
        container.initialize(Set.of());
        assertEquals(1, bean.calls);
    }

    @Test
    void propagatesHookFailureWithOriginalCause() {
        IoCContainer container = new IoCContainer();
        container.registerMethodHandler(new PostConstructHandler());
        container.registerInstance(Broken.class, new Broken());
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> container.initialize(Set.of()));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
        assertEquals("boom", failure.getCause().getMessage());
    }

    @Test
    void rejectsInvalidSignatures() {
        PostConstructHandler handler = new PostConstructHandler();
        for (java.lang.reflect.Method method : Invalid.class.getDeclaredMethods()) {
            assertThrows(IllegalArgumentException.class, () -> handler.handle(
                    new IoCContainer(), new Invalid(), method, method.getAnnotation(PostConstruct.class)));
        }
    }
}
