package org.tillerino.jagger.tests.model;

import java.util.ArrayList;
import org.tillerino.jagger.tests.TestSettingsBase;
import org.tillerino.jagger.tests.function.Zip;

public record ReferenceArrayFieldsRecord(
        Boolean[] boxedBooleanArray,
        Byte[] boxedByteArray,
        Character[] boxedCharArray,
        Short[] boxedShortArray,
        Integer[] boxedIntArray,
        Long[] boxedLongArray,
        Float[] boxedFloatArray,
        Double[] boxedDoubleArray,
        String[] stringArray,
        AnEnum[] enumArray) {

    public static ArrayList<ReferenceArrayFieldsRecord> instances(TestSettingsBase settings) {
        ArrayList<ReferenceArrayFieldsRecord> instances = new ArrayList<>();
        instances.add(null);
        instances.addAll(Zip.instantiate10(
                settings.javaData().BOXED_BOOLEAN_ARRAYS,
                settings.javaData().BOXED_BYTE_ARRAYS,
                settings.javaData().BOXED_CHAR_ARRAYS,
                settings.javaData().BOXED_SHORT_ARRAYS,
                settings.javaData().BOXED_INT_ARRAYS,
                settings.javaData().BOXED_LONG_ARRAYS,
                settings.javaData().boxedFloatArrays,
                settings.javaData().BOXED_DOUBLE_ARRAYS,
                settings.javaData().STRING_ARRAYS,
                settings.javaData().ENUM_ARRAYS,
                ReferenceArrayFieldsRecord::new));
        return instances;
    }
}
