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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.sling.ide.transport.ResourceProxy;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ContentXmlHandlerTest {

    @Test
    public void simpleContentXml() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("simple-content.xml", "/").getProperties();

        assertThat("properties.size", properties.size(), is(12));

        assertThat("properties[jcr:primaryType]", properties, hasEntry("jcr:primaryType", (Object) "sling:Folder"));
        assertThat("properties[jcr:title]", properties, hasEntry("jcr:title", (Object) "Application folder"));
        assertThat("properties[indexed]", properties, hasEntry("indexed", (Object) Boolean.TRUE));
        assertThat("properties[indexRatio]", properties, hasEntry("indexRatio", (Object) Double.valueOf(2.54)));
        assertThat("properties[indexDuration]", properties, hasEntry("indexDuration", (Object) BigDecimal.valueOf(500)));
        assertThat("properties[lastIndexTime]", (Calendar) properties.get("lastIndexTime"),
                is(millis(1378292400000l)));
        assertThat("properties[lastIndexId]", properties,
                hasEntry("lastIndexId", (Object) Long.valueOf(7293120000000l)));
        assertThat("properties[lastIndexId]", properties,
                hasEntry("lastIndexId", (Object) Long.valueOf(7293120000000l)));
        assertThat("properties[emptyValue]", properties, hasEntry("emptyValue", (Object) ""));

    }

    private ResourceProxy parseContentXmlFile(String fileName, String rootResourcePath)
            throws ParserConfigurationException, SAXException,
            IOException {

        InputSource source = new InputSource(getClass().getResourceAsStream(fileName));

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        SAXParser parser = factory.newSAXParser();
        ContentXmlHandler handler = new ContentXmlHandler(rootResourcePath);
        parser.parse(source, handler);

        return handler.getRoot();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void parseMultiValuedProperties() throws ParserConfigurationException, SAXException, IOException {

        Map<String, Object> properties = parseContentXmlFile("multivalued-properties-content.xml", "/").getProperties();

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

    @Test
    @SuppressWarnings("unchecked")
    public void parseSingleExplicitMultiValuedProperties() throws ParserConfigurationException, SAXException,
            IOException {

        Map<String, Object> properties = parseContentXmlFile("single-explicit-multivalued-properties-content.xml", "/")
                .getProperties();

        assertThat("properties.size", properties.size(), is(7));
        assertThat("properties[values]", (String[]) properties.get("values"),
                Matchers.is(new String[] { "first"}));
        assertThat("properties[decimals]", (BigDecimal[]) properties.get("decimals"),
                Matchers.is(new BigDecimal[] { new BigDecimal("5.10")}));
        assertThat("properties[doubles]", (Double[]) properties.get("doubles"),
                Matchers.is(new Double[] { new Double("5.1") }));
        assertThat("properties[flags]", (Boolean[]) properties.get("flags"),
                Matchers.is(new Boolean[] { Boolean.FALSE }));
        assertThat("properties[longs]", (Long[]) properties.get("longs"),
                Matchers.is(new Long[] { Long.valueOf(15)}));
        assertThat("properties[dates]", (Calendar[]) properties.get("dates"),
                array(millis(1377982800000l)));
    }

    @Test
    public void parseFullCoverageXmlFile() throws ParserConfigurationException, SAXException, IOException {

        ResourceProxy root = parseContentXmlFile("full-coverage.xml", "/apps/full-coverage");

        assertThat("full-coverage path", root.getPath(), is("/apps/full-coverage"));
        assertThat("full-coverage properties.size", root.getProperties().size(), is(3));
        assertThat("full-coverage properties[jcr:title]", root.getProperties(),
                hasEntry("jcr:title", (Object) "Full coverage parent"));
        assertThat("full-coverage children.size", root.getChildren().size(), is(2));

        ResourceProxy parent1 = root.getChildren().get(0);
        assertThat("parent-1 path", parent1.getPath(), is("/apps/full-coverage/parent-1"));
        assertThat("parent-1 properties[jcr:title]", parent1.getProperties(),
                hasEntry("jcr:title", (Object) "Parent 1"));
        assertThat("parent-1 children.size", parent1.getChildren().size(), is(2));

        ResourceProxy child11 = parent1.getChildren().get(0);
        assertThat("child-1-1 path", child11.getPath(), is("/apps/full-coverage/parent-1/child-1-1"));
        assertThat("child-1-1 properties[jcr:title]", child11.getProperties(),
                hasEntry("jcr:title", (Object) "Child 1-1"));

    }

    @Test
    public void parseRootContentXml() throws ParserConfigurationException, SAXException, IOException {

        ResourceProxy root = parseContentXmlFile("root-content.xml", "/");

        assertThat("root contains /jcr:system", root.getChildren(), hasChildPath("/jcr:system"));
    }

    @Test
    public void encodedChildContentXml() throws ParserConfigurationException, SAXException, IOException {

        ResourceProxy root = parseContentXmlFile("encoded-child-content.xml", "/ROOT");

        assertThat("/ROOT contains /_jcr_content", root.getChildren(), hasChildPath("/ROOT/_jcr_content"));
    }

    @Test
    public void parseContentXmlWithEscapedNames() throws ParserConfigurationException, SAXException, IOException {

        ResourceProxy root = parseContentXmlFile("full-coverage-escaped-names.xml", "/");
        assertThat("node contains /50-50", root.getChildren(), hasChildPath("/50-50"));
    }

    @Test
    public void parseContentXmlWithBinaryProperty() throws ParserConfigurationException, SAXException, IOException {

        ResourceProxy root = parseContentXmlFile("binary-property.xml", "/");

        assertThat("root has 1 property, binary property is ignored", root.getProperties().entrySet(), hasSize(1));
    }

    @Test
    public void escapedBraceAtStartOfPropertyValue() throws Exception {

        ResourceProxy root = parseContentXmlFile("escaped-braces-at-start-of-property.xml", "/");
        assertThat("properties[org.apache.sling.commons.log.pattern]",
                root.getProperties(), hasEntry("org.apache.sling.commons.log.pattern",
                        (Object) "{0,date,dd.MM.yyyy HH:mm:ss.SSS} *{4}* [{2}] {3} {5}"));
    }

    @Test
    public void escapedCommaInMultiValuedProperty() throws Exception {
        
        ResourceProxy root = parseContentXmlFile("escaped-comma-in-multi-valued-property.xml", "/");
        assertThat("properties[someProp]", (String[]) root.getProperties().get("someProp"),
                Matchers.is(new String[] { "first,first", "second" }));
    }

    private static Matcher<Calendar> millis(long millis) {

        return new CalendarTimeInMillisMatcher(millis);
    }

    private static Matcher<Iterable<? extends ResourceProxy>> hasChildPath(String path) {

        return new ResourceChildPathMatcher(path);
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

    static class ResourceChildPathMatcher extends TypeSafeMatcher<Iterable<? extends ResourceProxy>> {

        private final String resourcePath;

        private ResourceChildPathMatcher(String resourcePath) {

            this.resourcePath = resourcePath;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Resource with path ").appendValue(resourcePath);
        }

        @Override
        protected boolean matchesSafely(Iterable<? extends ResourceProxy> item) {
            for (ResourceProxy resource : item) {
                if (resourcePath.equals(resource.getPath())) {
                    return true;
                }
            }
            return false;
        }
    }
}
