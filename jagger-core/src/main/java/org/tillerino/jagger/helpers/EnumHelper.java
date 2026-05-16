package org.tillerino.jagger.helpers;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class EnumHelper {
    public static <T extends Enum<T>> Map<String, T> deserializationMap(
            Class<T> enumType, Function<T, String> keyMapper, Object[] explicitMappings) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T constant : enumType.getEnumConstants()) {
            int idx = findInPairs(explicitMappings, constant);
            String key = idx >= 0 ? explicitMappings[idx + 1].toString() : keyMapper.apply(constant);
            result.put(key, constant);
        }
        return Collections.unmodifiableMap(result);
    }

    public static <T extends Enum<T>> EnumMap<T, String> serializationMap(
            Class<T> enumType, Function<T, String> fallbackMapper, Object[] explicitMappings) {
        EnumMap<T, String> result = new EnumMap<>(enumType);
        for (int i = 0; i < explicitMappings.length; i += 2) {
            result.put(Enum.valueOf(enumType, explicitMappings[i].toString()), explicitMappings[i + 1].toString());
        }
        for (T constant : enumType.getEnumConstants()) {
            result.putIfAbsent(constant, fallbackMapper.apply(constant));
        }
        return result;
    }

    private static int findInPairs(Object[] pairs, Enum<?> constant) {
        String name = constant.name();
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i].toString().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
