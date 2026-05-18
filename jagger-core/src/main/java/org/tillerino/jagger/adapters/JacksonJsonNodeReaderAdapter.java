package org.tillerino.jagger.adapters;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.tillerino.jagger.api.JaggerReader;

/**
 * Adapter around {@link JsonNode}. Usually, you could get away with wrapping
 * {@link com.fasterxml.jackson.core.TreeNode#traverse()} in {@link JacksonJsonParserAdapter}. However, there is one use
 * case where this doesn't suffice.
 *
 * <p>The only real use case for deserializing from {@link JsonNode} over {@link com.fasterxml.jackson.core.JsonParser}
 * is when the discriminator is not necessarily the first field in the Object. This can happen if the JSON storage does
 * not preserve field order.
 */
public class JacksonJsonNodeReaderAdapter implements JaggerReader<RuntimeException> {
    protected final Stack<State> node = new Stack<>();

    public JacksonJsonNodeReaderAdapter(JsonNode node) throws RuntimeException {
        this.node.push(new NodeState(node));
    }

    @Override
    public boolean isObjectStart(Advance advance) throws RuntimeException {
        if (currentToken() != JsonToken.START_OBJECT) {
            return false;
        }
        State current = node.peek();
        JsonNode jsonNode = ((NodeState) current).node;
        if (advance == Advance.CONSUME) {
            MemberIterator memberIterator = new MemberIterator(jsonNode.fields(), (NodeState) current);
            node.push(memberIterator);
            Member next = memberIterator.next();
            if (next != null) {
                node.push(next);
            }
        }
        return true;
    }

    @Override
    public boolean isObjectEnd(Advance advance) throws RuntimeException {
        if (currentToken() != JsonToken.END_OBJECT) {
            return false;
        }
        advance(advance);
        return true;
    }

    @Override
    public boolean isArrayStart(Advance advance) throws RuntimeException {
        if (currentToken() != JsonToken.START_ARRAY) {
            return false;
        }
        State current = node.peek();
        JsonNode jsonNode = ((NodeState) current).node;
        if (advance == Advance.CONSUME) {
            ArrayIterator arrayIterator = new ArrayIterator(jsonNode.elements());
            node.push(arrayIterator);
            if (arrayIterator.arrayIterator.hasNext()) {
                node.push(new NodeState(arrayIterator.arrayIterator.next()));
            }
        }
        return true;
    }

    @Override
    public boolean isArrayEnd(Advance advance) throws RuntimeException {
        if (currentToken() != JsonToken.END_ARRAY) {
            return false;
        }
        advance(advance);
        return true;
    }

    @Override
    public boolean isNull(Advance advance) throws RuntimeException {
        if (currentToken() != JsonToken.VALUE_NULL) {
            return false;
        }
        advance(advance);
        return true;
    }

    @Override
    public boolean isBoolean() throws RuntimeException {
        return currentToken().isBoolean();
    }

    @Override
    public boolean isNumber() throws RuntimeException {
        return currentToken().isNumeric();
    }

    @Override
    public boolean isText() throws RuntimeException {
        return currentToken() == JsonToken.VALUE_STRING;
    }

    @Override
    public boolean isFieldName() throws RuntimeException {
        return currentToken() == JsonToken.FIELD_NAME;
    }

