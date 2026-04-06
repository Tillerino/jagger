package org.tillerino.jagger.tests;

import java.lang.reflect.Constructor;
import lombok.SneakyThrows;

public class SerdeUtil {
    @SneakyThrows
    public static <T> Class<? extends T> implClass(Class<T> cls) {
        return (Class<? extends T>) SerdeUtil.class.getClassLoader().loadClass(cls.getName() + "Impl");
    }

    @SneakyThrows
    public static <T> T impl(Class<T> cls) {
        Class<? extends T> implementation = SerdeUtil.implClass(cls);
        Constructor<? extends T> constructor = implementation.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
