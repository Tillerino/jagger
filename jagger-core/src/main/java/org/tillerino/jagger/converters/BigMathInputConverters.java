package org.tillerino.jagger.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.tillerino.jagger.annotations.JsonInputConverter;

/** Input converters for {@link BigDecimal} and {@link BigInteger}. */
public class BigMathInputConverters {
    @JsonInputConverter
    public static BigDecimal stringToBigDecimal(String string) {
        return new BigDecimal(string);
    }

    @JsonInputConverter
    public static BigInteger stringToBigInteger(String string) {
        return new BigInteger(string);
    }
}
