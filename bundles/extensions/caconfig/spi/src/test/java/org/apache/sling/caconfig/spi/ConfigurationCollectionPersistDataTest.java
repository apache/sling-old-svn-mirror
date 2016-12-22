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
package org.apache.sling.caconfig.spi;

import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationCollectionPersistDataTest {

    @Mock
    private Map<String,Object> props;
    
    @Test
    public void testProperties() {
        Collection<ConfigurationPersistData> items = ImmutableList.of(
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item1"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item2"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item3"));

        ConfigurationCollectionPersistData underTest = new ConfigurationCollectionPersistData(items)
                .properties(props);
        
        assertSame(items, underTest.getItems());
        assertSame(props, underTest.getProperties());
    }

    @Test(expected=ConfigurationPersistenceException.class)
    public void testItemsDuplicateKeys() {
        Collection<ConfigurationPersistData> itemList = ImmutableList.of(
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item1"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item2"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item1"));

        new ConfigurationCollectionPersistData(itemList);
    }

    @Test(expected=ConfigurationPersistenceException.class)
    public void testItemsMissingItemName() {
        Collection<ConfigurationPersistData> itemList = ImmutableList.of(
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item1"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item2"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()));

        new ConfigurationCollectionPersistData(itemList);
    }

    @Test(expected=ConfigurationPersistenceException.class)
    public void testItemsInvalidItemNAme() {
        Collection<ConfigurationPersistData> itemList = ImmutableList.of(
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item1"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item2"),
                new ConfigurationPersistData(ImmutableMap.<String, Object>of()).collectionItemName("item #1"));

        new ConfigurationCollectionPersistData(itemList);
    }

}
