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
package org.apache.sling.api.wrappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Assert;
import org.junit.Test;

public class CompositeValueMapTest {

    // Test property names
    private static final String PROP_NAME_UNCHANGED = "unchangedProp";
    private static final String PROP_NAME_OVERRIDDEN = "overriddenProp";
    private static final String PROP_NAME_NEW_TYPE = "newTypeProp";
    private static final String PROP_NAME_ADDED = "addedProp";
    private static final String PROP_NAME_DOES_NOT_EXIST = "doesNotExistProp";

    // Default resource's property values
    private static final String PROP_DEFAULT_UNCHANGED = "Default value of property '" + PROP_NAME_UNCHANGED + "'";
    private static final String PROP_DEFAULT_OVERRIDDEN = "Default value of property '" + PROP_NAME_OVERRIDDEN + "'";
    private static final String PROP_DEFAULT_NEW_TYPE = "10";

    // Extended resource's property values
    private static final String PROP_EXTENDED_OVERRIDDEN = "Extended value of property '" + PROP_NAME_OVERRIDDEN + "'";
    private static final Long PROP_EXTENDED_NEW_TYPE = 10L;
    private static final String PROP_EXTENDED_ADDED = "Extended value of property '" + PROP_NAME_ADDED + "'";

    private Map<String, Object> defaultProps = getDefaultProps();
    private Map<String, Object> extendedProps = getExtendedProps();

