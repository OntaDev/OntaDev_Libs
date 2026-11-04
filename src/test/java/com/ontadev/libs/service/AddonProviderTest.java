package com.ontadev.libs.service;

import com.ontadev.libs.exception.AddonNotFoundException;
import com.ontadev.libs.ioc.PluginIoC;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddonProviderTest {
    @Test
    void reusesFoundApiAcrossContainersWithoutCallingSupplierAgain() {
        AddonProvider addons = new AddonProvider(mock(ServicesManager.class));
        AtomicInteger lookups = new AtomicInteger();
        Runnable api = () -> {};
        addons.registerAddon(() -> { lookups.incrementAndGet(); return api; }, Runnable.class, true);
        addons.registerAddon(() -> { lookups.incrementAndGet(); return null; }, Runnable.class, true);
        PluginIoC first = mock(PluginIoC.class);
        PluginIoC second = mock(PluginIoC.class);
        addons.registerIn(first);
        addons.registerIn(second);
        assertEquals(1, lookups.get());
        verify(first).registerInstance(Runnable.class, api);
        verify(second).registerInstance(Runnable.class, api);
        verify(second).registerInstance(AddonProvider.class, addons);
    }

    @Test
    void doesNotCacheMissingOptionalApi() {
        AddonProvider addons = new AddonProvider(mock(ServicesManager.class));
        addons.registerAddon(() -> null, String.class, false);
        PluginIoC first = mock(PluginIoC.class);
        addons.registerIn(first);
        verify(first, never()).registerInstance(eq(String.class), any());
        addons.registerAddon(() -> "ready", String.class, false);
        PluginIoC second = mock(PluginIoC.class);
        addons.registerIn(second);
        verify(second).registerInstance(String.class, "ready");
    }

    @Test
    void rejectsMissingRequiredApi() {
        AddonProvider addons = new AddonProvider(mock(ServicesManager.class));
        assertThrows(AddonNotFoundException.class,
                () -> addons.registerAddon(() -> null, String.class, true));
    }

    @Test
    @SuppressWarnings("unchecked")
    void looksUpBukkitServiceOnlyOnce() {
        ServicesManager services = mock(ServicesManager.class);
        RegisteredServiceProvider<Runnable> registration = mock(RegisteredServiceProvider.class);
        Runnable api = () -> {};
        when(services.getRegistration(Runnable.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(api);
        AddonProvider addons = new AddonProvider(services);
        addons.registerAddon(Runnable.class, "Test", true);
        addons.registerAddon(Runnable.class, "Test", true);
        verify(services, times(1)).getRegistration(Runnable.class);
        PluginIoC ioc = mock(PluginIoC.class);
        addons.registerIn(ioc);
        verify(ioc).registerInstance(Runnable.class, api);
    }

    @Test
    void reportsMissingServiceName() {
        AddonProvider addons = new AddonProvider(mock(ServicesManager.class));
        AddonNotFoundException error = assertThrows(AddonNotFoundException.class,
                () -> addons.registerAddon(Runnable.class, "TestAddon", true));
        assertEquals("TestAddon not found", error.getMessage());
    }
}
