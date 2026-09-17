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
     * @Inject — выбор конструктора и полевая инъекция (без Provider<T>)
     */

    public static class PlainDependency {
        String value() {
            return "plain";
        }
    }

    @Service
    public static class PlainFieldInjectionHost {
        @Inject
        PlainDependency dependency;
    }

    @SuppressWarnings("unused")
    public static class MultiConstructor {
        final String source;
        final PlainDependency dependency;

        public MultiConstructor() {
            this.source = "no-arg";
            this.dependency = null;
        }

        @Inject
        public MultiConstructor(PlainDependency dependency) {
            this.source = "annotated";
            this.dependency = dependency;
        }
    }

    @Test
    void shouldInjectPlainFieldDependency() {
        IoCContainer container = newContainer();
        container.registerFieldHandler(new InjectFieldHandler());

        container.initialize(Set.of(PlainFieldInjectionHost.class));

        PlainFieldInjectionHost host = container.get(PlainFieldInjectionHost.class);

        Assertions.assertNotNull(host.dependency);
        Assertions.assertEquals("plain", host.dependency.value());
    }

    @Test
    void shouldUseInjectAnnotatedConstructorWhenMultiplePresent() {
        IoCContainer container = newContainer();

        MultiConstructor instance = container.create(MultiConstructor.class);

        Assertions.assertEquals("annotated", instance.source);
        Assertions.assertNotNull(instance.dependency);
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
    void shouldConstructProviderTargetOnlyOnceAcrossMultipleGetCalls() {
        IoCContainer container = newContainer();
        Dependency.constructedCount = 0;

        WithProviderConstructorParam host = container.create(WithProviderConstructorParam.class);

        Assertions.assertEquals(0, Dependency.constructedCount,
                "Provider не должен создавать X до первого .get()");

        Dependency first = host.provider.get();
        Dependency second = host.provider.get();

        Assertions.assertEquals(1, Dependency.constructedCount,
                "повторный .get() не должен пересоздавать X");
        Assertions.assertSame(first, second);
    }

    @Test
    void shouldNotCreateFalseCyclicDependencyWhenOneSideIsLazy() {
        IoCContainer container = newContainer();

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
