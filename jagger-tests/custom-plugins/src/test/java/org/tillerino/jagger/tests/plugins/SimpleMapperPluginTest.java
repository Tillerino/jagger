package org.tillerino.jagger.tests.plugins;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tillerino.jagger.tests.SerdeUtil;

class SimpleMapperPluginTest {
    @Data
    @RequiredArgsConstructor
    public static class SourceEntity {
        private final String name;
        private final int age;
        private final String email;
    }

    @Data
    @NoArgsConstructor
    public static class TargetDto {
        private String name;
        private int age;
        private String email;
    }

    public interface UserMapper {
        @SimpleMapperPlugin.Mapper
        TargetDto toDto(SourceEntity entity);
    }

    UserMapper mapper = SerdeUtil.impl(UserMapper.class);

    @Test
    void mapsFieldsByName() {
        SourceEntity source = new SourceEntity("Alice", 30, "alice@example.com");
        TargetDto result = mapper.toDto(source);
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("name", "Alice");
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("age", 30);
        Assertions.assertThat(result).hasFieldOrPropertyWithValue("email", "alice@example.com");
    }
}
