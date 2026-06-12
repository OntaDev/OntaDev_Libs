// PPFS_Libs Plugin
// Авторские права (c) 2024 OntaDev
// Лицензия: MIT

package com.ontadev.libs.message;

import java.util.*;

@SuppressWarnings("unused")
public class Placeholders {
    private final Map<String, List<String>> placeholders;

    public Placeholders() {
        this.placeholders = new LinkedHashMap<>();
    }


    public Placeholders(Map<String, List<String>> placeholders) {
        if (placeholders == null) throw new IllegalArgumentException("placeholders map is null");
        this.placeholders = new LinkedHashMap<>();
        placeholders.forEach((k, v) -> this.placeholders.put(k, new ArrayList<>(v)));
    }


    public static Placeholders of(String key, String... values) {
        return new Placeholders().add(key, values);
    }

    public static Placeholders of() {
        return new Placeholders();
    }

    public static Placeholders of(Map<String, List<String>> placeholders) {
        return new Placeholders(placeholders);
    }

    public synchronized Placeholders add(String key, Object... values) {
        if (key == null || values == null) throw new IllegalArgumentException("key or values is null: " + key);
        placeholders.put(key, Arrays.stream(values).map(Object::toString).toList());
        return this;
    }

    public synchronized Placeholders add(String key, String... values) {
        if (key == null || values == null) throw new IllegalArgumentException("key or values is null: " + key);
        placeholders.put(key, Arrays.asList(values));
        return this;
    }

    public synchronized Placeholders add(String key, List<String> values) {
        if (key == null || values == null) throw new IllegalArgumentException("key or values is null: " + key);
        placeholders.put(key, new ArrayList<>(values));
        return this;
    }

    public synchronized Placeholders add(Placeholders other) {
        if (other == null) return this;
        other.placeholders.forEach((k, v) -> this.placeholders.merge(k, new ArrayList<>(v), (oldV, newV) -> {
            List<String> merged = new ArrayList<>(oldV);
            merged.addAll(newV);
            return merged;
        }));
        return this;
    }

    public synchronized List<String> apply(List<String> strings) {
        if (strings == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();

        for (String str : strings) {
            List<String> temp = apply(str);
            if (temp == null || temp.isEmpty()) continue;

            result.addAll(temp);
        }

        return result;
    }

    public synchronized List<String> apply(String message) {
        if (message == null || message.isEmpty()) return Collections.emptyList();

        List<String> results = new ArrayList<>();
        results.add(message);

        for (Map.Entry<String, List<String>> entry : placeholders.entrySet()) {
            String placeholderToken = "<" + entry.getKey() + ">";
            List<String> newResults = new ArrayList<>();
            for (String result : results) {
                if (result.contains(placeholderToken)) {
                    for (String replacement : entry.getValue()) {
                        newResults.add(result.replace(placeholderToken, replacement));
                    }
                } else {
                    newResults.add(result);
                }
            }
            results = newResults;
        }
        return results;
    }

    public Map<String, List<String>> asMap() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        placeholders.forEach((k, v) -> copy.put(k, Collections.unmodifiableList(v)));
        return Collections.unmodifiableMap(copy);
    }
}