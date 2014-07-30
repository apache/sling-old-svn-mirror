/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.annotations;

import java.util.List;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ClassAnnotation;
import org.apache.felix.scrplugin.annotations.ScannedClass;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.PropertyUnbounded;
import org.apache.felix.scrplugin.description.ServiceDescription;
import org.apache.sling.hc.api.HealthCheck;

/** Annotation processor for the SlingHealthCheck annotation. */
public class SlingHealthCheckProcessor implements AnnotationProcessor {

    @Override
    public void process(final ScannedClass scannedClass, final ClassDescription classDescription) throws SCRDescriptorException, SCRDescriptorFailureException {
        final List<ClassAnnotation> servlets = scannedClass.getClassAnnotations(SlingHealthCheck.class.getName());
        scannedClass.processed(servlets);

        for (final ClassAnnotation cad : servlets) {
            processHealthCheck(cad, classDescription);
        }
    }

    /** Processes the given healthcheck annotation.
     * 
     * @param cad the annotation
     * @param classDescription the class description */
    private void processHealthCheck(final ClassAnnotation cad, final ClassDescription classDescription) {

        final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
        final boolean metatype = cad.getBooleanValue("metatype", true);

        // generate ComponentDescription if required
        if (generateComponent) {
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("componentName", classDescription.getDescribedClass().getName()));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(cad.getEnumValue("configurationPolicy",
                    ComponentConfigurationPolicy.OPTIONAL.name())));
            cd.setSetMetatypeFactoryPid(cad.getBooleanValue("configurationFactory", false));

            cd.setLabel(cad.getStringValue("label", null));
            cd.setDescription(cad.getStringValue("description", null));

            cd.setCreateMetatype(metatype);

            classDescription.add(cd);
        }

        // generate ServiceDescription if required
        final boolean generateService = cad.getBooleanValue("generateService", true);
        if (generateService) {
            final ServiceDescription sd = new ServiceDescription(cad);
            sd.addInterface(HealthCheck.class.getName());
            classDescription.add(sd);
        }

        // generate PropertyDescriptions
        generateStringArrPropertyDescriptor(cad, classDescription, metatype, "tags", HealthCheck.TAGS);
        generateStringPropertyDescriptor(cad, classDescription, metatype, "name", HealthCheck.NAME);
    }

    /** Generates a property descriptor of type {@link PropertyType#String[]} */
    private void generateStringArrPropertyDescriptor(final ClassAnnotation cad, final ClassDescription classDescription,
            final boolean metatype, final String annotationName, final String propertyDescriptorName) {

        final String[] values = (String[]) cad.getValue(annotationName);
        if (values == null) {
            return;
        }

        final PropertyDescription pd = new PropertyDescription(cad);
        pd.setName(propertyDescriptorName);
        pd.setMultiValue(values);
        pd.setType(PropertyType.String);
        pd.setUnbounded(PropertyUnbounded.ARRAY);
        pd.setCardinality(Integer.MAX_VALUE);
        if (metatype) {
            pd.setPrivate(true);
        }
        classDescription.add(pd);
    }

    
    /** Generates a property descriptor of type {@link PropertyType#String} */
    private void generateStringPropertyDescriptor(final ClassAnnotation cad, final ClassDescription classDescription,
            final boolean metatype, final String annotationName, final String propertyDescriptorName) {

        final String hcName = (String) cad.getValue(annotationName);

        final PropertyDescription pd = new PropertyDescription(cad);
        pd.setName(propertyDescriptorName);
        pd.setValue(hcName);
        pd.setType(PropertyType.String);
        if (metatype) {
            pd.setPrivate(true);
        }
        classDescription.add(pd);
    }
    
    @Override
    public int getRanking() {
        return 500;
    }

    @Override
    public String getName() {
        return SlingHealthCheck.class.getName() + " annotation processor.";
    }
}
