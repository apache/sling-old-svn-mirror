/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.freemarker.internal;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
    service = {
        TemplateModel.class
    },
    property = {
        "name=adaptTo"
    }
)
public class AdaptToMethod implements TemplateMethodModelEx {

    @Reference
    private AdapterManager adapterManager;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    public AdaptToMethod() {
    }

    @Override
    public Object exec(final List arguments) throws TemplateModelException {
        if (arguments.size() != 2) {
            throw new TemplateModelException("Wrong number of arguments");
        }
        try {
            final String classname = arguments.get(1).toString();
            final Class<?> clazz = dynamicClassLoaderManager.getDynamicClassLoader().loadClass(classname);
            final TemplateModel templateModel = (TemplateModel) arguments.get(0);
            final Object adaptable = DeepUnwrap.unwrap(templateModel);
            return adapterManager.getAdapter(adaptable, clazz);
        } catch (Exception e) {
            throw new TemplateModelException(e);
        }
    }

}
