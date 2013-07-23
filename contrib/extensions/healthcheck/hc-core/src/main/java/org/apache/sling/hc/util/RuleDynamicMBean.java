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
package org.apache.sling.hc.util;

import java.io.Serializable;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.apache.sling.hc.api.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link DynamicMBean} that gives access to a {@link Rule}'s data */
public class RuleDynamicMBean implements DynamicMBean, Serializable {

    private static final long serialVersionUID = -90745301105975287L;
    private static final Logger logger = LoggerFactory.getLogger(RuleDynamicMBean.class);
    private final String beanName;
    private final Rule rule;
    
    public static final String RULE_OK_ATTRIBUTE_NAME = "ok";
    public static final String LOG_ATTRIBUTE_NAME = "log";
    
    public RuleDynamicMBean(Rule r) {
        beanName = r.toString();
        rule = r;
    }
    
    @Override
    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        if(RULE_OK_ATTRIBUTE_NAME.equals(attribute)) {
            return !rule.evaluate().anythingToReport();
        } else {
            final Object o = rule.getInfo().get(attribute);
            if(o == null) {
                throw new AttributeNotFoundException(attribute);
            }
            return o;
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        final AttributeList result = new AttributeList();
        for(String key : attributes) {
            try {
                result.add(new Attribute(key, getAttribute(key)));
            } catch(Exception e) {
                logger.error("Exception getting Attribute " + key, e);
            }
        }
        return result;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        final MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[rule.getInfo().size() + 1];
        int i=0;
        attrs[i++] = new MBeanAttributeInfo(RULE_OK_ATTRIBUTE_NAME, Boolean.class.getName(), "The rule value", true, false, false);
        for(String key : rule.getInfo().keySet()) {
            attrs[i++] = new MBeanAttributeInfo(key, String.class.getName(), "Description of " + key, true, false, false);
        }
        
        return new MBeanInfo(this.getClass().getName(), beanName, attrs, null, null, null);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support operations on Rules");
    }

    @Override
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support setting Rules attributes");
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support setting Rules attributes");
    }
}