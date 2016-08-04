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

import org.apache.sling.scripting.thymeleaf.internal.processor.SlingIncludeAttributeTagProcessor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.processor.IProcessor;

@Component(
    service = IDialect.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Sling Dialect for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
public final class SlingDialect extends AbstractProcessorDialect {

    public static final String NAME = "Sling";

    public static final String PREFIX = "sling";

    public SlingDialect() {
        super(NAME, PREFIX, 0);
    }

    @Override
    public Set<IProcessor> getProcessors(final String prefix) {
        final Set<IProcessor> processors = new HashSet<IProcessor>();
        processors.add(new SlingIncludeAttributeTagProcessor(prefix));
        return processors;
    }

}
