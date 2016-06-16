/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine.extension;

import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.runtime.SlingRuntimeObjectModel;
import org.apache.sling.scripting.sightly.render.RenderContext;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtension.NAME, value = RuntimeFunction.JOIN)
})
public class JoinFilterExtension implements RuntimeExtension {

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.JOIN, arguments, 2);
        Object joinArgument = arguments[0];
        Collection<?> collection = SlingRuntimeObjectModel.toCollection(joinArgument);
        String joinString = SlingRuntimeObjectModel.toString(arguments[1]);
        return join(collection, joinString);
    }

    private String join(Collection<?> collection, String joinString) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            String element = SlingRuntimeObjectModel.toString(iterator.next());
            sb.append(element);
            if (iterator.hasNext()) {
                sb.append(joinString);
            }
        }
        return sb.toString();
    }

}
