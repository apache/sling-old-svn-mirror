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
package org.apache.sling.models.impl;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ViaProvider;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.servlet.Servlet;

@SuppressWarnings("deprecation")
public class ModelConfigurationPrinter {

    private static final String EXPORT_SERVLET_FILTER = "(" + ModelPackageBundleListener.PROP_EXPORTER_SERVLET_CLASS + "=*)";

    private final ModelAdapterFactory modelAdapterFactory;
    private final BundleContext bundleContext;
    private final AdapterImplementations adapterImplementations;

    ModelConfigurationPrinter(ModelAdapterFactory modelAdapterFactory, BundleContext bundleContext, AdapterImplementations adapterImplementations) {
        this.modelAdapterFactory = modelAdapterFactory;
        this.bundleContext = bundleContext;
        this.adapterImplementations = adapterImplementations;
    }

    public void printConfiguration(PrintWriter printWriter) {
        
        // injectors
        printWriter.println("Sling Models Injectors:");
        Collection<Injector> injectors = modelAdapterFactory.getInjectors();
        if (injectors.isEmpty()) {
            printWriter.println("none");
        } else {
            for (Injector injector : injectors) {
                printWriter.printf("%s - %s", injector.getName(), injector.getClass().getName());
                printWriter.println();
            }
        }
        printWriter.println();
        
        // inject annotations processor factories
        printWriter.println("Sling Models Inject Annotation Processor Factories:");
        Collection<InjectAnnotationProcessorFactory> factories = modelAdapterFactory.getInjectAnnotationProcessorFactories();
        Collection<InjectAnnotationProcessorFactory2> factories2 = modelAdapterFactory.getInjectAnnotationProcessorFactories2();
        Collection<StaticInjectAnnotationProcessorFactory> staticFactories = modelAdapterFactory.getStaticInjectAnnotationProcessorFactories();
        if ((factories.isEmpty())
                && (factories2.isEmpty())
                && (staticFactories.isEmpty())) {
            printWriter.println("none");
        } else {
            for (StaticInjectAnnotationProcessorFactory factory : staticFactories) {
                printWriter.printf("%s", factory.getClass().getName());
                printWriter.println();
            }
            for (InjectAnnotationProcessorFactory2 factory : factories2) {
                printWriter.printf("%s", factory.getClass().getName());
                printWriter.println();
            }
            for (InjectAnnotationProcessorFactory factory : factories) {
                printWriter.printf("%s", factory.getClass().getName());
                printWriter.println();
            }
        }
        printWriter.println();
        
        // implementation pickers
        printWriter.println("Sling Models Implementation Pickers:");
        ImplementationPicker[] pickers = modelAdapterFactory.getImplementationPickers();
        if (pickers == null || pickers.length == 0) {
            printWriter.println("none");
        } else {
            for (ImplementationPicker picker : pickers) {
                printWriter.printf("%s", picker.getClass().getName());
                printWriter.println();
            }
        }

        printWriter.println();

        // implementation pickers
        printWriter.println("Sling Models Via Providers:");
        Map<Class<? extends ViaProviderType>, ViaProvider> viaProviders = modelAdapterFactory.getViaProviders();
        if (viaProviders == null || viaProviders.size() == 0) {
            printWriter.println("none");
        } else {
            for (Map.Entry<Class<? extends ViaProviderType>, ViaProvider> entry : viaProviders.entrySet()) {
                printWriter.printf("%s (Type: %s)", entry.getValue().getClass().getName(), entry.getKey().getName());
                printWriter.println();
            }
        }


        printWriter.println();

        // models bound to resource types
        printWriter.println("Sling Models Bound to Resource Types *For Resources*:");
        for (Map.Entry<String, Class<?>> entry : adapterImplementations.getResourceTypeMappingsForResources().entrySet()) {
            printWriter.print(entry.getValue().getName());
            printWriter.print(" - ");
            printWriter.println(entry.getKey());
        }
        printWriter.println();

        printWriter.println("Sling Models Bound to Resource Types *For Requests*:");
        for (Map.Entry<String, Class<?>> entry : adapterImplementations.getResourceTypeMappingsForRequests().entrySet()) {
            printWriter.print(entry.getValue().getName());
            printWriter.print(" - ");
            printWriter.println(entry.getKey());
        }

        printWriter.println();

        // registered exporter servlets
        printWriter.println("Sling Models Exporter Servlets:");
        try {
            ServiceReference[] servlets = bundleContext.getServiceReferences(Servlet.class.getName(), EXPORT_SERVLET_FILTER);
            if (servlets != null) {
                for (ServiceReference ref : servlets) {
                    printWriter.print(ref.getProperty(ModelPackageBundleListener.PROP_EXPORTER_SERVLET_CLASS));
                    printWriter.print(" exports '");
                    printWriter.print(ref.getProperty("sling.servlet.resourceTypes"));
                    printWriter.print("' with selector '");
                    printWriter.print(ref.getProperty("sling.servlet.selectors"));
                    printWriter.print("' and extension '");
                    printWriter.print(ref.getProperty("sling.servlet.extensions"));
                    printWriter.print("' with exporter '");
                    printWriter.print(ref.getProperty(ModelPackageBundleListener.PROP_EXPORTER_SERVLET_NAME));
                    printWriter.println("'");
                }
            }
        } catch (InvalidSyntaxException e) {
            // ignore
        }
    }

}