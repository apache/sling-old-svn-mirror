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

import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

public class ModelConfigurationPrinter {

    private final ModelAdapterFactory modelAdapterFactory;

    ModelConfigurationPrinter(ModelAdapterFactory modelAdapterFactory) {
        this.modelAdapterFactory = modelAdapterFactory;
    }

    public void printConfiguration(PrintWriter printWriter) {
        
        // injectors
        printWriter.println("Sling Models Injectors:");
        Injector[] injectors = modelAdapterFactory.getInjectors();
        if (injectors == null || injectors.length == 0) {
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
        InjectAnnotationProcessorFactory[] factories = modelAdapterFactory.getInjectAnnotationProcessorFactories();
        InjectAnnotationProcessorFactory2[] factories2 = modelAdapterFactory.getInjectAnnotationProcessorFactories2();
        Collection<StaticInjectAnnotationProcessorFactory> staticFactories = modelAdapterFactory.getStaticInjectAnnotationProcessorFactories();
        if ((factories == null || factories.length == 0)
                && (factories2 == null || factories2.length == 0)
                && (staticFactories == null || staticFactories.size() == 0)) {
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
    }

}