    @Override
    public boolean getBoolean(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            boolean b = ((NodeState) current).node.asBoolean();
            advance(advance);
            return b;
        }
        throw unexpectedToken("boolean");
    }

    @Override
    public byte getByte(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            byte b = ((NodeState) current).node.numberValue().byteValue();
            advance(advance);
            return b;
        }
        throw unexpectedToken("number");
    }

    @Override
    public short getShort(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            short s = ((NodeState) current).node.numberValue().shortValue();
            advance(advance);
            return s;
        }
        throw unexpectedToken("number");
    }

    @Override
    public int getInt(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            int i = ((NodeState) current).node.asInt();
            advance(advance);
            return i;
        }
        throw unexpectedToken("number");
    }

    @Override
    public long getLong(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            long l = ((NodeState) current).node.asLong();
            advance(advance);
            return l;
        }
        throw unexpectedToken("number");
    }

    @Override
    public float getFloat(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            float f = ((NodeState) current).node.numberValue().floatValue();
            advance(advance);
            return f;
        }
        throw unexpectedToken("number");
    }

    @Override
    public double getDouble(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            double d = ((NodeState) current).node.asDouble();
            advance(advance);
            return d;
        }
        throw unexpectedToken("number");
    }

    @Override
    public String getText(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof NodeState) {
            String text = ((NodeState) current).node.asText();
            advance(advance);
            return text;
        }
        throw unexpectedToken("text");
    }

    @Override
    public String getFieldName(Advance advance) throws RuntimeException {
        State current = node.peek();
        if (current instanceof Member) {
            String fieldName = ((Member) current).name;
            if (advance == Advance.CONSUME) {
                node.push(new NodeState(((Member) current).value));
            }
            return fieldName;
        }
        throw unexpectedToken("field name");
    }

    @Override
    public String getDiscriminator(String expectedName, boolean visible) throws RuntimeException {
        State current = node.peek();
        if (current instanceof Member) {
            NodeState parent = ((Member) current).parent;
            ObjectNode parentNode = (ObjectNode) parent.node;
            if (!parentNode.has(expectedName)) {
                throw new RuntimeException("Expected discriminator " + expectedName + " not found at TODO LOCATION");
            }
            if (!visible) {
                if (parent.hideField != null) {
                    throw new IllegalStateException();
                }
                if (getFieldName(Advance.KEEP).equals(expectedName)) {
                    getFieldName(Advance.CONSUME);
                    return getText(Advance.CONSUME);
                }
                parent.hideField = expectedName;
            }
            return parentNode.get(expectedName).asText();
        }
        throw unexpectedToken("field name");
    }

    @Override
    public void skipChildren(Advance advance) throws RuntimeException {
        if (isArrayStart(Advance.KEEP)) {
            node.push(new ArrayIterator(Collections.emptyIterator()));
        } else if (isObjectEnd(Advance.KEEP)) {
            node.push(new MemberIterator(Collections.emptyIterator(), null));
        }
        advance(advance);
    }

    @Override
    public RuntimeException unexpectedToken(String expectedToken) {
        return new RuntimeException("Expected " + expectedToken + " but got " + currentToken() + " at TODO LOCATION");
    }

    @Override
    public RuntimeException unexpectedValue(String message) {
        return new RuntimeException(message);
    }

    @Override
    public RuntimeException unrecognizedProperty(String propertyName) {
        return new RuntimeException("Unrecognized field \"" + propertyName + "\"" + " at TODO LOCATION");
    }

    protected JsonToken currentToken() {
        if (node.isEmpty()) {
            throw new RuntimeException("Unexpected end of input");
        }
        return node.peek().asToken();
    }

    protected void advance(Advance advance) {
        if (advance == Advance.KEEP) {
            return;
        }
        if (node.peek() instanceof ArrayIterator || node.peek() instanceof MemberIterator) {
            node.pop();
        }
        node.pop();
        if (!node.isEmpty() && node.peek() instanceof Member) {
            node.pop();
        }
        if (node.isEmpty()) {
            return;
        }
        State parent = node.peek();
        if (parent instanceof MemberIterator) {
            Member next = ((MemberIterator) parent).next();
            if (next != null) {
                node.push(next);
            }
        } else if (parent instanceof ArrayIterator) {
            if (((ArrayIterator) parent).arrayIterator.hasNext()) {
                node.push(new NodeState(((ArrayIterator) parent).arrayIterator.next()));
            }
        }
        // otherwise we are at the end of the array or object
    }

    protected interface State {
        JsonToken asToken();
    }

    /** If this is the top state, we are at a value or at object/array start. */
    protected static class NodeState implements State {
        protected JsonNode node;
        protected String hideField;

        protected NodeState(JsonNode node) {
            this.node = node;
        }

        @Override
        public JsonToken asToken() {
            if (node.isObject()) {
                return JsonToken.START_OBJECT;
            }
            if (node.isArray()) {
                return JsonToken.START_ARRAY;
            }
            if (node.isNull()) {
                return JsonToken.VALUE_NULL;
            }
            if (node.isBoolean()) {
                return JsonToken.VALUE_TRUE;
            }
            if (node.isNumber()) {
                return JsonToken.VALUE_NUMBER_INT;
            }
            if (node.isTextual()) {
                return JsonToken.VALUE_STRING;
            }
            throw new IllegalStateException();
        }
    }

    /** If this is the top state, we are at object end. */
    protected static class MemberIterator implements State {
        protected Iterator<Map.Entry<String, JsonNode>> memberIterator;
        protected NodeState parent;

        protected MemberIterator(Iterator<Map.Entry<String, JsonNode>> memberIterator, NodeState parent) {
            this.memberIterator = memberIterator;
            this.parent = parent;
        }

        /** @return can be null if there are no more fields */
        protected Member next() {
            while (memberIterator.hasNext()) {
                Map.Entry<String, JsonNode> next = memberIterator.next();
                if (next.getKey().equals(parent.hideField)) {
                    continue;
                }
                return new Member(next.getKey(), next.getValue(), parent);
            }
            return null;
        }

        @Override
        public JsonToken asToken() {
            return JsonToken.END_OBJECT;
        }
    }

    /** If this is the top state, we are at field name. */
    protected static class Member implements State {
        protected String name;
        protected JsonNode value;
        protected NodeState parent;

        protected Member(String name, JsonNode value, NodeState parent) {
            this.name = name;
            this.value = value;
            this.parent = parent;
        }

        @Override
        public JsonToken asToken() {
            return JsonToken.FIELD_NAME;
        }
    }

    /** If this is the top state, we are at array end. */
    protected static class ArrayIterator implements State {
        protected Iterator<JsonNode> arrayIterator;

        protected ArrayIterator(Iterator<JsonNode> elements) {
            this.arrayIterator = elements;
        }

        @Override
        public JsonToken asToken() {
            return JsonToken.END_ARRAY;
        }
    }
}
