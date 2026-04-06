package org.tillerino.jagger.tests.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ScalarFieldsClass {
    public boolean bo;
    public byte by;
    public short s;
    public int i;
    public long l;
    public char c;
    public float f;
    public double d;
    public Boolean bbo;
    public Byte bby;
    public Short ss;
    public Integer ii;
    public Long ll;
    public Character cc;
    public Float ff;
    public Double dd;
    public String str;
    public AnEnum en;

    public static String staticField = "abc";

    public transient String transientField = "def";

    public ScalarFieldsClass() {}

    public ScalarFieldsClass(
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
            AnEnum en) {
        this.bo = bo;
        this.by = by;
        this.s = s;
        this.i = i;
        this.l = l;
        this.c = c;
        this.f = f;
        this.d = d;
        this.bbo = bbo;
        this.bby = bby;
        this.ss = ss;
        this.ii = ii;
        this.ll = ll;
        this.cc = cc;
        this.ff = ff;
        this.dd = dd;
        this.str = str;
        this.en = en;
    }
}
