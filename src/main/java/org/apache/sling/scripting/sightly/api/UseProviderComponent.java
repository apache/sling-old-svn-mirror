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

package org.apache.sling.scripting.sightly.api;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.felix.scr.annotations.Activate;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import aQute.bnd.annotation.ConsumerType;

/**
 * Component-based use provider
 */
@ConsumerType
public abstract class UseProviderComponent implements UseProvider {

    public static final String PRIORITY = "org.apache.sling.scripting.sightly.api.use.priority";

    private int priority = DEFAULT_PRIORITY;

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(UseProvider o) {
        if (this.priority < o.priority()) {
            return -1;
        } else if (this.priority == o.priority()) {
            return  0;
        }
        return 1;
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        priority = PropertiesUtil.toInteger(componentContext.getProperties().get(PRIORITY), DEFAULT_PRIORITY);
    }

    /**
     * Combine to bindings objects. Priority goes to latter bindings
     * @param former First map of bindings
     * @param latter Second, with greater visibility, map of bindings
     * @return the merging of the two maps
     */
    protected Bindings merge(Bindings former, Bindings latter) {
        Bindings bindings = new SimpleBindings();
        bindings.putAll(former);
        bindings.putAll(latter);
        return bindings;
    }
}
