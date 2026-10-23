// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.lifecycle.Shutdown;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.ioc.handlers.impl.ServiceHandler;
import com.ontadev.libs.ioc.handlers.impl.ShutdownHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ShutdownHandlerTest {

    static final List<String> CALLS = new ArrayList<>();

    @Service
    public static class Flusher {
        @Shutdown
        public void flush() {
            CALLS.add("flusher");
        }
    }

    @Service
    public static class Broken {
        @Shutdown
        public void explode() {
            CALLS.add("broken");
            throw new IllegalStateException("boom");
        }
    }

    @Service
    public static class Hidden {
        @Shutdown
        private void closeQuietly() {
            CALLS.add("hidden");
        }
    }

    @Service
    public static class Plain {
        public void notAHook() {
            CALLS.add("plain");
        }
    }

    private ShutdownHandler handler;

    @BeforeEach
    void setUp() {
        CALLS.clear();
        handler = new ShutdownHandler();
        IoCContainer container = new IoCContainer();
        container.registerClassHandler(new ServiceHandler());
        container.registerMethodHandler(handler);
        container.initialize(Set.of(Flusher.class, Broken.class, Hidden.class, Plain.class));
    }

    @Test
    void nothingRunsBeforeShutdown() {
        Assertions.assertTrue(CALLS.isEmpty(), "хуки не должны срабатывать при старте: " + CALLS);
    }

    @Test
    void everyMarkedMethodRunsOnShutdownIncludingPrivateOnes() {
        handler.runAll();

        Assertions.assertTrue(CALLS.contains("flusher"));
        Assertions.assertTrue(CALLS.contains("hidden"), "приватный метод тоже вызывается");
        Assertions.assertFalse(CALLS.contains("plain"), "метод без @Shutdown не вызывается");
    }

    @Test
    void aFailingHookDoesNotStopTheOthers() {
        handler.runAll();

        Assertions.assertTrue(CALLS.contains("broken"));
        Assertions.assertTrue(CALLS.contains("flusher"), "после упавшего хука остальные всё равно отработали");
        Assertions.assertTrue(CALLS.contains("hidden"));
        Assertions.assertEquals(3, CALLS.size());
    }
}
