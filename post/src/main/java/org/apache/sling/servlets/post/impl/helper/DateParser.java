/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes a string representation of a time-date string and tries for parse it
 * using different formats.
 */
public class DateParser {

    /**
     * default log
     */
    private static final Logger log = LoggerFactory.getLogger(DateParser.class);

    /**
     * lits of formats
     */
    private final List<CalendarParserSupport> formats = new LinkedList<CalendarParserSupport>();

    /**
     * Registers a format string to the list of internally checked ones.
     * Uses the {@link SimpleDateFormat}.
     * @param format format as in {@link SimpleDateFormat}
     * @throws IllegalArgumentException if the format is not valid.
     */
    public void register(String format) {
        final CalendarParserSupport parser;
        if (Iso8601ParserSupport.FORMAT_MARKER.equalsIgnoreCase(format)) {
            parser = new Iso8601ParserSupport();
        } else {
            parser = new SimpleDateFormatParserSupport(format);
        }
        formats.add(parser);
    }

    /**
     * Parses the given source string and returns the respective calendar
     * instance. If no format matches returns <code>null</code>.
     * <p/>
     * Note: method is synchronized because SimpleDateFormat is not.
     *
     * @param source date time source string
     * @return calendar representation of the source or <code>null</code>
     */
    public synchronized Calendar parse(String source) {
        for (CalendarParserSupport fmt : formats) {
            try {
                final Calendar c = fmt.parse(source);
                if (log.isDebugEnabled()) {
                    log.debug("Parsed " + source + " using " + fmt + " into "
                        + c);
                }
                return c;
            } catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed parsing " + source + " using " + fmt);
                }
            }
        }
        return null;
    }

    /**
     * Parses the given source strings and returns the respective calendar
     * instances. If no format matches for any of the sources
     * returns <code>null</code>.
     * <p/>
     * Note: method is synchronized because SimpleDateFormat is not.
     *
     * @param sources date time source strings
     * @return calendar representations of the source or <code>null</code>
     */
    public synchronized Calendar[] parse(String sources[]) {
        Calendar ret[] = new Calendar[sources.length];
        for (int i=0; i< sources.length; i++) {
            if ((ret[i] = parse(sources[i])) == null) {
                return null;
            }
        }
        return ret;
    }

    /**
     * Parses the given source strings and returns the respective jcr date value
     * instances. If no format matches for any of the sources
     * returns <code>null</code>.
     * <p/>
     * Note: method is synchronized because SimpleDateFormat is not.
     *
     * @param sources date time source strings
     * @param factory the value factory
     * @return jcr date value representations of the source or <code>null</code>
     */
    public synchronized Value[] parse(String sources[], ValueFactory factory) {
        Value ret[] = new Value[sources.length];
        for (int i=0; i< sources.length; i++) {
            Calendar c = parse(sources[i]);
            if (c == null) {
                return null;
            }
            ret[i] = factory.createValue(c);
        }
        return ret;
    }

    private static interface CalendarParserSupport {
        Calendar parse(String dateTime) throws ParseException;
    }

    private static class SimpleDateFormatParserSupport implements CalendarParserSupport {
        private final SimpleDateFormat dateFormat;

        SimpleDateFormatParserSupport(String format) {
            this.dateFormat = new SimpleDateFormat(format, Locale.US);
        }

        public Calendar parse(String dateTime) throws ParseException {
            final Date d;
            synchronized (dateFormat) {
                d = dateFormat.parse(dateTime);
            }

            final Calendar c = Calendar.getInstance();
            c.setTime(d);

            return c;
        }

        @Override
        public String toString() {
            return "SimpleDateFormat:" + dateFormat.toPattern();
        }
    }

    private static class Iso8601ParserSupport implements CalendarParserSupport {

        static final String FORMAT_MARKER = "ISO8601";

        public Calendar parse(String dateTime) throws ParseException {
            try {
                final Calendar c = ISO8601.parse(dateTime);
                if (c == null) {
                    throw new ParseException(dateTime
                        + " cannot be parsed as ISO8601 formatted date string",
                        0);
                }
                return c;
            } catch (Exception e) {
                throw new ParseException(e.getMessage(), 0);
            }
        }

        @Override
        public String toString() {
            return "ISO8601 Parser";
        }
    }

}