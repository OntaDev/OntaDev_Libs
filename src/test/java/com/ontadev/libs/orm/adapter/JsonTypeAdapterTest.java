package com.ontadev.libs.orm.adapter;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTypeAdapterTest {

    enum Plain {A}

    @SuppressWarnings("unused")
    enum WithBodies {
        A {
            @Override
            int value() {
                return 1;
            }
        };

        abstract int value();
    }

    private final JsonTypeAdapter adapter = new JsonTypeAdapter();
    private final ConfigRegistry config = new ConfigRegistry();

    @Test
    void plainEnumIsLeftToJdbi() {
        assertFalse(adapter.build(Plain.A.getClass(), Plain.A, config).isPresent());
    }

    @Test
    void enumConstantWithABodyIsAlsoLeftToJdbiInsteadOfBeingWrittenAsJson() {
        assertFalse(WithBodies.A.getClass().isEnum(), "test premise: runtime class of A is not itself an enum");

        assertFalse(adapter.build(WithBodies.A.getClass(), WithBodies.A, config).isPresent());
    }

    @Test
    void realJsonTypesStillGoThroughGson() {
        assertTrue(adapter.build(java.util.ArrayList.class, List.of("x"), config).isPresent());
    }
}
