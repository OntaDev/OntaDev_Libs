package com.ontadev.libs.metrics;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginMetricsTest {
    @Test
    void createsSeparateMetricsForEachPlugin() {
        JavaPlugin first = mock(JavaPlugin.class);
        JavaPlugin second = mock(JavaPlugin.class);
        try (MockedConstruction<Metrics> created = mockConstruction(Metrics.class, (metrics, context) -> {
            assertSame(context.getCount() == 1 ? first : second, context.arguments().get(0));
            assertEquals(context.getCount() == 1 ? 35438 : 12345, context.arguments().get(1));
        })) {
            assertEquals(35438, new PluginMetrics(first, 35438).getServiceId());
            new PluginMetrics(second, 12345);
            assertEquals(2, created.constructed().size());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void zeroIdDoesNotCreateMetricsOrCollectData() {
        Callable<String> data = mock(Callable.class);
        try (MockedConstruction<Metrics> created = mockConstruction(Metrics.class)) {
            PluginMetrics metrics = new PluginMetrics(mock(JavaPlugin.class), 0);
            metrics.addSimplePie("mode", data);
            metrics.shutdown();
            assertTrue(created.constructed().isEmpty());
            verifyNoInteractions(data);
        }
    }

    @Test
    void rejectsNegativeId() {
        assertThrows(IllegalArgumentException.class,
                () -> new PluginMetrics(mock(JavaPlugin.class), -1));
    }

    @Test
    void registersAllChartTypesAndCollectsTheirValuesLazily() {
        try (MockedConstruction<Metrics> created = mockConstruction(Metrics.class)) {
            PluginMetrics metrics = new PluginMetrics(mock(JavaPlugin.class), 35438);
            metrics.addSimplePie("mode", () -> "survival")
                    .addAdvancedPie("features", () -> Map.of("quests", 2))
                    .addDrilldownPie("storage", () -> Map.of("sql", Map.of("h2", 1)))
                    .addSingleLineChart("quests", () -> 7)
                    .addMultiLineChart("tasks", () -> Map.of("active", 3))
                    .addSimpleBarChart("levels", () -> Map.of("high", 4))
                    .addAdvancedBarChart("groups", () -> Map.of("default", new int[]{1, 2}));
            ArgumentCaptor<CustomChart> charts = ArgumentCaptor.forClass(CustomChart.class);
            verify(created.constructed().get(0), times(7)).addCustomChart(charts.capture());
            String[] expected = {"survival", "quests", "h2", "7", "active", "high", "default"};
            for (int i = 0; i < expected.length; i++) {
                String json = charts.getAllValues().get(i).getRequestJsonObject(Assertions::fail, true).toString();
                assertTrue(json.contains(expected[i]), json);
            }
        }
    }

    @Test
    void shutdownStopsOnlyOwnMetricsOnceAndDiscardsFurtherCharts() {
        try (MockedConstruction<Metrics> created = mockConstruction(Metrics.class)) {
            PluginMetrics first = new PluginMetrics(mock(JavaPlugin.class), 35438);
            new PluginMetrics(mock(JavaPlugin.class), 12345);
            first.shutdown();
            first.shutdown();
            first.addSingleLineChart("ignored", () -> fail("Stopped metrics must not collect data"));
            verify(created.constructed().get(0), times(1)).shutdown();
            verifyNoMoreInteractions(created.constructed().get(0));
            verifyNoInteractions(created.constructed().get(1));
        }
    }
}
