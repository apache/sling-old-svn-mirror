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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.felix.scr.annotations.ConfigurationPolicy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SlingHealthCheck {
    
    /** Defines the name of the health check. <p>
     * This attribute is converted to values for the <code>hc.name</code> property. */
    String name() default "";    

    /** One ore more tags.
     * <p>
     * This attribute is converted to values for the <code>hc.tags</code> property. */
    String[] tags() default {};
    
    /** The JMX mbean name (optional, the mbean is only registered if attribute present).
     * <p>
     * This attribute is converted to values for the <code>hc.mbean.name</code> property. */
    String mbeanName() default "";    
    
    /** Cron expression for asynchronous execution (optional, default is synchronous execution).
     * <p>
     * This attribute is converted to values for the <code>hc.async.cronExpression</code> property. */
    String asyncCronExpression() default "";       
    
    // handling of service and component properties (optional)

    /** Whether to generate a default SCR component tag. If set to false, a {@link org.apache.felix.scr.annotations.Component} annotation can be added manually
     * with defined whatever configuration needed. */
    boolean generateComponent() default true;

    /** Whether to generate a default SCR service tag with "interface=org.apache.sling.hc.api.HealthCheck". If set to false, a
     * {@link org.apache.felix.scr.annotations.Service} annotation can be added manually with defined whatever configuration needed. */
    boolean generateService() default true;
    
    /** Defines the Component name also used as the PID for the Configuration Admin Service. Default value: Fully qualified name of the Java class. */
    String componentName() default "";

    /** Whether Metatype Service data is generated or not. If this parameter is set to true Metatype Service data is generated in the <code>metatype.xml</code>
     * file for this component. Otherwise no Metatype Service data is generated for this component. */
    boolean metatype() default true;

    /** Whether immediate is set on the SCR component.  */
    boolean immediate() default false;

    /** Set the metatype factory pid property (only for non factory components). */
    boolean configurationFactory() default false;

    /** The component configuration policy */
    ConfigurationPolicy configurationPolicy() default ConfigurationPolicy.OPTIONAL;

    /** This is generally used as a title for the object described by the meta type. This name may be localized by prepending a % sign to the name. Default
     * value: %&lt;name&gt;.name */
    String label() default "";

    /** This is generally used as a description for the object described by the meta type. This name may be localized by prepending a % sign to the name. Default
     * value: %&lt;name&gt;.description */
    String description() default "";
}
