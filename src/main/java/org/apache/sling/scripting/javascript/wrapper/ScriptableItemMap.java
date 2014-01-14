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
package org.apache.sling.scripting.javascript.wrapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptableItemMap extends ScriptableObject {

    public static final String CLASSNAME = "ItemMap";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, Item> items = new LinkedHashMap<String, Item>();

    public void jsConstructor(Object res) {
        if (res instanceof Iterator<?>) {
            Iterator<?> itemIterator = (Iterator<?>) res;
            while (itemIterator.hasNext()) {
                Item item = (Item) itemIterator.next();
                try {
                    items.put(item.getName(), item);
                } catch (RepositoryException re) {
                    log.error("ScriptableItemMap<init>: Cannot get name of item "
                        + item, re);
                }
            }
        }
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return getItem(index) != null;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return items.containsKey(name);
    }

    @Override
    public Object get(int index, Scriptable start) {
        Item item = getItem(index);
        if (item != null) {
            return ScriptRuntime.toObject(this, item);
        }

        return Undefined.instance;
    }

    @Override
    public Object get(String name, Scriptable start) {
        // special provision for the "length" property to simulate an array
        if ("length".equals(name)) {
            return ScriptRuntime.toNumber(this.items.keySet().size()+"");
        }

        Item item = items.get(name);
        Object result = Undefined.instance;
        if (item != null) {
            result = ScriptRuntime.toObject(this, item);
        }

        return result;
    }

    @Override
    public Object[] getIds() {
        return items.keySet().toArray();
    }

    private Item getItem(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }

        Iterator<Item> itemsIter = items.values().iterator();
        while (itemsIter.hasNext() && index > 0) {
            itemsIter.next();
            index--;
        }

        return itemsIter.hasNext() ? itemsIter.next() : null;
    }
}
