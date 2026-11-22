package org.tillerino.jagger.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Stack;
import org.tillerino.jagger.api.JaggerWriter;

public class Jackson2JsonNodeWriterAdapter implements JaggerWriter<RuntimeException> {
    protected final JsonNodeFactory factory;

    protected Stack<JsonNode> node;

    protected JsonNode root;

    protected String pendingFieldName;

    public Jackson2JsonNodeWriterAdapter(JsonNodeFactory factory) {
        this.factory = factory;
    }

    public JsonNode getResult() {
        if (node != null && !node.isEmpty()) {
            throw new IllegalStateException("node not finished");
        }
        return root;
    }

    @Override
    public void startObject() throws RuntimeException {
        if (root == null) {
            node = new Stack<>();
            node.push(root = factory.objectNode());
        } else if (pendingFieldName != null) {
            node.push(node.peek().withObjectProperty(pendingFieldName));
            pendingFieldName = null;
        } else {
            node.push(((ArrayNode) node.peek()).addObject());
        }
    }

    @Override
    public void endObject() throws RuntimeException {
        node.pop();
    }

    @Override
    public void startArray() throws RuntimeException {
        if (root == null) {
            node = new Stack<>();
            node.push(root = factory.arrayNode());
        } else if (pendingFieldName != null) {
            node.push(node.peek().withArray(pendingFieldName));
            pendingFieldName = null;
        } else {
            node.push(((ArrayNode) node.peek()).addArray());
        }
    }

    @Override
    public void endArray() throws RuntimeException {
        node.pop();
    }

    @Override
    public void writeNull() throws RuntimeException {
        if (root == null) {
            root = factory.nullNode();
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).putNull(pendingFieldName);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).addNull();
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(boolean value) throws RuntimeException {
        if (root == null) {
            root = factory.booleanNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(byte value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(short value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(int value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(long value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(float value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(double value) throws RuntimeException {
        if (root == null) {
            root = factory.numberNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void write(String value) throws RuntimeException {
        if (root == null) {
            root = factory.textNode(value);
        } else if (pendingFieldName != null) {
            ((ObjectNode) node.peek()).put(pendingFieldName, value);
            pendingFieldName = null;
        } else if (node.peek().isArray()) {
            ((ArrayNode) node.peek()).add(value);
        } else {
            throw new IllegalStateException("stack not empty");
        }
    }

    @Override
    public void writeFieldName(String name) throws RuntimeException {
        pendingFieldName = name;
    }

    @Override
    public void startObjectField(String name) throws RuntimeException {
        node.push(node.peek().withObjectProperty(name));
    }

    @Override
    public void startArrayField(String name) throws RuntimeException {
        node.push(node.peek().withArray(name));
    }

    @Override
    public void writeNullField(String name) throws RuntimeException {
        ((ObjectNode) node.peek()).putNull(name);
    }

    @Override
    public void writeField(String name, boolean value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, byte value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, short value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, int value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, long value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, float value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, double value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }

    @Override
    public void writeField(String name, String value) throws RuntimeException {
        ((ObjectNode) node.peek()).put(name, value);
    }
}
