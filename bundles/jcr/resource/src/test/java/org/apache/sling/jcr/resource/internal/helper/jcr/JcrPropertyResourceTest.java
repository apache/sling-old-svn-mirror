/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.resource.internal.helper.jcr;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JcrPropertyResourceTest ...
 */
@RunWith(JMock.class)
public class JcrPropertyResourceTest {

    protected final Mockery context = new JUnit4Mockery();

    @Test
    public void testCorrectUTF8ByteLength() throws RepositoryException, UnsupportedEncodingException {
        final HashMap<Object, Integer> testData = new HashMap<Object, Integer>() {{
            put("some random ascii string", PropertyType.STRING);
            put("русский язык", PropertyType.STRING);
            put("贛語", PropertyType.STRING);
            put("string with ümlaut", PropertyType.STRING);
            put(true, PropertyType.BOOLEAN);
            put(1000L, PropertyType.LONG);
            put(BigDecimal.TEN, PropertyType.DECIMAL);
        }};

        final ResourceResolver resolver = this.context.mock(ResourceResolver.class);
        for (final Entry<Object, Integer> data : testData.entrySet()) {
            final String stringValue = data.getKey().toString();
            final long stringByteLength =  stringValue.getBytes("UTF-8").length;
            final Property property = this.context.mock(Property.class, stringValue);
            this.context.checking(new Expectations() {{
                ignoring(resolver);
                allowing(property).getParent();
                allowing(property).getName();
                allowing(property).isMultiple(); will(returnValue(false));
                allowing(property).getLength(); will(returnValue((long) stringValue.length()));

                allowing(property).getType(); will(returnValue(data.getValue()));
                allowing(property).getString(); will(returnValue(stringValue));
            }});
            final JcrPropertyResource propResource = new JcrPropertyResource(resolver, "/path/to/string-property", null, property);
            assertEquals("Byte length of " +  stringValue, stringByteLength, propResource.getResourceMetadata().getContentLength());
        }
    }
}
