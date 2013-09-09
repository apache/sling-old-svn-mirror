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
package org.apache.sling.ide.impl.vlt.serialization;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.array;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ContentXmlHandlerTest {

    @Test
    public void simpleContentXml() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("simple-content.xml");

        assertThat("properties.size", properties.size(), is(7));

        assertThat("properties[jcr:primaryType]", properties, hasEntry("jcr:primaryType", (Object) "sling:Folder"));
        assertThat("properties[jcr:title]", properties, hasEntry("jcr:title", (Object) "Application folder"));
        assertThat("properties[indexed]", properties, hasEntry("indexed", (Object) Boolean.TRUE));
        assertThat("properties[indexRatio]", properties, hasEntry("indexRatio", (Object) Double.valueOf(2.54)));
        assertThat("properties[indexDuration]", properties, hasEntry("indexDuration", (Object) BigDecimal.valueOf(500)));
        assertThat("properties[lastIndexTime]", (Calendar) properties.get("lastIndexTime"),
                is(millis(1378292400000l)));
        assertThat("properties[lastIndexId]", properties,
                hasEntry("lastIndexId", (Object) Long.valueOf(7293120000000l)));

    }

    private Map<String, Object> parseContentXmlFile(String fileName) throws ParserConfigurationException, SAXException,
            IOException {

        InputSource source = new InputSource(getClass().getResourceAsStream(fileName));

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();
        ContentXmlHandler handler = new ContentXmlHandler();
        parser.parse(source, handler);

        return handler.getProperties();
    }

    @Test
    @Ignore("Not implemented")
    public void parseNameProperty() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("name-content.xml");

        assertThat("properties.size", properties.size(), is(2));
    }

    @Test
    @Ignore("Not implemented")
    public void parsePathProperty() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("path-content.xml");

        assertThat("properties.size", properties.size(), is(2));
    }

    @Test
    @Ignore("Not implemented")
    public void parseReferenceProperties() throws ParserConfigurationException, SAXException, IOException {
        
        Map<String, Object> properties = parseContentXmlFile("reference-content.xml");
        
        assertThat("properties.size", properties.size(), is(3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parseMultiValuedProperties() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("multivalued-properties-content.xml");

        assertThat("properties.size", properties.size(), is(7));
        assertThat("properties[values]", (String[]) properties.get("values"),
                Matchers.is(new String[] { "first", "second" }));
        assertThat("properties[decimals]", (BigDecimal[]) properties.get("decimals"),
                Matchers.is(new BigDecimal[] { new BigDecimal("5.10"), new BigDecimal("5.11") }));
        assertThat("properties[doubles]", (Double[]) properties.get("doubles"),
                Matchers.is(new Double[] { new Double("5.1"), new Double("7.5"), new Double("9.0") }));
        assertThat("properties[flags]", (Boolean[]) properties.get("flags"),
                Matchers.is(new Boolean[] { Boolean.FALSE, Boolean.TRUE }));
        assertThat("properties[longs]", (Long[]) properties.get("longs"),
                Matchers.is(new Long[] { Long.valueOf(15), Long.valueOf(25) }));
        assertThat("properties[dates]", (Calendar[]) properties.get("dates"), array(
                millis(1377982800000l), millis(1378242000000l)));
    }
    
    private static Matcher<Calendar> millis(long millis) {
        
        return new CalendarTimeInMillisMatcher(millis);
    }

    static class CalendarTimeInMillisMatcher extends TypeSafeMatcher<Calendar> {

        private final long timeInMillis;

        private CalendarTimeInMillisMatcher(long timeInMillis) {
            this.timeInMillis = timeInMillis;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("getTimeInMillis() does not equal ").appendValue(timeInMillis);
        }

        @Override
        protected boolean matchesSafely(Calendar item) {
            return timeInMillis == item.getTimeInMillis();
        }

    }
}
