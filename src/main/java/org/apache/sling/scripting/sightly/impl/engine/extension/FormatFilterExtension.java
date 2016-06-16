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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;

@Component
@Service(RuntimeExtension.class)
@Properties({
        @Property(name = RuntimeExtension.NAME, value = RuntimeFunction.FORMAT)
})
public class FormatFilterExtension implements RuntimeExtension {

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\{\\d+}");

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.FORMAT, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String source = runtimeObjectModel.toString(arguments[0]);
        Object[] params = decodeParams(runtimeObjectModel, arguments[1]);
        return replace(runtimeObjectModel, source, params);
    }

    private Object[] decodeParams(RuntimeObjectModel runtimeObjectModel, Object paramObj) {
        if (runtimeObjectModel.isCollection(paramObj)) {
            return runtimeObjectModel.toCollection(paramObj).toArray();
        }
        return new Object[] {paramObj};
    }

    private String replace(RuntimeObjectModel runtimeObjectModel, String source, Object[] params) {
        Matcher matcher = PLACEHOLDER_REGEX.matcher(source);
        StringBuilder builder = new StringBuilder();
        int lastPos = 0;
        boolean matched = true;
        while (matched) {
            matched = matcher.find();
            if (matched) {
                String group = matcher.group();
                int paramIndex = Integer.parseInt(group.substring(1, group.length() - 1));
                String replacement = param(runtimeObjectModel, params, paramIndex);
                int matchStart = matcher.start();
                int matchEnd = matcher.end();
                builder.append(source, lastPos, matchStart).append(replacement);
                lastPos = matchEnd;
            }
        }
        builder.append(source, lastPos, source.length());
        return builder.toString();
    }

    private String param(RuntimeObjectModel runtimeObjectModel, Object[] params, int index) {
        if (index >= 0 && index < params.length) {
            return runtimeObjectModel.toString(params[index]);
        }
        return "";
    }
}
