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
package org.apache.sling.validation.impl.util;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.validation.ValidationResult;
import org.apache.sling.validation.exceptions.SlingValidationException;
import org.apache.sling.validation.impl.util.examplevalidators.DerivedStringValidator;
import org.apache.sling.validation.impl.util.examplevalidators.ExtendedStringValidator;
import org.apache.sling.validation.impl.util.examplevalidators.GenericTypeParameterBaseClass;
import org.apache.sling.validation.impl.util.examplevalidators.IntegerValidator;
import org.apache.sling.validation.impl.util.examplevalidators.StringArrayValidator;
import org.apache.sling.validation.impl.util.examplevalidators.StringValidator;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.apache.sling.validation.spi.DefaultValidationResult;
import org.apache.sling.validation.spi.Validator;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class ValidatorTypeUtilTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testGetValidatorTypeOfDirectImplementations() {
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new RegexValidator()), Matchers.equalTo(String.class));
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new StringValidator()), Matchers.equalTo(String.class));
        Assert.assertThat((Class<Integer>)ValidatorTypeUtil.getValidatorType(new IntegerValidator()), Matchers.equalTo(Integer.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGetValidatorTypeOfDerivedImplementations() {
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new DerivedStringValidator()), Matchers.equalTo(String.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGetValidatorTypeWithAdditionalTypeParameters() {
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new ExtendedStringValidator()), Matchers.equalTo(String.class));
    }
    
    private class InnerStringValidator implements Validator<String> {
        @Override
        public @Nonnull ValidationResult validate(@Nonnull String data, @Nonnull ValueMap valueMap, Resource resource, @Nonnull ValueMap arguments)
                throws SlingValidationException {
            return DefaultValidationResult.VALID;
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGetValidatorTypeWithInnerClass() {
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new InnerStringValidator()), Matchers.equalTo(String.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGetValidatorTypeWithAnonymousClass() {
        Assert.assertThat((Class<String>)ValidatorTypeUtil.getValidatorType(new Validator<String>() {
            @Override
            public @Nonnull ValidationResult validate(@Nonnull String data, @Nonnull ValueMap valueMap, Resource resource, @Nonnull ValueMap arguments)
                    throws SlingValidationException {
                return DefaultValidationResult.VALID;
            }
            
        }), Matchers.equalTo(String.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testGetValidatorTypeWithArrayType() {
        Assert.assertThat((Class<String[]>)ValidatorTypeUtil.getValidatorType(new StringArrayValidator()), Matchers.equalTo(String[].class));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetValidatorTypeWithCollectionType() {
        ValidatorTypeUtil.getValidatorType(new Validator<Collection<String>>() {
            @Override
            public @Nonnull ValidationResult validate(@Nonnull Collection<String> data, @Nonnull ValueMap valueMap, Resource resource, @Nonnull ValueMap arguments)
                    throws SlingValidationException {
                return DefaultValidationResult.VALID;
            }
        });
    }
    
    private class InnerStringValidatorWithAdditionalBaseClass extends GenericTypeParameterBaseClass<Integer> implements Validator<String> {
        @Override
        public @Nonnull ValidationResult validate(@Nonnull String data, @Nonnull ValueMap valueMap, Resource resource, @Nonnull ValueMap arguments)
                throws SlingValidationException {
            return DefaultValidationResult.VALID;
        }
    }
    
    @Test
    public void testGetValidatorTypeWithUnrelatedSuperClass() {
        // http://stackoverflow.com/questions/24093000/how-do-i-match-a-class-against-a-specific-class-instance-in-a-hamcrest-matche
        Assert.assertThat((Class<?>)ValidatorTypeUtil.getValidatorType(new InnerStringValidatorWithAdditionalBaseClass()), Matchers.is(CoreMatchers.<Class<?>>equalTo(String.class)));
    }
}
