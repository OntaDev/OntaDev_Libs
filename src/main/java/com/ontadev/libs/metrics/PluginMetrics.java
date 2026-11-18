package com.ontadev.libs.metrics;

import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedBarChart;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Метрики одного плагина. Публичные методы не требуют классов bStats.
 * Источники данных вызываются bStats при сборе статистики в основном потоке Bukkit.
 * Учитывается глобальная настройка bStats/config.yml.
 */
@SuppressWarnings("UnusedReturnValue")
public final class PluginMetrics {
    /**
     * @return ID плагина на bStats; 0 означает, что ID не задан   */
    @Getter
    private final int serviceId;
    private Metrics metrics;

    /**
     * @param plugin владелец метрик
     * @param serviceId ID плагина на bStats; 0 отключает создание метрик
     * @throws IllegalArgumentException если ID отрицательный
     */
    public PluginMetrics(JavaPlugin plugin, int serviceId) {
        Objects.requireNonNull(plugin, "plugin");
        if (serviceId < 0) throw new IllegalArgumentException("bStats service ID must not be negative");
        this.serviceId = serviceId;
        if (serviceId > 0) metrics = new Metrics(plugin, serviceId);
    }

    /**
     * Добавляет круговую диаграмму с одной категорией на сервер.
     * @param chartId ID графика на bStats
     * @param data источник категории
     * @return этот экземпляр
     */
    public PluginMetrics addSimplePie(String chartId, Callable<String> data) {
        return add(new SimplePie(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет круговую диаграмму с несколькими категориями на сервер.
     * @param chartId ID графика на bStats
     * @param data источник значений по категориям
     * @return этот экземпляр
     */
    public PluginMetrics addAdvancedPie(String chartId, Callable<Map<String, Integer>> data) {
        return add(new AdvancedPie(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет круговую диаграмму с вложенными категориями.
     * @param chartId ID графика на bStats
     * @param data источник групп и значений вложенных категорий
     * @return этот экземпляр
     */
    public PluginMetrics addDrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> data) {
        return add(new DrilldownPie(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет линейный график с одним числовым значением.
     * @param chartId ID графика на bStats
     * @param data источник значения
     * @return этот экземпляр
     */
    public PluginMetrics addSingleLineChart(String chartId, Callable<Integer> data) {
        return add(new SingleLineChart(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет линейный график с несколькими рядами.
     * @param chartId ID графика на bStats
     * @param data источник значений по именам рядов
     * @return этот экземпляр
     */
    public PluginMetrics addMultiLineChart(String chartId, Callable<Map<String, Integer>> data) {
        return add(new MultiLineChart(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет столбчатую диаграмму с одним значением на категорию.
     * @param chartId ID графика на bStats
     * @param data источник значений по категориям
     * @return этот экземпляр
     */
    public PluginMetrics addSimpleBarChart(String chartId, Callable<Map<String, Integer>> data) {
        return add(new SimpleBarChart(chartId, Objects.requireNonNull(data, "data")));
    }

    /**
     * Добавляет столбчатую диаграмму с несколькими значениями на категорию.
     * @param chartId ID графика на bStats
     * @param data источник массивов значений по категориям
     * @return этот экземпляр
     */
    public PluginMetrics addAdvancedBarChart(String chartId, Callable<Map<String, int[]>> data) {
        return add(new AdvancedBarChart(chartId, Objects.requireNonNull(data, "data")));
    }

    /** Останавливает планировщик метрик. Повторный вызов ничего не делает. */
    public void shutdown() {
        if (metrics == null) return;
        metrics.shutdown();
        metrics = null;
    }

    private PluginMetrics add(CustomChart chart) {
        if (metrics != null) metrics.addCustomChart(chart);
        return this;
    }
}
