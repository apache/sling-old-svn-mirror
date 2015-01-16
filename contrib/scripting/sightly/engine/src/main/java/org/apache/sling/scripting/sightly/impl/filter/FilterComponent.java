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

package org.apache.sling.scripting.sightly.impl.filter;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

/**
 * Filters implemented as components.
 */
public abstract class FilterComponent implements Filter {

    public static final String PRIORITY = "org.apache.sling.scripting.sightly.impl.filter.priority";
    public static final int DEFAULT_PRIORITY = 100;

    private int priority = DEFAULT_PRIORITY;

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(Filter o) {
        if (this.priority < o.priority()) {
            return -1;
        } else if (this.priority == o.priority()) {
            return  0;
        }
        return 1;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        priority = PropertiesUtil.toInteger(properties.get(PRIORITY), DEFAULT_PRIORITY);
    }
}
