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
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A {@link ColumnMapper} to map a date field.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class ColumnMapperDate extends ColumnMapperSingle<Long> {

    /** The default {@link java.text.SimpleDateFormat} pattern. */
    public static final String DEFAULT_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";

    /** The {@link java.text.SimpleDateFormat} pattern. */
    private final String pattern;

    /** The thread safe date format. */
    private final ThreadLocal<DateFormat> concurrentDateFormat;

    /**
     * Builds a new {@link ColumnMapperDate} using the specified pattern.
     *
     * @param pattern The {@link java.text.SimpleDateFormat} pattern to be used.
     */
    @JsonCreator
    public ColumnMapperDate(@JsonProperty("pattern") String pattern) {
        super(new AbstractType<?>[]{AsciiType.instance,
                                    UTF8Type.instance,
                                    Int32Type.instance,
                                    LongType.instance,
                                    IntegerType.instance,
                                    FloatType.instance,
                                    DoubleType.instance,
                                    DecimalType.instance,
                                    TimestampType.instance},
              new AbstractType[]{LongType.instance, TimestampType.instance});
        this.pattern = pattern == null ? DEFAULT_PATTERN : pattern;
        concurrentDateFormat = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat(ColumnMapperDate.this.pattern);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public Long indexValue(String name, Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return concurrentDateFormat.get().parse(value.toString()).getTime();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Long queryValue(String name, Object value) {
        return indexValue(name, value);
    }

    /** {@inheritDoc} */
    @Override
    public Field field(String name, Object value) {
        return new LongField(name, indexValue(name, value), STORE);
    }

    /** {@inheritDoc} */
    @Override
    public SortField sortField(String field, boolean reverse) {
        return new SortField(field, Type.LONG, reverse);
    }

    /** {@inheritDoc} */
    @Override
    public Class<Long> baseClass() {
        return Long.class;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("pattern", pattern).toString();
    }
}
