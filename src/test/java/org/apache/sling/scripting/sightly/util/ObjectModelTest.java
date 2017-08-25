/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.util.ObjectModel;
import org.apache.sling.scripting.sightly.testobjects.Person;
import org.apache.sling.scripting.sightly.testobjects.internal.AdultFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectModelTest {

    @Test
    public void testToBoolean() {
        assertFalse(ObjectModel.toBoolean(null));
        assertFalse(ObjectModel.toBoolean(0));
        assertTrue(ObjectModel.toBoolean(123456));
        assertFalse(ObjectModel.toBoolean(""));
        assertFalse(ObjectModel.toBoolean("FalSe"));
        assertFalse(ObjectModel.toBoolean("false"));
        assertFalse(ObjectModel.toBoolean("FALSE"));
        assertTrue(ObjectModel.toBoolean("true"));
        assertTrue(ObjectModel.toBoolean("TRUE"));
        assertTrue(ObjectModel.toBoolean("TrUE"));
        Integer[] testArray = new Integer[] {1, 2, 3};
        List testList = Arrays.asList(testArray);
        assertTrue(ObjectModel.toBoolean(testArray));
        assertFalse(ObjectModel.toBoolean(new Integer[]{}));
        assertTrue(ObjectModel.toBoolean(testList));
        assertFalse(ObjectModel.toBoolean(Collections.emptyList()));
        Map<String, Integer> map = new HashMap<String, Integer>() {{
            put("one", 1);
            put("two", 2);
        }};
        assertTrue(ObjectModel.toBoolean(map));
        assertFalse(ObjectModel.toBoolean(Collections.EMPTY_MAP));
        assertTrue(ObjectModel.toBoolean(testList.iterator()));
        assertFalse(ObjectModel.toBoolean(Collections.EMPTY_LIST.iterator()));
        assertTrue(ObjectModel.toBoolean(new Bag<>(testArray)));
        assertFalse(ObjectModel.toBoolean(new Bag<>(new Integer[]{})));
        assertTrue(ObjectModel.toBoolean(new Date()));
    }

    @Test
    public void testToNumber() {
        assertEquals(1, ObjectModel.toNumber(1));
        assertEquals(1, ObjectModel.toNumber("1"));
        assertNull(ObjectModel.toNumber(null));
        assertNull(ObjectModel.toNumber("1-2"));
    }

    @Test
    public void testToString() {
        assertEquals("", ObjectModel.toString(null));
        assertEquals("1", ObjectModel.toString("1"));
        assertEquals("1", ObjectModel.toString(1));
        assertEquals("ADD", ObjectModel.toString(BinaryOperator.ADD));
        Integer[] testArray = new Integer[] {1, 2, 3};
        List testList = Arrays.asList(testArray);
        assertEquals("1,2,3", ObjectModel.toString(testList));
    }

    @Test
    public void testToCollection() {
        assertTrue(ObjectModel.toCollection(null).isEmpty());
        assertTrue(ObjectModel.toCollection(new StringBuilder()).isEmpty());
        Integer[] testArray = new Integer[] {1, 2, 3};
        List testList = Arrays.asList(testArray);
        Map<String, Integer> map = new HashMap<String, Integer>() {{
            put("one", 1);
            put("two", 2);
        }};
        assertEquals(testList, ObjectModel.toCollection(testArray));
        assertEquals(testList, ObjectModel.toCollection(testList));
        assertEquals(map.keySet(), ObjectModel.toCollection(map));
        Vector vector = new Vector(testList);
        assertEquals(testList, ObjectModel.toCollection(vector.elements()));
        assertEquals(testList, ObjectModel.toCollection(testList.iterator()));
        assertEquals(testList, ObjectModel.toCollection(new Bag<>(testArray)));
        String stringObject = "test";
        Integer numberObject = 1;
        Collection stringCollection = ObjectModel.toCollection(stringObject);
        assertTrue(stringCollection.size() == 1 && stringCollection.contains(stringObject));
        Collection numberCollection = ObjectModel.toCollection(numberObject);
        assertTrue(numberCollection.size() == 1 && numberCollection.contains(numberObject));
    }

    @Test
    public void testCollectionToString() {
        Integer[] testArray = new Integer[] {1, 2, 3};
        List testList = Arrays.asList(testArray);
        assertEquals("1,2,3", ObjectModel.collectionToString(testList));
    }

    @Test
    public void testFromIterator() {
        Integer[] testArray = new Integer[] {1, 2, 3};
        List testList = Arrays.asList(testArray);
        assertEquals(testList, ObjectModel.fromIterator(testList.iterator()));
    }

    @Test
    public void testResolveProperty() {
        assertNull(ObjectModel.resolveProperty(null, null));
        Integer[] testArray = new Integer[] {1, 2, 3};
        assertEquals(2, ObjectModel.resolveProperty(testArray, 1));
        assertNull(ObjectModel.resolveProperty(testArray, 3));
        assertNull(ObjectModel.resolveProperty(testArray, -1));
        List<Integer> testList = Arrays.asList(testArray);
        assertEquals(2, ObjectModel.resolveProperty(testList, 1));
        assertNull(ObjectModel.resolveProperty(testList, 3));
        assertNull(ObjectModel.resolveProperty(testList, -1));
        Map<String, Integer> map = new HashMap<String, Integer>() {{
            put("one", 1);
            put("two", 2);
        }};
        assertEquals(1, ObjectModel.resolveProperty(map, "one"));
        assertNull(ObjectModel.resolveProperty(map, null));
        assertNull(ObjectModel.resolveProperty(map, ""));
        Person johnDoe = AdultFactory.createAdult("John", "Doe");
        assertEquals("Expected to be able to access public static final constants.", 1l, ObjectModel.resolveProperty(johnDoe, "CONSTANT"));
        assertNull("Did not expect to be able to access public fields from package protected classes.", ObjectModel.resolveProperty(johnDoe,
                "TODAY"));
        assertEquals("Expected to be able to access an array's length property.", 3, ObjectModel.resolveProperty(testArray, "length"));
        assertNotNull("Expected not null result for invocation of interface method on implementation class.",
                ObjectModel.resolveProperty(johnDoe, "lastName"));
        assertNull("Expected null result for public method available on implementation but not exposed by interface.", ObjectModel
                .resolveProperty(johnDoe, "fullName"));
        assertNull("Expected null result for inexistent method.", ObjectModel.resolveProperty(johnDoe, "nomethod"));
    }


    private class Bag<T> implements Iterable<T> {

        private T[] backingArray;

        public Bag(T[] array) {
            this.backingArray = array;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                int index = 0;

                @Override
                public boolean hasNext() {
                    return index < backingArray.length;
                }

                @Override
                public T next() {
                    return backingArray[index++];
                }

                @Override
                public void remove() {

                }
            };
        }
    }
}
