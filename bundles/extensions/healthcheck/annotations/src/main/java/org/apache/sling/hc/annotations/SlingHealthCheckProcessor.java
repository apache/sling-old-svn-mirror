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
        final List<ClassAnnotation> healthChecks = scannedClass.getClassAnnotations(SlingHealthCheck.class.getName());
        scannedClass.processed(healthChecks);

        for (final ClassAnnotation cad : healthChecks) {
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
        final boolean immediate = cad.getBooleanValue("immediate", false);

        // generate ComponentDescription if required
        if (generateComponent) {
            String nameOfAnnotatedClass = classDescription.getDescribedClass().getName();
            
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("componentName", nameOfAnnotatedClass));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(cad.getEnumValue("configurationPolicy",
                    ComponentConfigurationPolicy.OPTIONAL.name())));
            cd.setSetMetatypeFactoryPid(cad.getBooleanValue("configurationFactory", false));

            String nameFromAnnotation = (String) cad.getValue("name");
            String defaultLabel = "Sling Health Check: " + (nameFromAnnotation!=null ? nameFromAnnotation : nameOfAnnotatedClass);
            cd.setLabel(cad.getStringValue("label", defaultLabel));
            cd.setDescription(cad.getStringValue("description", "Health Check Configuration"));

            cd.setCreateMetatype(metatype);
            cd.setImmediate(immediate);

            classDescription.add(cd);
        }

        // generate ServiceDescription if required
        final boolean generateService = cad.getBooleanValue("generateService", true);
        if (generateService) {
            final ServiceDescription sd = new ServiceDescription(cad);
            sd.addInterface(HealthCheck.class.getName());
            classDescription.add(sd);
        }

        // generate HC PropertyDescriptions
        generatePropertyDescriptor(cad, classDescription, metatype, "name", HealthCheck.NAME, PropertyType.String, "Name", "Name of the Health Check", false);
        generatePropertyDescriptor(cad, classDescription, metatype, "tags", HealthCheck.TAGS, PropertyType.String, "Tags", "List of tags", true);
        generatePropertyDescriptor(cad, classDescription, metatype, "mbeanName", HealthCheck.MBEAN_NAME, PropertyType.String, "MBean", "MBean name (leave empty for not using JMX)", false);
        generatePropertyDescriptor(cad, classDescription, metatype, "asyncCronExpression",  HealthCheck.ASYNC_CRON_EXPRESSION, PropertyType.String, "Cron expression", "Cron expression for asynchronous execution (leave empty for synchronous execution)", false);
        generatePropertyDescriptor(cad, classDescription, metatype, "resultCacheTtlInMs", "hc.resultCacheTtlInMs" /* use constant once API is released */, PropertyType.Long , "Result Cache TTL", "TTL for results. The value -1 (default) uses the global configuration in health check executor. Redeployment of a HC always invalidates its cached result.", false);
    }

    /** Generates a property descriptor of type {@link PropertyType} */
    private void generatePropertyDescriptor(final ClassAnnotation cad, final ClassDescription classDescription,
            final boolean metatype, final String propertyName, final String propertyDescriptorName, PropertyType propertyType, String label, String description, boolean isArray) {

        final PropertyDescription pd = new PropertyDescription(cad);
        pd.setName(propertyDescriptorName);
        pd.setLabel(label);
        pd.setDescription(description);
        pd.setType(propertyType);

        if(isArray) {
            final String[] values = (String[]) cad.getValue(propertyName);
            pd.setMultiValue(values);
            pd.setUnbounded(PropertyUnbounded.ARRAY);
            pd.setCardinality(Integer.MAX_VALUE);
        } else {
            final Object propertyVal = cad.getValue(propertyName);
            String pdValue = (propertyVal instanceof String) ? (String) propertyVal :
                propertyVal!=null ? propertyVal.toString() : null;
            pd.setValue(pdValue);
            pd.setUnbounded(PropertyUnbounded.DEFAULT);
        }
        
        if (!metatype) {
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
