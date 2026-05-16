package org.tillerino.jagger.tests.plugins;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.annotations.JsonConfig;
import org.tillerino.jagger.annotations.JsonConfig.ImplementationMode;
import org.tillerino.jagger.annotations.JsonTemplate;
import org.tillerino.jagger.annotations.JsonTemplate.TypeArray;
import org.tillerino.jagger.tests.SerdeUtil;
import org.tillerino.jagger.tests.plugins.SimpleMapperPlugin.Mapper;
import org.tillerino.jagger.tests.plugins.SimpleMapperPluginTest.SourceEntity.SourceAddress;
import org.tillerino.jagger.tests.plugins.SimpleMapperPluginTest.TargetDto.AddressDto;

class SimpleMapperPluginTest {
    @Data
    @RequiredArgsConstructor
    public static class SourceEntity {
        private final String name;
        private final int age;
        private final String email;
        private final SourceAddress address;

        @Data
        @RequiredArgsConstructor
        public static class SourceAddress {
            private final String street;
            private final int number;
        }
    }

    @Data
    public static class TargetDto {
        private String name;
        private int age;
        private String email;
        private AddressDto address;

        @Data
        public static class AddressDto {
            private String street;
            private int number;
        }
    }

    @Nested
    class RegularUsage {
        public interface EntityMapper {
            @Mapper
            TargetDto toDto(SourceEntity entity);

            @Mapper
            AddressDto toDto(SourceAddress address);
        }

        EntityMapper mapper = SerdeUtil.impl(EntityMapper.class);

        @Test
        void mapsFieldsByName() {
            SourceEntity source = new SourceEntity("Alice", 30, "alice@example.com", new SourceAddress("main", 5));
            TargetDto result = mapper.toDto(source);
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("name", "Alice");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("age", 30);
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("email", "alice@example.com");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("address.street", "main");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("address.number", 5);
        }
    }

    @Nested
    class TemplatedUsage {
        @JsonConfig(implement = ImplementationMode.DO_NOT_IMPLEMENT)
        interface MapperTemplate<S, T> {
            @Mapper
            T map(S s);
        }

        @JsonTemplate(
                templates = {MapperTemplate.class},
                typeArrays = {
                    @TypeArray({TargetDto.class, SourceEntity.class}),
                    @TypeArray({AddressDto.class, SourceAddress.class}),
                })
        interface EntityMapper {}

        // We need to use the impl here, since the interface has no methods.
        SimpleMapperPluginTest$TemplatedUsage$EntityMapperImpl mapper =
                new SimpleMapperPluginTest$TemplatedUsage$EntityMapperImpl();

        @Test
        void mapsFieldsByName() {
            SourceEntity source = new SourceEntity("Alice", 30, "alice@example.com", new SourceAddress("main", 5));
            TargetDto result = mapper.mapSourceEntityToTargetDto(source);
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("name", "Alice");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("age", 30);
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("email", "alice@example.com");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("address.street", "main");
            Assertions.assertThat(result).hasFieldOrPropertyWithValue("address.number", 5);
        }
    }
}
