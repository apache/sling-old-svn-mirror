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
package org.apache.sling.scripting.thymeleaf.internal.dialect;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingAddSelectorsAttrProcessor;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingIncludeAttrProcessor;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingReplaceSelectorsAttrProcessor;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingReplaceSuffixAttrProcessor;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingResourceTypeAttrProcessor;
import org.apache.sling.scripting.thymeleaf.internal.processor.attr.SlingUnwrapAttrProcessor;
import org.osgi.framework.Constants;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.processor.IProcessor;

@Component(
    immediate = true
)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling dialect for Sling Scripting Thymeleaf")
})
public final class SlingDialect extends AbstractDialect {

    public static final String PREFIX = "sling";

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public Set<IProcessor> getProcessors() {
        final Set<IProcessor> processors = new HashSet<IProcessor>();
        processors.add(new SlingAddSelectorsAttrProcessor());
        processors.add(new SlingIncludeAttrProcessor());
        processors.add(new SlingReplaceSelectorsAttrProcessor());
        processors.add(new SlingReplaceSuffixAttrProcessor());
        processors.add(new SlingResourceTypeAttrProcessor());
        processors.add(new SlingUnwrapAttrProcessor());
        return processors;
    }

}
