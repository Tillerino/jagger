package org.tillerino.jagger.tests.model;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ScalarAccessorsClass {
    private boolean bo;
    private byte by;
    private short s;
    private int i;
    private long l;
    private char c;
    private float f;
    private double d;
    private Boolean bbo;
    private Byte bby;
    private Short ss;
    private Integer ii;
    private Long ll;
    private Character cc;
    private Float ff;
    private Double dd;
    private String str;
    private AnEnum en;

    public ScalarAccessorsClass() {}

    public ScalarAccessorsClass(
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

    public boolean isBo() {
        return bo;
    }

    public void setBo(boolean bo) {
        this.bo = bo;
    }

    public byte getBy() {
        return by;
    }

    public void setBy(byte by) {
        this.by = by;
    }

    public short getS() {
        return s;
    }

    public void setS(short s) {
        this.s = s;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public long getL() {
        return l;
    }

    public void setL(long l) {
        this.l = l;
    }

    public char getC() {
        return c;
    }

    public void setC(char c) {
        this.c = c;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public Boolean getBbo() {
        return bbo;
    }

    public void setBbo(Boolean bbo) {
        this.bbo = bbo;
    }

    public Byte getBby() {
        return bby;
    }

    public void setBby(Byte bby) {
        this.bby = bby;
    }

    public Short getSs() {
        return ss;
    }

    public void setSs(Short ss) {
        this.ss = ss;
    }

    public Integer getIi() {
        return ii;
    }

    public void setIi(Integer ii) {
        this.ii = ii;
    }

    public Long getLl() {
        return ll;
    }

    public void setLl(Long ll) {
        this.ll = ll;
    }

    public Character getCc() {
        return cc;
    }

    public void setCc(Character cc) {
        this.cc = cc;
    }

    public Float getFf() {
        return ff;
    }

    public void setFf(Float ff) {
        this.ff = ff;
    }

    public Double getDd() {
        return dd;
    }

    public void setDd(Double dd) {
        this.dd = dd;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public AnEnum getEn() {
        return en;
    }

    public void setEn(AnEnum en) {
        this.en = en;
    }
}
