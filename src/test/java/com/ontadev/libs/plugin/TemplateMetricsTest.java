package com.ontadev.libs.plugin;

import com.ontadev.libs.ioc.PluginIoC;
import com.ontadev.libs.metrics.PluginMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateMetricsTest {
    @Test
    void registersMetricsBeforeComponentsAndAddsChartsAfterInitialization() throws Exception {
        OntaDev_Template plugin = mock(OntaDev_Template.class);
        PluginIoC ioc = mock(PluginIoC.class);
        setField(plugin, "pluginIoC", ioc);
        when(plugin.getBStatsId()).thenReturn(12345);
        doCallRealMethod().when(plugin).onEnable();
        AtomicBoolean initialized = new AtomicBoolean();
        doAnswer(call -> {
            verify(ioc).registerInstance(eq(PluginMetrics.class), any(PluginMetrics.class));
            initialized.set(true);
            return null;
        }).when(plugin).initializeContainer(ioc);
        doAnswer(call -> {
            assertTrue(initialized.get());
            return null;
        }).when(plugin).registerMetrics(any(PluginMetrics.class), eq(ioc));
        try (MockedConstruction<PluginMetrics> created = mockConstruction(PluginMetrics.class, (metrics, context) -> {
            assertFalse(initialized.get());
            assertSame(plugin, context.arguments().get(0));
            assertEquals(12345, context.arguments().get(1));
        })) {
            plugin.onEnable();
            assertEquals(1, created.constructed().size());
            PluginMetrics metrics = created.constructed().get(0);
            var order = inOrder(ioc, plugin);
            order.verify(ioc).registerInstance(PluginMetrics.class, metrics);
            order.verify(plugin).initializeContainer(ioc);
            order.verify(plugin).registerMetrics(metrics, ioc);
            order.verify(ioc).onEnable();
            order.verify(plugin).onPluginEnable(ioc);
        }
    }

    @Test
    void stopsCreatedMetricsOnDisableAfterContainerInitializationFails() throws Exception {
        OntaDev_Template plugin = mock(OntaDev_Template.class);
        PluginIoC ioc = mock(PluginIoC.class);
        setField(plugin, "pluginIoC", ioc);
        when(plugin.getBStatsId()).thenReturn(12345);
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).onDisable();
        doThrow(new IllegalStateException("startup failed")).when(plugin).initializeContainer(ioc);
        try (MockedConstruction<PluginMetrics> created = mockConstruction(PluginMetrics.class)) {
            assertThrows(IllegalStateException.class, plugin::onEnable);
            assertEquals(1, created.constructed().size());
            verify(plugin, never()).registerMetrics(nullable(PluginMetrics.class), eq(ioc));
            plugin.onDisable();
            verify(created.constructed().get(0)).shutdown();
        }
    }

    @Test
    void nonPositiveIdSkipsMetricsRegistrationAndAllowsShutdown() throws Exception {
        for (int id : new int[]{0, -1}) {
            OntaDev_Template plugin = mock(OntaDev_Template.class);
            PluginIoC ioc = mock(PluginIoC.class);
            setField(plugin, "pluginIoC", ioc);
            when(plugin.getBStatsId()).thenReturn(id);
            doCallRealMethod().when(plugin).onEnable();
            doCallRealMethod().when(plugin).onDisable();
            try (MockedConstruction<PluginMetrics> created = mockConstruction(PluginMetrics.class)) {
                plugin.onEnable();
                plugin.onDisable();
                assertTrue(created.constructed().isEmpty());
                verify(ioc, never()).registerInstance(eq(PluginMetrics.class), nullable(PluginMetrics.class));
                verify(plugin, never()).registerMetrics(nullable(PluginMetrics.class), eq(ioc));
                verify(ioc).shutdownPlugin();
            }
        }
    }

    @Test
    void stopsMetricsEvenIfShutdownHookFails() throws Exception {
        OntaDev_Template plugin = mock(OntaDev_Template.class);
        PluginIoC ioc = mock(PluginIoC.class);
        PluginMetrics metrics = mock(PluginMetrics.class);
        setField(plugin, "pluginIoC", ioc);
        setField(plugin, "metrics", metrics);
        doThrow(new IllegalStateException("hook failed")).when(ioc).shutdownPlugin();
        doCallRealMethod().when(plugin).onDisable();
        assertThrows(IllegalStateException.class, plugin::onDisable);
        assertThrows(IllegalStateException.class, plugin::onDisable);
        verify(metrics, times(1)).shutdown();
    }

    @Test
    void defaultIdDisablesMetricsAndLibraryUsesAssignedId() {
        OntaDev_Template plugin = mock(OntaDev_Template.class);
        when(plugin.getBStatsId()).thenCallRealMethod();
        assertEquals(0, plugin.getBStatsId());
        OntaDev_Libs library = mock(OntaDev_Libs.class);
        when(library.getBStatsId()).thenCallRealMethod();
        assertEquals(35438, library.getBStatsId());
    }

    private void setField(OntaDev_Template plugin, String name, Object value) throws Exception {
        Field field = OntaDev_Template.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
}
