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

package org.apache.sling.scripting.sightly.compiler;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.MarkupParser;
import org.apache.sling.scripting.sightly.api.ObjectModel;
import org.apache.sling.scripting.sightly.compiler.api.SightlyCompiler;
import org.apache.sling.scripting.sightly.compiler.api.plugin.Plugin;
import org.apache.sling.scripting.sightly.compiler.frontend.SimpleFrontend;
import org.apache.sling.scripting.sightly.compiler.optimization.CoalescingWrites;
import org.apache.sling.scripting.sightly.compiler.optimization.DeadCodeRemoval;
import org.apache.sling.scripting.sightly.compiler.optimization.SequenceStreamTransformer;
import org.apache.sling.scripting.sightly.compiler.optimization.StreamTransformer;
import org.apache.sling.scripting.sightly.compiler.optimization.SyntheticMapRemoval;
import org.apache.sling.scripting.sightly.compiler.optimization.UnusedVariableRemoval;
import org.apache.sling.scripting.sightly.compiler.optimization.reduce.ConstantFolding;
import org.apache.sling.scripting.sightly.common.Dynamic;

/**
 * Implementation for the Sightly compiler
 */
@Component(metatype = true, label = "Apache Sling Scripting Sightly Java Compiler", description = "The Apache Sling Sightly Java Compiler" +
        " is responsible for translating Sightly scripts into Java source code.")
@Service(SightlyCompiler.class)
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
@Properties({
        @Property(
                name = SightlyCompilerImpl.CONSTANT_FOLDING_OPT,
                boolValue = true,
                label = "Constant Folding",
                description = "Optimises expressions by evaluating static parts in advance."
        ),
        @Property(
                name = SightlyCompilerImpl.DEAD_CODE_OPT,
                boolValue = true,
                label = "Dead Code Removal",
                description = "Optimises expressions evaluations by removing code that's under the false branch of an if."
        ),
        @Property(
                name = SightlyCompilerImpl.SYNTHETIC_MAP_OPT,
                boolValue = true,
                label = "Synthetic Map Removal",
                description = "Optimises expressions by replacing calls to map.get with the actual object from the map if the only usages" +
                        " of that map are value retrievals."
        ),
        @Property(
                name = SightlyCompilerImpl.UNUSED_VAR_OPT,
                boolValue = true,
                label = "Unused Variables Removal",
                description = "Optimises expression evaluations by removing unused variables from the generated Java source code."
        ),
        @Property(
                name = SightlyCompilerImpl.COALESCING_WRITES_OPT,
                boolValue = true,
                label = "Coalescing Writes",
                description = "Optimises expression evaluations by merging together consecutive writes to the Java source code files."
        )
})
public class SightlyCompilerImpl extends BaseCompiler {

    public static final String CONSTANT_FOLDING_OPT = "org.apache.sling.scripting.sightly.compiler.constantFolding";
    public static final String DEAD_CODE_OPT = "org.apache.sling.scripting.sightly.compiler.deadCodeRemoval";
    public static final String SYNTHETIC_MAP_OPT = "org.apache.sling.scripting.sightly.compiler.syntheticMapRemoval";
    public static final String UNUSED_VAR_OPT = "org.apache.sling.scripting.sightly.compiler.unusedVarRemoval";
    public static final String COALESCING_WRITES_OPT = "org.apache.sling.scripting.sightly.compiler.coalescingWrites";

    private List<Filter> filters = new ArrayList<Filter>();
    private List<Plugin> plugins = new ArrayList<Plugin>();

    private volatile StreamTransformer optimizer;
    private volatile CompilerFrontend frontend;

    @Reference
    protected MarkupParser markupParser;

    @Reference
    protected ObjectModel objectModel;

    @Override
    protected StreamTransformer getOptimizer() {
        return optimizer;
    }

    @Override
    protected CompilerFrontend getFrontend() {
        return frontend;
    }

    @Activate
    protected void activate(ComponentContext context) {
        Dictionary properties = context.getProperties();
        reloadOptimizations(properties);
        reloadFrontend();
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void bindFilterService(Filter filter, Map<String, Object> properties) {
        synchronized(filters) {
            filters = add(filters, filter);
            reloadFrontend();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void unbindFilterService(Filter filter, Map<String, Object> properties) {
        synchronized (filters) {
            filters = remove(filters, filter);
            reloadFrontend();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void bindPluginService(Plugin plugin, Map<String, Object> properties) {
        synchronized (plugins) {
            plugins = add(plugins, plugin);
            reloadFrontend();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void unbindPluginService(Plugin plugin, Map<String, Object> properties) {
        synchronized (plugins) {
            plugins = remove(plugins, plugin);
            reloadFrontend();
        }
    }

    private void reloadOptimizations(Dictionary properties) {
        ArrayList<StreamTransformer> transformers = new ArrayList<StreamTransformer>();
        Dynamic dynamic = new Dynamic(objectModel);
        activateOptimization(CONSTANT_FOLDING_OPT, transformers, properties, ConstantFolding.transformer(dynamic));
        activateOptimization(DEAD_CODE_OPT, transformers, properties, DeadCodeRemoval.transformer(dynamic));
        activateOptimization(SYNTHETIC_MAP_OPT, transformers, properties, SyntheticMapRemoval.TRANSFORMER);
        activateOptimization(UNUSED_VAR_OPT, transformers, properties, UnusedVariableRemoval.TRANSFORMER);
        activateOptimization(COALESCING_WRITES_OPT, transformers, properties, CoalescingWrites.TRANSFORMER);
        optimizer = new SequenceStreamTransformer(transformers);
    }

    private void activateOptimization(String option, ArrayList<StreamTransformer> transformers,
                                      Dictionary dictionary, StreamTransformer transformer) {
        boolean activate = PropertiesUtil.toBoolean(dictionary.get(option), true);
        if (activate) {
            transformers.add(transformer);
        }
    }

    private void reloadFrontend() {
        frontend = new SimpleFrontend(markupParser, plugins, filters);
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
