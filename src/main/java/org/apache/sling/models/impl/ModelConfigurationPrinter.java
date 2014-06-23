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

import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;

public class ModelConfigurationPrinter {

    private final ModelAdapterFactory modelAdapterFactory;

    ModelConfigurationPrinter(ModelAdapterFactory modelAdapterFactory) {
        this.modelAdapterFactory = modelAdapterFactory;
    }

    public void printConfiguration(PrintWriter printWriter) {
        printWriter.println("Sling Models Injectors:");
        Injector[] injectors = modelAdapterFactory.getInjectors();
        if (injectors == null) {
            printWriter.println("none");
        } else {
            for (Injector injector : injectors) {
                printWriter.printf("%s - %s", injector.getName(), injector.getClass().getName());
                printWriter.println();
            }
        }
        printWriter.println();
        printWriter.println("Sling Models Inject Annotation Processor Factories:");
        InjectAnnotationProcessorFactory[] factories = modelAdapterFactory.getInjectAnnotationProcessorFactories();
        if (factories == null) {
            printWriter.println("none");
        } else {
            for (InjectAnnotationProcessorFactory factory : factories) {
                printWriter.printf("%s", factory.getClass().getName());
                printWriter.println();
            }
        }
    }

}