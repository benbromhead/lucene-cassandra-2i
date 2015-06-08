/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.schema.mapping;

import com.google.common.base.Objects;
import org.apache.cassandra.db.marshal.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.math.BigDecimal;

/**
 * A {@link ColumnMapper} to map {@link java.math.BigDecimal} values. A max number of digits for the integer a decimal parts must
 * be specified.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class ColumnMapperBigDecimal extends ColumnMapperSingle<String> {

    /** The default max number of digits for the integer part. */
    public static final int DEFAULT_INTEGER_DIGITS = 32;

    /** The default max number of digits for the decimal part. */
    public static final int DEFAULT_DECIMAL_DIGITS = 32;

    /** The max number of digits for the integer part. */
    private final int integerDigits;

    /** The max number of digits for the decimal part. */
    private final int decimalDigits;

    private final BigDecimal complement;

    /**
     * Builds a new {@link ColumnMapperBigDecimal} using the specified max number of digits for the integer and decimal
     * parts.
     *
     * @param integerDigits The max number of digits for the integer part. If {@code null}, the {@link
     *                      #DEFAULT_INTEGER_DIGITS} will be used.
     * @param decimalDigits The max number of digits for the decimal part. If {@code null}, the {@link
     *                      #DEFAULT_DECIMAL_DIGITS} will be used.
     */
    @JsonCreator
    public ColumnMapperBigDecimal(@JsonProperty("integer_digits") Integer integerDigits,
                                  @JsonProperty("decimal_digits") Integer decimalDigits) {
        super(new AbstractType<?>[]{AsciiType.instance,
                                    UTF8Type.instance,
                                    Int32Type.instance,
                                    LongType.instance,
                                    IntegerType.instance,
                                    FloatType.instance,
                                    DoubleType.instance,
                                    DecimalType.instance}, new AbstractType[]{});

        // Setup integer part mapping
        if (integerDigits != null && integerDigits <= 0) {
            throw new IllegalArgumentException("Positive integer part digits required");
        }
        this.integerDigits = integerDigits == null ? DEFAULT_INTEGER_DIGITS : integerDigits;

        // Setup decimal part mapping
        if (decimalDigits != null && decimalDigits <= 0) {
            throw new IllegalArgumentException("Positive decimal part digits required");
        }
        this.decimalDigits = decimalDigits == null ? DEFAULT_DECIMAL_DIGITS : decimalDigits;

        int totalDigits = this.integerDigits + this.decimalDigits;
        BigDecimal divisor = BigDecimal.valueOf(10).pow(this.decimalDigits);
        BigDecimal dividend = BigDecimal.valueOf(10).pow(totalDigits).subtract(BigDecimal.valueOf(1));
        complement = dividend.divide(divisor);
    }

    /**
     * Returns the max number of digits for the integer part.
     *
     * @return The max number of digits for the integer part.
     */
    public int getIntegerDigits() {
        return integerDigits;
    }

    /**
     * Returns the max number of digits for the decimal part.
     *
     * @return The max number of digits for the decimal part.
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }

    /** {@inheritDoc} */
    @Override
    public String indexValue(String name, Object value) {

        // Check not null
        if (value == null) {
            return null;
        }

        // Parse big decimal
        String svalue = value.toString();
        BigDecimal bd;
        try {
            bd = new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            String message = String.format("Field %s requires a base 10 decimal, but found \"%s\"", name, svalue);
            throw new IllegalArgumentException(message);
        }

        // Split integer and decimal part
        bd = bd.stripTrailingZeros();
        String[] parts = bd.toPlainString().split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length == 1 ? "0" : parts[1];

        if (integerPart.replaceFirst("-", "").length() > integerDigits) {
            throw new IllegalArgumentException("Too much digits in integer part");
        }
        if (decimalPart.length() > decimalDigits) {
            throw new IllegalArgumentException("Too much digits in decimal part");
        }

        BigDecimal complemented = bd.add(complement);
        String bds[] = complemented.toString().split("\\.");
        integerPart = bds[0];
        decimalPart = bds.length == 2 ? bds[1] : "0";
        integerPart = StringUtils.leftPad(integerPart, integerDigits + 1, '0');

        return integerPart + "." + decimalPart;
    }

    /** {@inheritDoc} */
    @Override
    public String queryValue(String name, Object value) {
        return indexValue(name, value);
    }

    /** {@inheritDoc} */
    @Override
    public Field field(String name, Object value) {
        String string = indexValue(name, value);
        return new StringField(name, string, STORE);
    }

    /** {@inheritDoc} */
    @Override
    public SortField sortField(String field, boolean reverse) {
        return new SortField(field, Type.STRING, reverse);
    }

    /** {@inheritDoc} */
    @Override
    public Class<String> baseClass() {
        return String.class;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                      .add("integerDigits", integerDigits)
                      .add("decimalDigits", decimalDigits)
                      .toString();
    }
}
