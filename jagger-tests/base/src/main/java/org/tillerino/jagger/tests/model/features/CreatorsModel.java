package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.tillerino.jagger.tests.model.features.CreatorsModel.PolyInterface.PolyChildWithCreator;

public interface CreatorsModel {
    @EqualsAndHashCode
    @Getter
    class JsonCreatorConstructorCreatorClass<T> {
        private final T prop;
        private final String s;

        @JsonCreator
        public JsonCreatorConstructorCreatorClass(T notprop, String nots) {
            this.prop = notprop;
            this.s = nots;
        }
    }

    @EqualsAndHashCode
    @Getter
    class JsonCreatorConstructorCreatorSinglePropertyClass<T> {
        private final List<T> prop;

        @JsonCreator(mode = Mode.PROPERTIES)
        public JsonCreatorConstructorCreatorSinglePropertyClass(T notprop) {
            this.prop = List.of(notprop);
        }
    }

    @EqualsAndHashCode
    @Getter
    class JsonCreatorConstructorFactoryClass<T> {
        private final T prop;

        @JsonCreator
        public JsonCreatorConstructorFactoryClass(Map<String, T> props) {
            this.prop = props.get("notprop");
        }
    }

    @EqualsAndHashCode
    @Getter
    class JsonCreatorConstructorFactoryMultiplePropertiesClass<T> {
        private final T prop;
        private final Thread t;

        @JsonCreator(mode = Mode.DELEGATING)
        public JsonCreatorConstructorFactoryMultiplePropertiesClass(T notprop, Thread t) {
            this.prop = notprop;
            this.t = t;
        }
    }

    record JsonCreatorMethodCreatorRecord<T>(T prop, String s) {
        @JsonCreator
        public static <U> JsonCreatorMethodCreatorRecord<U> of(U notprop, String nots) {
            return new JsonCreatorMethodCreatorRecord<>(notprop, nots);
        }
    }

    record JsonCreatorMethodCreatorSinglePropertyRecord<T>(List<T> prop) {
        @JsonCreator(mode = Mode.PROPERTIES)
        public static <U> JsonCreatorMethodCreatorSinglePropertyRecord<U> of(U notprop) {
            return new JsonCreatorMethodCreatorSinglePropertyRecord<>(List.of(notprop));
        }
    }

    record JsonCreatorMethodFactoryRecord<T>(T prop) {
        @JsonCreator
        public static <U> JsonCreatorMethodFactoryRecord<U> of(Map<String, U> props) {
            return new JsonCreatorMethodFactoryRecord<>(props.get("notprop"));
        }
    }

    record JsonCreatorMethodFactoryMultiplePropertiesRecord<T>(T prop, Thread t) {
        @JsonCreator(mode = Mode.DELEGATING)
        public static <U> JsonCreatorMethodFactoryMultiplePropertiesRecord<U> of(U notprop, Thread t) {
            return new JsonCreatorMethodFactoryMultiplePropertiesRecord<>(notprop, t);
        }
    }

    record JsonValueRecord<T>(T prop) {
        @JsonValue
        public Map<String, T> toMap() {
            LinkedHashMap<String, T> map = new LinkedHashMap<>();
            map.put("notprop", prop);
            return map;
        }
    }

    /**
     * Edge case where creator was not being picked up on polymorphism when there was no dedicated prototype for the
     * child class.
     */
    @JsonTypeInfo(use = Id.MINIMAL_CLASS)
    @JsonSubTypes(@Type(PolyChildWithCreator.class))
    interface PolyInterface {
        record PolyChildWithCreator(int i) implements PolyInterface {
            @JsonCreator(mode = Mode.PROPERTIES)
            public static PolyChildWithCreator create(Object ignored) {
                return new PolyChildWithCreator(123);
            }
        }
    }

    /**
     * A bunch of classes which are special (enum/iterable/...) which is supposed to distract from {@link JsonValue} and
     * {@link JsonCreator}.
     */
    interface Priority {
        enum JsonValueEnum {
            VALUE1;

            @JsonValue
            public List<Integer> toList() {
                return List.of(1);
            }
        }

        class JsonValueIterable implements Iterable<Integer> {
            @JsonValue
            public String asString() {
                return "value";
            }

            @Override
            public Iterator<Integer> iterator() {
                return null;
            }
        }

        class JsonValueMap extends HashMap<String, Integer> {
            @JsonValue
            public String asString() {
                return "value";
            }
        }

        enum JsonCreatorMethodEnum {
            VALUE1;

            @JsonCreator
            public static JsonCreatorMethodEnum from(List<Integer> i) {
                return VALUE1;
            }
        }

        @EqualsAndHashCode(callSuper = false)
        class JsonCreatorMethodCollection extends ArrayList<String> {
            public int s;

            @JsonCreator
            public static JsonCreatorMethodCollection from(List<Integer> i) {
                JsonCreatorMethodCollection strings = new JsonCreatorMethodCollection();
                strings.s = i.size();
                return strings;
            }
        }

        @EqualsAndHashCode(callSuper = false)
        class JsonCreatorMethodMap extends HashMap<String, Integer> {
            public int s;

            @JsonCreator
            public static JsonCreatorMethodMap from(List<Integer> i) {
                JsonCreatorMethodMap strings = new JsonCreatorMethodMap();
                strings.s = i.size();
                return strings;
            }
        }

        enum JsonCreatorMethodMultipleParamsEnum {
            VALUE1;

            @JsonCreator
            public static JsonCreatorMethodMultipleParamsEnum from(List<Integer> i, String name, boolean flag) {
                return VALUE1;
            }
        }

        @EqualsAndHashCode(callSuper = false)
        class JsonCreatorMethodMultipleParamsCollection extends ArrayList<String> {
            public String name;
            public int count;

            @JsonCreator
            public static JsonCreatorMethodMultipleParamsCollection from(List<Integer> i, String name, boolean flag) {
                JsonCreatorMethodMultipleParamsCollection collection = new JsonCreatorMethodMultipleParamsCollection();
                collection.name = name;
                collection.count = i.size();
                return collection;
            }
        }

        @EqualsAndHashCode(callSuper = false)
        class JsonCreatorMethodMultipleParamsMap extends HashMap<String, Integer> {
            public String name;
            public int count;

            @JsonCreator
            public static JsonCreatorMethodMultipleParamsMap from(List<Integer> i, String name, boolean flag) {
                JsonCreatorMethodMultipleParamsMap map = new JsonCreatorMethodMultipleParamsMap();
                map.name = name;
                map.count = i.size();
                return map;
            }
        }
    }
}
