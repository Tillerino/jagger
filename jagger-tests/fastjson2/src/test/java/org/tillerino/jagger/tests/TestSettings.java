package org.tillerino.jagger.tests;

public class TestSettings {
    public static final TestSettingsBase SETTINGS = TestSettingsBase.builder()
            .canReadIntArrayNatively(true)
            .canReadLongArrayNatively(true)
            .canReadStringArrayNatively(true)
            .canWriteBooleanArrayNatively(true)
            .canWriteShortArrayNatively(true)
            .canWriteIntArrayNatively(true)
            .canWriteLongArrayNatively(true)
            .build();
}
