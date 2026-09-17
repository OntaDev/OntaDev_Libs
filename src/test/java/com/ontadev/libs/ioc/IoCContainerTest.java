// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.injection.Inject;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.ioc.handlers.impl.InjectFieldHandler;
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

    /*
     * Provider<T> — lazy load
     */

    public static class Dependency {
        static int constructedCount = 0;

        public Dependency() {
            constructedCount++;
        }
    }

    public static class WithProviderConstructorParam {
        final Provider<Dependency> provider;

        public WithProviderConstructorParam(Provider<Dependency> provider) {
            this.provider = provider;
        }
    }

    @Service
    public static class EagerA {
        final Provider<LazyB> lazyBProvider;

        public EagerA(Provider<LazyB> lazyBProvider) {
            this.lazyBProvider = lazyBProvider;
        }
    }

    @Service
    public static class LazyB {
        final EagerA eagerA;

        public LazyB(EagerA eagerA) {
            this.eagerA = eagerA;
        }
    }

    @Service
    public static class FieldInjectionHost {
        @Inject
        Provider<Dependency> provider;
    }

    @Test
    void shouldNotEagerlyCreateProviderTargetInConstructorParam() {
        IoCContainer container = newContainer();
        Dependency.constructedCount = 0;

        WithProviderConstructorParam host = container.create(WithProviderConstructorParam.class);

        Assertions.assertEquals(0, Dependency.constructedCount);
        Assertions.assertNull(container.getIfExists(Dependency.class));

        Dependency dependency = host.provider.get();

        Assertions.assertNotNull(dependency);
        Assertions.assertEquals(1, Dependency.constructedCount);
        Assertions.assertSame(dependency, container.getIfExists(Dependency.class));
    }

    @Test
    void shouldNotCreateFalseCyclicDependencyWhenOneSideIsLazy() {
        IoCContainer container = newContainer();

        // Без Provider<LazyB> у EagerA это была бы настоящая цикличная зависимость
        // (EagerA -> LazyB -> EagerA) и initialize() бросил бы IllegalStateException.
        Assertions.assertDoesNotThrow(() -> container.initialize(Set.of(EagerA.class, LazyB.class)));

        EagerA eagerA = container.get(EagerA.class);
        LazyB lazyB = container.get(LazyB.class);

        Assertions.assertNotNull(eagerA);
        Assertions.assertNotNull(lazyB);
        Assertions.assertSame(lazyB, eagerA.lazyBProvider.get());
        Assertions.assertSame(eagerA, lazyB.eagerA);
    }

    @Test
    void shouldLazilyResolveProviderFieldInjection() {
        IoCContainer container = newContainer();
        container.registerFieldHandler(new InjectFieldHandler());
        Dependency.constructedCount = 0;

        container.initialize(Set.of(FieldInjectionHost.class));

        FieldInjectionHost host = container.get(FieldInjectionHost.class);

        Assertions.assertNotNull(host.provider);
        Assertions.assertEquals(0, Dependency.constructedCount);

        Dependency dependency = host.provider.get();

        Assertions.assertEquals(1, Dependency.constructedCount);
        Assertions.assertSame(dependency, container.getIfExists(Dependency.class));
    }
}
