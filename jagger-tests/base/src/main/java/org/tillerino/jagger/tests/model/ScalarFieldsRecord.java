package org.tillerino.jagger.tests.model;

public record ScalarFieldsRecord(
        boolean bo,
        byte by,
        short s,
        int i,
        long l,
        char c,
        float f,
        double d,
        Boolean bbo,
        Byte bby,
        Short ss,
        Integer ii,
        Long ll,
        Character cc,
        Float ff,
        Double dd,
        String str,
        AnEnum en) {}
