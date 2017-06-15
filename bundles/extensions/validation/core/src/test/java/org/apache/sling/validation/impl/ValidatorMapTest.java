/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.validation.impl.ValidatorMap.ValidatorMetadata;
import org.apache.sling.validation.impl.util.examplevalidators.DateValidator;
import org.apache.sling.validation.impl.util.examplevalidators.StringValidator;
import org.apache.sling.validation.spi.Validator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class ValidatorMapTest {

    private ValidatorMap validatorMap;
    private DateValidator dateValidator;
    
    @Mock
    private ServiceReference<Validator<?>> validatorServiceReference;
    @Mock
    private ServiceReference<Validator<?>> newValidatorServiceReference;
    @Mock
    private Bundle providingBundle;

    private static final String DATE_VALIDATOR_ID = "DateValidator";
    
    @Before
    public void setUp() {
        validatorMap = new ValidatorMap();
        dateValidator =  new DateValidator();
        Mockito.doReturn("some name").when(providingBundle).getSymbolicName();
        Mockito.doReturn(providingBundle).when(validatorServiceReference).getBundle();
        Mockito.doReturn(providingBundle).when(newValidatorServiceReference).getBundle();
        validatorMap.put(DATE_VALIDATOR_ID, dateValidator, validatorServiceReference, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPutWithoutValidatorIdProperty() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorMap.put(validatorProperties, dateValidator, validatorServiceReference);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPutWithWronglyTypedValidatorId() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, new String[]{"some", "value"});
        validatorMap.put(validatorProperties, dateValidator, validatorServiceReference);
    }

    @Test
    public void testPutValidatorWithSameValidatorIdAndHigherRanking() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, DATE_VALIDATOR_ID);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 2);
        Mockito.doReturn(1).when(newValidatorServiceReference).compareTo(Mockito.anyObject());
        Validator<String> stringValidator = new StringValidator();
        validatorMap.put(validatorProperties,stringValidator, newValidatorServiceReference);
        Assert.assertEquals(new ValidatorMetadata(stringValidator, newValidatorServiceReference, 2), validatorMap.get(DATE_VALIDATOR_ID));
    }

    @Test
    public void testPutValidatorWithSameValidatorIdAndLowerRanking() {
        Map<String, Object> validatorProperties = new HashMap<>();
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, DATE_VALIDATOR_ID);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 2);
        Mockito.doReturn(-1).when(newValidatorServiceReference).compareTo(Mockito.anyObject());
        Validator<String> stringValidator = new StringValidator();
        validatorMap.put(validatorProperties, stringValidator, newValidatorServiceReference);
        Assert.assertEquals(new ValidatorMetadata(dateValidator, validatorServiceReference, 10), validatorMap.get(DATE_VALIDATOR_ID));
    }

    @Test
    public void testUpdateChangingValidatorId() {
        Map<String, Object> validatorProperties = new HashMap<>();
        String newId = "newId";
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_ID, newId);
        validatorProperties.put(Validator.PROPERTY_VALIDATOR_SEVERITY, 1);
        Mockito.doReturn(-1).when(newValidatorServiceReference).compareTo(Mockito.anyObject());
        validatorMap.update(validatorProperties, dateValidator, validatorServiceReference);
        Assert.assertEquals(new ValidatorMetadata(dateValidator, validatorServiceReference, 1), validatorMap.get(newId));
        // make sure that the old validator id is no longer in the list
        Assert.assertNull(validatorMap.get(DATE_VALIDATOR_ID));
    }
}
