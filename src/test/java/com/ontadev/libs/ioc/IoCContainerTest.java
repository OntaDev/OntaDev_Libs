// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.ioc.handlers.impl.ServiceHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class IoCContainerTest {

    @Service
    public interface Greeter {
        String greet();
    }

    public static class GreeterImpl implements Greeter {
        @Override
        public String greet() {
            return "hello";
        }
    }

    public static class SecondGreeterImpl implements Greeter {
        @Override
        public String greet() {
            return "hi";
        }
    }

    private IoCContainer newContainer() {
        IoCContainer container = new IoCContainer();
        container.registerClassHandler(new ServiceHandler());
        return container;
    }

    @Test
    void shouldAutoResolveSingleImplementationOfAnnotatedInterface() {
        IoCContainer container = newContainer();

        container.initialize(Set.of(Greeter.class, GreeterImpl.class));

        Greeter greeter = container.get(Greeter.class);

        Assertions.assertNotNull(greeter);
        Assertions.assertEquals(GreeterImpl.class, greeter.getClass());
        Assertions.assertEquals("hello", greeter.greet());
    }

    @Test
    void shouldNotResolveInterfaceWhenMultipleImplementationsFound() {
        IoCContainer container = newContainer();

        container.initialize(Set.of(Greeter.class, GreeterImpl.class, SecondGreeterImpl.class));

        Assertions.assertNull(container.get(Greeter.class));
    }

    @Test
    void shouldNotResolveInterfaceWhenNoImplementationFound() {
        IoCContainer container = newContainer();

        container.initialize(Set.of(Greeter.class));

        Assertions.assertNull(container.get(Greeter.class));
    }
}
