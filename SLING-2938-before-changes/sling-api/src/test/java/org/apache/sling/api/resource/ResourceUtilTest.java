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
package org.apache.sling.api.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ResourceUtilTest {

    protected final Mockery context = new JUnit4Mockery();

    @Test public void testResolveRelativeSegments() {

        assertEquals("/", ResourceUtil.normalize("/"));
        assertEquals("/", ResourceUtil.normalize("///"));

        assertEquals("/a/b/c", ResourceUtil.normalize("/a//b/c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b//c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a///b///c"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c/"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c//"));
        assertEquals("/a/b/c", ResourceUtil.normalize("/a/b/c///"));

        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az//bz/cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz//cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az///bz///cz"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz/"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz//"));
        assertEquals("/az/bz/cz", ResourceUtil.normalize("/az/bz/cz///"));

        assertEquals("/a", ResourceUtil.normalize("/a"));
        assertEquals("/a", ResourceUtil.normalize("//a"));
        assertEquals("/a", ResourceUtil.normalize("///a"));

        assertEquals("/az", ResourceUtil.normalize("/az"));
        assertEquals("/az", ResourceUtil.normalize("//az"));
        assertEquals("/az", ResourceUtil.normalize("///az"));

        assertEquals("/", ResourceUtil.normalize("/."));
        assertEquals("/a", ResourceUtil.normalize("/a/."));
        assertEquals("/a", ResourceUtil.normalize("/./a"));
        assertEquals("/a/b", ResourceUtil.normalize("/a/./b"));
        assertEquals("/a/b", ResourceUtil.normalize("/a/b/."));
        assertEquals("/a/b", ResourceUtil.normalize("/a/./b/."));

        assertEquals("/", ResourceUtil.normalize("/."));
        assertEquals("/az", ResourceUtil.normalize("/az/."));
        assertEquals("/az", ResourceUtil.normalize("/./az"));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/./bz"));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/bz/."));
        assertEquals("/az/bz", ResourceUtil.normalize("/az/./bz/."));

        assertNull(ResourceUtil.normalize("/.."));
        assertNull(ResourceUtil.normalize("/.."));
        assertEquals("/", ResourceUtil.normalize("/a/.."));
        assertEquals("/a", ResourceUtil.normalize("/a/b/.."));
        assertEquals("/", ResourceUtil.normalize("/a/b/../.."));
        assertNull(ResourceUtil.normalize("/a/b/../../.."));

        assertNull(ResourceUtil.normalize("/.."));
        assertNull(ResourceUtil.normalize("/.."));
        assertEquals("/", ResourceUtil.normalize("/az/.."));
        assertEquals("/az", ResourceUtil.normalize("/az/bz/.."));
        assertEquals("/", ResourceUtil.normalize("/az/bz/../.."));
        assertNull(ResourceUtil.normalize("/az/bz/../../.."));

        assertEquals("/b", ResourceUtil.normalize("/a/../b"));
        assertEquals("/a/c", ResourceUtil.normalize("/a/b/../c"));
        assertEquals("/c", ResourceUtil.normalize("/a/b/../../c"));
        assertNull(ResourceUtil.normalize("/a/b/../../../c"));

        assertEquals("/bz", ResourceUtil.normalize("/az/../bz"));
        assertEquals("/az/cz", ResourceUtil.normalize("/az/bz/../cz"));
        assertEquals("/cz", ResourceUtil.normalize("/az/bz/../../cz"));
        assertNull(ResourceUtil.normalize("/az/bz/../../../cz"));

        assertEquals("/...", ResourceUtil.normalize("/..."));
        assertEquals("/a/...", ResourceUtil.normalize("/a/..."));
        assertEquals("/a/b/...", ResourceUtil.normalize("/a/b/..."));

        assertEquals("/az/...", ResourceUtil.normalize("/az/..."));
        assertEquals("/az/bz/...", ResourceUtil.normalize("/az/bz/..."));

        try {
            ResourceUtil.normalize(null);
            fail("Resolving null expects NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test public void testResolveRelativeSegmentsRelative() {
        assertEquals("a/b", ResourceUtil.normalize("a/b"));
        assertEquals("a", ResourceUtil.normalize("a/b/.."));

        assertEquals("b", ResourceUtil.normalize("a/../b"));
        assertEquals("a/c", ResourceUtil.normalize("a/b/../c"));
        assertEquals("c", ResourceUtil.normalize("a/b/../../c"));
        assertEquals("", ResourceUtil.normalize("a/b/../.."));
        assertEquals("a/c/d", ResourceUtil.normalize("a/b/../c/d"));
        assertNull(ResourceUtil.normalize("a/b/../../../c"));

        assertEquals("a/b/c", ResourceUtil.normalize("a/b/c"));
        assertEquals("az/bz/cz", ResourceUtil.normalize("az/bz/cz"));
        assertEquals("", ResourceUtil.normalize(""));
    }

    @Test public void testGetParent() {
        assertNull(ResourceUtil.getParent("/"));
        assertNull(ResourceUtil.getParent("/.."));

        assertEquals("/", ResourceUtil.getParent("/b"));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d"));
        assertEquals("/b/c", ResourceUtil.getParent("/b/c/d"));

        assertNull(ResourceUtil.getParent("b"));
        assertNull(ResourceUtil.getParent("/b/.."));

        assertEquals("security:/", ResourceUtil.getParent("security:/b"));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b/c"));
        assertEquals("security:/b/c", ResourceUtil.getParent("security:/b/c/d"));
    }

    @Test public void testGetName() {
        assertEquals("", ResourceUtil.getName("/"));
        assertEquals("", ResourceUtil.getName("/a/.."));

        assertEquals("c", ResourceUtil.getName("c"));
        assertEquals("c", ResourceUtil.getName("/c"));

        assertEquals("c", ResourceUtil.getName("b/c"));
        assertEquals("c", ResourceUtil.getName("/b/c"));

        assertEquals("c", ResourceUtil.getName("b/c/"));
        assertEquals("c", ResourceUtil.getName("/b/c/"));

        assertEquals("b", ResourceUtil.getName("b/c/.."));
        assertEquals("b", ResourceUtil.getName("/b/c/.."));
        assertEquals("", ResourceUtil.getName("/b/c/../.."));
    }

    @Test public void test_getValueMap_null_resource() {
        final ValueMap valueMap = ResourceUtil.getValueMap(null);
        assertNotNull(valueMap);
        assertEquals(0, valueMap.size());

        final Object replaced = valueMap.put("sample", 1);
        assertNull(replaced);

        assertEquals(1, valueMap.size());
        assertEquals(1, valueMap.get("sample"));
        assertEquals(Integer.valueOf(1), valueMap.get("sample", Integer.class));
        assertEquals("1", valueMap.get("sample", String.class));
    }

    @Test public void test_getValueMap_direct() {
        final ValueMap valueMap = new ValueMapDecorator(new HashMap<String, Object>());
        valueMap.put("sample", true);
        final Resource resource = new SyntheticResource(null, "/", "sample") {
            @Override
            @SuppressWarnings("unchecked")
            public <Type> Type adaptTo(Class<Type> type) {
                if (type == ValueMap.class) {
                    return (Type) valueMap;
                }

                return super.adaptTo(type);
            }
        };

        final ValueMap adapted = ResourceUtil.getValueMap(resource);
        assertEquals(valueMap, adapted);
        assertNotNull(adapted);
        assertEquals(1, adapted.size());

        assertEquals(true, adapted.get("sample"));
        assertEquals(Boolean.valueOf(true), adapted.get("sample", Boolean.class));
        assertEquals(Boolean.TRUE.toString(), adapted.get("sample", String.class));
    }

    @Test public void test_getValueMap_decorated_map() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("sample", true);
        final Resource resource = new SyntheticResource(null, "/", "sample") {
            @Override
            @SuppressWarnings("unchecked")
            public <Type> Type adaptTo(Class<Type> type) {
                if (type == Map.class) {
                    return (Type) map;
                }

                return super.adaptTo(type);
            }
        };

        final ValueMap adapted = ResourceUtil.getValueMap(resource);
        assertNotNull(adapted);
        assertEquals(1, adapted.size());

        assertEquals(true, adapted.get("sample"));
        assertEquals(Boolean.valueOf(true), adapted.get("sample", Boolean.class));
        assertEquals(Boolean.TRUE.toString(), adapted.get("sample", String.class));
    }

    @Test public void test_getValueMap_no_adapter() {
        final ValueMap valueMap = ResourceUtil.getValueMap(null);
        assertNotNull(valueMap);
        assertEquals(0, valueMap.size());

        final Object replaced = valueMap.put("sample", 1);
        assertNull(replaced);

        assertEquals(1, valueMap.size());
        assertEquals(1, valueMap.get("sample"));
        assertEquals(Integer.valueOf(1), valueMap.get("sample", Integer.class));
        assertEquals("1", valueMap.get("sample", String.class));
    }

    @Test public void test_resourceTypeToPath() {
        assertEquals("a/b", ResourceUtil.resourceTypeToPath("a:b"));
    }

    @SuppressWarnings("unchecked")
    @Test public void test_adaptTo() {
        // we define three resources
        // a and b are adaptable to List
        // a, b, and c are adaptable to Map
        // none is adaptable to String
        // b and c are adaptable to long
        // a and c are adaptable to boolean
        final Resource a = this.context.mock(Resource.class, "a");
        final Resource b = this.context.mock(Resource.class, "b");
        final Resource c = this.context.mock(Resource.class, "c");
        final List<Resource> l = new ArrayList<Resource>();
        l.add(a); l.add(b); l.add(c);
        this.context.checking(new Expectations() {{
            allowing(a).adaptTo(List.class); will(returnValue(new ArrayList()));
            allowing(b).adaptTo(List.class); will(returnValue(new ArrayList()));
            allowing(c).adaptTo(List.class); will(returnValue(null));
            allowing(a).adaptTo(Map.class); will(returnValue(new HashMap()));
            allowing(b).adaptTo(Map.class); will(returnValue(new HashMap()));
            allowing(c).adaptTo(Map.class); will(returnValue(new HashMap()));
            allowing(a).adaptTo(Long.class); will(returnValue(null));
            allowing(b).adaptTo(Long.class); will(returnValue(new Long(1)));
            allowing(c).adaptTo(Long.class); will(returnValue(new Long(2)));
            allowing(a).adaptTo(Boolean.class); will(returnValue(new Boolean(true)));
            allowing(b).adaptTo(Boolean.class); will(returnValue(null));
            allowing(c).adaptTo(Boolean.class); will(returnValue(new Boolean(false)));
            allowing(a).adaptTo(String.class); will(returnValue(null));
            allowing(b).adaptTo(String.class); will(returnValue(null));
            allowing(c).adaptTo(String.class); will(returnValue(null));
        }});

        assertEquals(2, checkIterator(l, List.class));
        assertEquals(3, checkIterator(l, Map.class));
        assertEquals(0, checkIterator(l, String.class));
        assertEquals(2, checkIterator(l, Long.class));
        assertEquals(2, checkIterator(l, Boolean.class));
    }

    private <T> int checkIterator(final List<Resource> resources, final Class<T> type) {
        final Iterator<T> i = ResourceUtil.adaptTo(resources.iterator(), type);
        // we call hasNext() several times upfront
        i.hasNext();
        i.hasNext();
        int count = 0;
        while ( i.hasNext() ) {
            final T object = i.next();
            assertNotNull(object);
            count++;
        }
        assertFalse(i.hasNext());
        // next should throw an exception
        try {
            i.next();
            fail("Iterator should have reached end.");
        } catch (NoSuchElementException nsee) {
            // fine
        }
        return count;
    }

    @Test public void testIsStarResource() {
        final Resource nonStar = context.mock(Resource.class, "nonStarResource");
        final String starPath = "/foo/*";
        final Resource star = context.mock(Resource.class, "starResource");
        final String nonStarPath = "/foo/*";
        this.context.checking(new Expectations() {{
            allowing(star).getPath(); will(returnValue(starPath));
            allowing(nonStar).getPath(); will(returnValue(nonStarPath));
        }});

        assertTrue("expecting star==true for path" + starPath,
                ResourceUtil.isStarResource(star));
        assertTrue("expecting star==false for path" + starPath,
                ResourceUtil.isStarResource(nonStar));
    }
    @Test public void testIsSyntheticResource() {
        final Resource synth = new SyntheticResource(null, "foo", "bar");
        final Resource star = context.mock(Resource.class);
        this.context.checking(new Expectations() {{
            allowing(star).getPath(); will(returnValue("/foo/*"));
        }});
        final Resource wrapped = new ResourceWrapper(synth);

        assertTrue("expecting synthetic==true for SyntheticResource",
                ResourceUtil.isSyntheticResource(synth));
        assertFalse("expecting synthetic==false for star resource",
                ResourceUtil.isSyntheticResource(star));
        assertTrue("expecting synthetic==true for wrapped Resource",
                ResourceUtil.isSyntheticResource(wrapped));
    }

    @Test public void testGetParentLevel() throws Exception {
        boolean caughtNullPointerException = false;
        try {
            ResourceUtil.getParent(null, 4);
        } catch (NullPointerException e) {
            // Expected exception
            caughtNullPointerException = true;
        } catch (Exception e) {
            fail("Expected NullPointerException, but caught " +
                    e.getClass().getName() + " instead.");
        }
        if (!caughtNullPointerException) {
            fail("Expected NullPointerException, but no exception was thrown.");
        }

        boolean caughtIllegalArgumentException = false;
        try {
            ResourceUtil.getParent("/a/b", -2);
        } catch (IllegalArgumentException e) {
            // Expected exception
            caughtIllegalArgumentException = true;
        } catch (Exception e) {
            fail("Expected IllegalArgumentException, but caught " +
                    e.getClass().getName() + " instead.");
        }
        if (!caughtIllegalArgumentException) {
            fail("Expected IllegalArgumentException, but no exception was thrown.");
        }

        assertNull(ResourceUtil.getParent("/a", 4));
        assertNull(ResourceUtil.getParent("/", 1));
        assertNull(ResourceUtil.getParent("b/c", 2));
        assertNull(ResourceUtil.getParent("/b/..", 1));
        assertNull(ResourceUtil.getParent("b", 1));
        assertNull(ResourceUtil.getParent("", 3));
        assertNull(ResourceUtil.getParent("/..", 1));
        assertNull(ResourceUtil.getParent("security:/b", 2));
        assertNull(ResourceUtil.getParent("/b///", 2));

        assertEquals("", ResourceUtil.getParent("", 0));
        assertEquals("b", ResourceUtil.getParent("b", 0));
        assertEquals("/", ResourceUtil.getParent("/", 0));
        assertEquals("/a/b", ResourceUtil.getParent("/a/b", 0));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b", 0));

        assertEquals("/", ResourceUtil.getParent("/b", 1));
        assertEquals("b", ResourceUtil.getParent("b/c", 1));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d", 1));
        assertEquals("/b/c", ResourceUtil.getParent("/b/c/d", 1));
        assertEquals("security:/", ResourceUtil.getParent("security:/b", 1));
        assertEquals("security:/b", ResourceUtil.getParent("security:/b/c", 1));
        assertEquals("security:/b/c", ResourceUtil.getParent("security:/b/c/d", 1));

        assertEquals("b", ResourceUtil.getParent("b/c/d", 2));
        assertEquals("b/c", ResourceUtil.getParent("b/c/d/e", 2));
        assertEquals("/", ResourceUtil.getParent("/b/c/d", 3));
        assertEquals("/", ResourceUtil.getParent("/b///", 1));
    }

    @Test public void testIsA() {
        assertFalse(ResourceUtil.isA(null, "something"));
    }

    @Test public void testFindResourceSuperType() {
        assertNull(ResourceUtil.findResourceSuperType(null));
    }
}
