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
package org.apache.sling.jcr.contentloader.internal;

import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;

import javax.jcr.*;

import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentReader;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(JMock.class)
public class DefaultContentCreatorTest {
    
    DefaultContentCreator contentCreator;
    
    Mockery mockery = new JUnit4Mockery();
    
    Node parentNode;
    
    Property prop;
    
    @Test
    public void willRewriteUndefinedPropertyType() throws RepositoryException {
        contentCreator = new DefaultContentCreator(null);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(U.createImportOptions(true, true, true, false, false),
                new HashMap<String, ContentReader>(), null, null);
        
        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
        	allowing(parentNode).isNodeType("mix:versionable"); will(returnValue(Boolean.FALSE));
        	allowing(parentNode).getParent(); will(returnValue(null));
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).setProperty(with(equal("foo")), with(equal("bar")));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }
    
    @Test
    public void willNotRewriteUndefinedPropertyType() throws RepositoryException {
        contentCreator = new DefaultContentCreator(null);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);
        contentCreator.init(U.createImportOptions(false, false, true, false, false),
                new HashMap<String, ContentReader>(), null, null);
        
        contentCreator.prepareParsing(parentNode, null);
        this.mockery.checking(new Expectations() {{
            oneOf (parentNode).hasProperty("foo"); will(returnValue(Boolean.TRUE));
            oneOf (parentNode).getProperty("foo"); will(returnValue(prop));
            oneOf (prop).isNew(); will(returnValue(Boolean.FALSE));
        }});
        contentCreator.createProperty("foo", PropertyType.UNDEFINED, "bar");
    }

    @Test
    public void testDoesNotCreateProperty() throws RepositoryException {
        final String propertyName = "foo";
        prop = mockery.mock(Property.class);
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(propertyName); will(returnValue(true));
            oneOf(parentNode).getProperty(propertyName); will(returnValue(prop));
            oneOf(prop).isNew(); will(returnValue(false));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);
        //By calling this method we expect that it will returns on first if-statement
        contentCreator.createProperty("foo", PropertyType.REFERENCE, "bar");
        //Checking that
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateReferenceProperty() throws RepositoryException {
        final String propertyName = "foo";
        final String propertyValue = "bar";
        final String rootNodeName = "root";
        final String uuid = "1b8c88d37f0000020084433d3af4941f";
        final Session session = mockery.mock(Session.class);
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(session).itemExists(with(any(String.class))); will(returnValue(true));
            oneOf(session).getItem(with(any(String.class))); will(returnValue(parentNode));

            exactly(2).of(parentNode).getPath(); will(returnValue("/" + rootNodeName));
            oneOf(parentNode).isNode(); will(returnValue(true));
            oneOf(parentNode).isNodeType("mix:referenceable"); will(returnValue(true));
            oneOf(parentNode).getUUID(); will(returnValue(uuid));
            oneOf(parentNode).getSession(); will(returnValue(session));
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).setProperty(propertyName, uuid, PropertyType.REFERENCE);
            oneOf(parentNode).getProperty(with(any(String.class)));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode,null);
        contentCreator.createProperty(propertyName, PropertyType.REFERENCE, propertyValue);
        //The only way I found how to test this method is to check numbers of methods calls
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateFalseCheckedOutPreperty() throws RepositoryException {
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        int numberOfVersionablesNodes = contentCreator.getVersionables().size();
        contentCreator.createProperty("jcr:isCheckedOut", PropertyType.UNDEFINED, "false");
        //Checking that list of versionables was changed
        assertEquals(numberOfVersionablesNodes + 1, contentCreator.getVersionables().size());
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateTrueCheckedOutPreperty() throws RepositoryException {
        parentNode = mockery.mock(Node.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, null);
        contentCreator.prepareParsing(parentNode, null);

        int numberOfVersionablesNodes = contentCreator.getVersionables().size();
        contentCreator.createProperty("jcr:isCheckedOut", PropertyType.UNDEFINED, "true");
        //Checking that list of versionables doesn't changed
        assertEquals(numberOfVersionablesNodes, contentCreator.getVersionables().size());
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreateDateProperty() throws RepositoryException, ParseException {
        final String propertyName = "dateProp";
        final String propertyValue = "2012-10-01T09:45:00.000+02:00";
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).setProperty(with(any(String.class)), with(any(Calendar.class)));
            oneOf(parentNode).getProperty(with(any(String.class))); will(returnValue(prop));
            oneOf(prop).getPath(); will(returnValue(""));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, PropertyType.DATE, propertyValue);
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCreatePropertyWithNewPropertyType() throws RepositoryException, ParseException {
        final String propertyName = "foo";
        final String propertyValue = "bar";
        final Integer propertyType = -1;
        final ContentImportListener listener = mockery.mock(ContentImportListener.class);
        parentNode = mockery.mock(Node.class);
        prop = mockery.mock(Property.class);

        this.mockery.checking(new Expectations(){{
            oneOf(parentNode).hasProperty(with(any(String.class)));
            oneOf(parentNode).getProperty(propertyName); will(returnValue(prop));
            oneOf(parentNode).setProperty(propertyName, propertyValue, propertyType);
            oneOf(prop).getPath(); will(returnValue(""));
            oneOf(listener).onCreate(with(any(String.class)));
        }});

        contentCreator = new DefaultContentCreator(null);
        contentCreator.init(U.createImportOptions(false, false, false, false, false),
                new HashMap<String, ContentReader>(), null, listener);
        contentCreator.prepareParsing(parentNode, null);

        contentCreator.createProperty(propertyName, propertyType, propertyValue);
        mockery.assertIsSatisfied();
    }
}
