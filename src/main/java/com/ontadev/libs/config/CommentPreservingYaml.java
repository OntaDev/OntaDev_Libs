package com.ontadev.libs.config;

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
        Node updated = yaml.represent(value);
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
