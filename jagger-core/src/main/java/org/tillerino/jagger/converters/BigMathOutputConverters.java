package org.tillerino.jagger.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.tillerino.jagger.annotations.JsonOutputConverter;

/** Output converters for {@link BigDecimal} and {@link BigInteger}. */
public class BigMathOutputConverters {
    @JsonOutputConverter
    public static String bigDecimalToString(BigDecimal bigDecimal) {
        return bigDecimal.toPlainString();
    }

    @JsonOutputConverter
    public static String bigIntegerToString(BigInteger bigInteger) {
        return bigInteger.toString();
    }
}
