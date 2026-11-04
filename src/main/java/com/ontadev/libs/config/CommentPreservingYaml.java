package com.ontadev.libs.config;

import com.google.gson.annotations.SerializedName;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class CommentPreservingYaml {
    private final Yaml yaml;

    CommentPreservingYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);

        this.yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions),
                dumperOptions, loaderOptions);
    }

    void save(Path file, Object value) throws IOException {
        save(file, value, null);
    }

    void save(Path file, Object value, Object commentSource) throws IOException {
        Node updated = yaml.represent(value);
        if (commentSource != null) addComments(updated, commentSource);
        if (Files.exists(file) && Files.size(file) > 0) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Node existing = yaml.compose(reader);
                if (existing != null) updated = merge(existing, updated);
            }
        }

        Path parent = file.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Cannot determine parent directory for " + file);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                yaml.serialize(updated, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void addComments(Node node, Object source) throws IOException {
        if (source == null) return;
        if (node instanceof SequenceNode && source instanceof Iterable<?>) {
            java.util.Iterator<?> values = ((Iterable<?>) source).iterator();
            for (Node item : ((SequenceNode) node).getValue()) {
                if (!values.hasNext()) break;
                addComments(item, values.next());
            }
            return;
        }
        if (!(node instanceof MappingNode)) return;
        MappingNode mapping = (MappingNode) node;
        if (source instanceof Map<?, ?>) {
            for (NodeTuple tuple : mapping.getValue()) {
                addComments(tuple.getValueNode(), ((Map<?, ?>) source).get(key(tuple.getKeyNode())));
            }
            return;
        }
        for (Class<?> type = source.getClass(); type != null && type != Object.class;
             type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())) continue;
                SerializedName name = field.getAnnotation(SerializedName.class);
                String yamlName = name == null ? field.getName() : name.value();
                int index = findKey(mapping.getValue(), yamlName);
                if (index < 0) continue;
                NodeTuple tuple = mapping.getValue().get(index);
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null && !comment.value().isEmpty()) {
                    List<CommentLine> lines = new ArrayList<>();
                    for (String line : comment.value().split("\\R", -1)) {
                        lines.add(new CommentLine(null, null, " " + line, CommentType.BLOCK));
                    }
                    tuple.getKeyNode().setBlockComments(lines);
                }
                try {
                    field.setAccessible(true);
                    addComments(tuple.getValueNode(), field.get(source));
                } catch (IllegalAccessException e) {
                    throw new IOException("Cannot read config field " + field.getName(), e);
                }
            }
        }
    }

    private Node merge(Node existing, Node updated) {
        if (existing instanceof MappingNode && updated instanceof MappingNode)
            return mergeMapping((MappingNode) existing, (MappingNode) updated);
        if (existing instanceof SequenceNode && updated instanceof SequenceNode)
            return mergeSequence((SequenceNode) existing, (SequenceNode) updated);
        copyComments(existing, updated);
        return updated;
    }

    private Node mergeMapping(MappingNode existing, MappingNode updated) {
        List<NodeTuple> result = new ArrayList<>(existing.getValue());
        for (NodeTuple changed : updated.getValue()) {
            int index = findKey(result, key(changed.getKeyNode()));
            if (index < 0) {
                result.add(changed);
                continue;
            }
            NodeTuple old = result.get(index);
            Node mergedValue = merge(old.getValueNode(), changed.getValueNode());
            result.set(index, new NodeTuple(old.getKeyNode(), mergedValue));
        }
        existing.setValue(result);
        return existing;
    }

    private Node mergeSequence(SequenceNode existing, SequenceNode updated) {
        List<Node> result = new ArrayList<>();
        for (int index = 0; index < updated.getValue().size(); index++) {
            Node changed = updated.getValue().get(index);
            result.add(index < existing.getValue().size()
                    ? merge(existing.getValue().get(index), changed) : changed);
        }
        existing.getValue().clear();
        existing.getValue().addAll(result);
        return existing;
    }

    private int findKey(List<NodeTuple> tuples, String key) {
        for (int index = 0; index < tuples.size(); index++)
            if (java.util.Objects.equals(key(tuples.get(index).getKeyNode()), key)) return index;
        return -1;
    }

    private String key(Node node) {
        return node instanceof ScalarNode ? ((ScalarNode) node).getValue() : null;
    }

    private void copyComments(Node source, Node target) {
        target.setBlockComments(source.getBlockComments());
        target.setInLineComments(source.getInLineComments());
        target.setEndComments(source.getEndComments());
    }
}
