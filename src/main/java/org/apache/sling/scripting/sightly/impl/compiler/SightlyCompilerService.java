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

package org.apache.sling.scripting.sightly.impl.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.impl.compiler.debug.SanityChecker;
import org.apache.sling.scripting.sightly.impl.compiler.frontend.SimpleFrontend;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.CoalescingWrites;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.DeadCodeRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.SequenceStreamTransformer;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.SyntheticMapRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.UnusedVariableRemoval;
import org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce.ConstantFolding;
import org.apache.sling.scripting.sightly.impl.compiler.ris.CommandStream;
import org.apache.sling.scripting.sightly.impl.compiler.util.stream.PushStream;
import org.apache.sling.scripting.sightly.impl.filter.Filter;
import org.apache.sling.scripting.sightly.impl.html.dom.HtmlParserService;
import org.apache.sling.scripting.sightly.impl.plugin.Plugin;
import org.osgi.service.component.ComponentContext;

/**
 * Implementation for the Sightly compiler
 */
@Component
@Service(SightlyCompilerService.class)
@References({
        @Reference(
                policy = ReferencePolicy.DYNAMIC,
                referenceInterface = Filter.class,
                name = "filterService",
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
        ),
        @Reference(
                policy = ReferencePolicy.DYNAMIC,
                referenceInterface = Plugin.class,
                name = "pluginService",
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
        )
})
public class SightlyCompilerService {

    private List<Filter> filters = new ArrayList<Filter>();
    private List<Plugin> plugins = new ArrayList<Plugin>();
    private static final Lock FILTERS_LOCK = new ReentrantLock();
    private static final Lock PLUGINS_LOCK = new ReentrantLock();

    private volatile StreamTransformer optimizer;
    private volatile CompilerFrontend frontend;
    private volatile boolean initialised = false;

    @Reference
    protected HtmlParserService htmlParserService;

    /**
     * Compile the given markup source and feed it to the given backend
     * @param source the HTML source code
     * @param backend the backend that will process the command stream from the source
     */
    public void compile(String source, CompilerBackend backend) {
        initIfNeeded();
        PushStream stream = new PushStream();
        SanityChecker.attachChecker(stream);
        CommandStream optimizedStream = optimizer.transform(stream);
        //optimizedStream.addHandler(LoggingHandler.INSTANCE);
        backend.handle(optimizedStream);
        frontend.compile(stream, source);
    }

    private void initIfNeeded() {
        if (!initialised) {
            synchronized (this) {
                if (!initialised) {
                    ArrayList<StreamTransformer> transformers = new ArrayList<StreamTransformer>();
                    transformers.add(ConstantFolding.transformer());
                    transformers.add(DeadCodeRemoval.transformer());
                    transformers.add(SyntheticMapRemoval.TRANSFORMER);
                    transformers.add(UnusedVariableRemoval.TRANSFORMER);
                    transformers.add(CoalescingWrites.TRANSFORMER);
                    optimizer = new SequenceStreamTransformer(transformers);
                    initialised = true;
                }
            }
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        initialised = false;
        reloadFrontend();
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void bindFilterService(Filter filter, Map<String, Object> properties) {
        FILTERS_LOCK.lock();
        try {
            filters = add(filters, filter);
            reloadFrontend();
        } finally {
            FILTERS_LOCK.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void unbindFilterService(Filter filter, Map<String, Object> properties) {
        FILTERS_LOCK.lock();
        try {
            filters = remove(filters, filter);
            reloadFrontend();
        } finally {
            FILTERS_LOCK.unlock();
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    protected void bindPluginService(Plugin plugin, Map<String, Object> properties) {
        PLUGINS_LOCK.lock();
        try {
            plugins = add(plugins, plugin);
            reloadFrontend();
        } finally {
            PLUGINS_LOCK.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void unbindPluginService(Plugin plugin, Map<String, Object> properties) {
        PLUGINS_LOCK.lock();
        try {
            plugins = remove(plugins, plugin);
            reloadFrontend();
        } finally {
            PLUGINS_LOCK.unlock();
        }
    }

    private void reloadFrontend() {
        frontend = new SimpleFrontend(htmlParserService, plugins, filters);
    }

    private static <T> List<T> add(List<T> list, T item) {
        ArrayList<T> result = new ArrayList<T>(list);
        result.add(item);
        return result;
    }

    private static <T> List<T> remove(List<T> list, T item) {
        ArrayList<T> result = new ArrayList<T>(list);
        result.remove(item);
        return result;
    }
}