    @Test
    public void testMerge() throws Exception {
        // Get value map for extended node using default node as defaults
        CompositeValueMap valueMap = new CompositeValueMap(
                getExtendedProps(),
                getDefaultProps()
        );

        Set<CompositeValueMapTestResult> expectations = new HashSet<CompositeValueMapTestResult>();
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_UNCHANGED));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_OVERRIDDEN));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_NEW_TYPE, false, PROP_EXTENDED_NEW_TYPE.getClass()));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_ADDED));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_DOES_NOT_EXIST));

        verifyResults(valueMap, expectations);
    }

    @Test
    public void testMergeNoDefaults() throws Exception {
        // Get value map for extended node using an empty default
        CompositeValueMap valueMap = new CompositeValueMap(
                getExtendedProps(),
                null
        );

        Set<CompositeValueMapTestResult> expectations = new HashSet<CompositeValueMapTestResult>();
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_UNCHANGED, true)); // Property won't exist as there is no default
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_OVERRIDDEN));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_NEW_TYPE, false, PROP_EXTENDED_NEW_TYPE.getClass()));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_ADDED));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_DOES_NOT_EXIST));

        verifyResults(valueMap, expectations);
    }

    @Test
    public void testOverride() throws Exception {
        // Get value map for extended node using default node as defaults
        // and override only mode
        CompositeValueMap valueMap = new CompositeValueMap(
                getExtendedProps(),
                getDefaultProps(),
                false
        );

        Set<CompositeValueMapTestResult> expectations = new HashSet<CompositeValueMapTestResult>();
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_UNCHANGED));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_OVERRIDDEN));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_NEW_TYPE, false, PROP_EXTENDED_NEW_TYPE.getClass()));
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_ADDED, true)); // Property won't exist as there is no default and it's an override
        expectations.add(new CompositeValueMapTestResult(PROP_NAME_DOES_NOT_EXIST));

        verifyResults(valueMap, expectations);
    }

    @Test
    public void testOverrideNoDefaults() throws Exception {
        // Get value map for extended node using an empty default
        // and override only mode
        CompositeValueMap valueMap = new CompositeValueMap(
                getExtendedProps(),
                null,
                false
        );

        Assert.assertTrue("Final map should be empty", valueMap.isEmpty());
    }

    private ValueMap getDefaultProps() {
        final Map<String, Object> defaultProps = new HashMap<String, Object>();

        defaultProps.put(PROP_NAME_UNCHANGED, PROP_DEFAULT_UNCHANGED);
        defaultProps.put(PROP_NAME_OVERRIDDEN, PROP_DEFAULT_OVERRIDDEN);
        defaultProps.put(PROP_NAME_NEW_TYPE, PROP_DEFAULT_NEW_TYPE);

        return new ValueMapDecorator(defaultProps);
    }

    private ValueMap getExtendedProps() {
        final Map<String, Object> defaultProps = new HashMap<String, Object>();

        defaultProps.put(PROP_NAME_OVERRIDDEN, PROP_EXTENDED_OVERRIDDEN);
        defaultProps.put(PROP_NAME_NEW_TYPE, PROP_EXTENDED_NEW_TYPE);
        defaultProps.put(PROP_NAME_ADDED, PROP_EXTENDED_ADDED);

        return new ValueMapDecorator(defaultProps);
    }

    private void verifyResults(CompositeValueMap valueMap, Set<CompositeValueMapTestResult> expectations) {
        Map<String, Object> expectedMap = new HashMap<String, Object>();

        int expectedSize = 0;
        for (CompositeValueMapTestResult testResult : expectations) {
            String property = testResult.propertyName;

            if (testResult.doesNotExist()) {
                Assert.assertFalse("Property '" + property + "' should NOT exist", valueMap.containsKey(property));

            } else if (testResult.shouldBeDeleted()) {
                Assert.assertFalse("Property '" + property + "' should NOT be part of the final map", valueMap.containsKey(property));
                Assert.assertNull("Property '" + property + "' should be null", valueMap.get(property));

            } else {
                Assert.assertTrue("Property '" + property + "' should be part of the final map", valueMap.containsKey(property));
                expectedSize++;

                if (testResult.shouldBeUnchanged()) {
                    Assert.assertEquals("Property '" + property + "' should NOT have changed", testResult.defaultValue, valueMap.get(property));
                    expectedMap.put(property, testResult.defaultValue);
                }

                if (testResult.shouldBeOverriden()) {
                    Assert.assertEquals("Property '" + property + "' should have changed", testResult.extendedValue, valueMap.get(property));
                    expectedMap.put(property, testResult.extendedValue);
                }

                if (testResult.shouldHaveNewType()) {
                    Assert.assertTrue("Type of property '" + property + "' should have changed", valueMap.get(property).getClass().equals(testResult.expectedNewType));
                    expectedMap.put(property, testResult.extendedValue);
                }

                if (testResult.shouldBeAdded()) {
                    Assert.assertEquals("Property '" + property + "' should have been added", testResult.extendedValue, valueMap.get(property));
                    expectedMap.put(property, testResult.extendedValue);
                }
            }
        }

        Assert.assertEquals("Final map size does NOT match", expectedSize, valueMap.size());
        Assert.assertEquals("Final map entries do NOT match", expectedMap.entrySet(), valueMap.entrySet());
        Assert.assertEquals("Final map keys do NOT match", expectedMap.keySet(), valueMap.keySet());
        Assert.assertTrue("Final map values do NOT match expected: <" + expectedMap.values() + "> but was: <" + valueMap.values() + ">", CollectionUtils.isEqualCollection(expectedMap.values(), valueMap.values()));
    }

    /**
     * <code>CompositeValueMapTestResult</code> is an internal helper to analyze
     * test result and check if the value retrieved from the map matches the
     * expected value.
     */
    private class CompositeValueMapTestResult {
        private final String propertyName;
        private final Object defaultValue;
        private final Object extendedValue;
        private final boolean shouldBeDeleted;
        private final Class expectedNewType;

        private CompositeValueMapTestResult(String propertyName) {
            this(propertyName, false);
        }

        private CompositeValueMapTestResult(String propertyName, boolean shouldBeDeleted) {
            this(propertyName, shouldBeDeleted, null);
        }

        private CompositeValueMapTestResult(String propertyName, boolean shouldBeDeleted, Class expectedNewType) {
            this.propertyName = propertyName;
            this.defaultValue = defaultProps.get(propertyName);
            this.extendedValue = extendedProps.get(propertyName);
            this.shouldBeDeleted = shouldBeDeleted;
            this.expectedNewType = expectedNewType;
        }

        /**
         * Checks if the value should not have changed
         * @return <code>true</code> if the value should not have changed
         */
        boolean shouldBeUnchanged() {
            return defaultValue != null && extendedValue == null;
        }

        /**
         * Checks if the value should have been overridden
         * @return <code>true</code> if the value should have been overridden
         */
        boolean shouldBeOverriden() {
            return defaultValue != null && extendedValue != null;
        }

        /**
         * Checks if the value should have a new type
         * @return <code>true</code> if the value should have a new type
         */
        boolean shouldHaveNewType() {
            return expectedNewType != null;
        }

        /**
         * Checks if the property should have been added
         * @return <code>true</code> if the property should have been added
         */
        boolean shouldBeAdded() {
            return defaultValue == null && extendedValue != null;
        }

        /**
         * Checks if the property should have been deleted
         * @return <code>true</code> if the property should have been deleted
         */
        boolean shouldBeDeleted() {
            return shouldBeDeleted;
        }

        /**
         * Checks if the property should not exist
         * @return <code>true</code> if the property should not exist
         */
        boolean doesNotExist() {
            return defaultValue == null && extendedValue == null;
        }

    }

}
