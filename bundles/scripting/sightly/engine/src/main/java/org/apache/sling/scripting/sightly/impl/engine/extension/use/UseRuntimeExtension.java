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
package org.apache.sling.scripting.sightly.impl.engine.extension.use;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.engine.extension.ExtensionUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.render.RuntimeObjectModel;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Runtime extension for the USE plugin
 */
@Component(
        service = RuntimeExtension.class,
        property = {
                RuntimeExtension.NAME + "=" + RuntimeFunction.USE
        }
)
public class UseRuntimeExtension implements RuntimeExtension {

    private final Map<ServiceReference, UseProvider> providersMap = new ConcurrentSkipListMap<>();

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        ExtensionUtils.checkArgumentCount(RuntimeFunction.USE, arguments, 2);
        RuntimeObjectModel runtimeObjectModel = renderContext.getObjectModel();
        String identifier = runtimeObjectModel.toString(arguments[0]);
        if (StringUtils.isEmpty(identifier)) {
            throw new SightlyException("data-sly-use needs to be passed an identifier");
        }
        Map<String, Object> useArgumentsMap = runtimeObjectModel.toMap(arguments[1]);
        Bindings useArguments = new SimpleBindings(Collections.unmodifiableMap(useArgumentsMap));
        ArrayList<UseProvider> providers = new ArrayList<>(providersMap.values());
        ListIterator<UseProvider> iterator = providers.listIterator(providers.size());
        while (iterator.hasPrevious()) {
            UseProvider provider = iterator.previous();
            ProviderOutcome outcome = provider.provide(identifier, renderContext, useArguments);
            Throwable failureCause;
            if (outcome.isSuccess()) {
                return outcome.getResult();
            } else if ((failureCause = outcome.getCause()) != null) {
                throw new SightlyException("Identifier " + identifier + " cannot be correctly instantiated by the Use API", failureCause);
            }
        }
        throw new SightlyException("No use provider could resolve identifier " + identifier);
    }

    // OSGi ################################################################################################################################
    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            service = UseProvider.class,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    private void bindUseProvider(ServiceReference serviceReference) {
        BundleContext bundleContext = serviceReference.getBundle().getBundleContext();
        providersMap.put(serviceReference, (UseProvider) bundleContext.getService(serviceReference));
    }

    private void unbindUseProvider(ServiceReference serviceReference) {
        providersMap.remove(serviceReference);
    }
}
