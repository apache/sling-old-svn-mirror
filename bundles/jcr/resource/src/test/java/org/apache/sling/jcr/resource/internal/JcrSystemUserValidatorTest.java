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
package org.apache.sling.jcr.resource.internal;

import java.lang.reflect.Field;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.junit.Before;
import org.junit.Test;


public class JcrSystemUserValidatorTest extends RepositoryTestBase {
    
    private static final String GROUP_ADMINISTRATORS = "administrators";

    private JcrSystemUserValidator jcrSystemUserValidator;
    
    @Before
    public void setUp() throws IllegalArgumentException, IllegalAccessException, RepositoryException, NamingException, NoSuchFieldException, SecurityException {
        jcrSystemUserValidator = new JcrSystemUserValidator();
        Field repositoryField = jcrSystemUserValidator.getClass().getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(jcrSystemUserValidator, getRepository());
    }
    
    @Test
    public void testIsValidWithEnforcementOfSystemUsersEnabled() throws Exception {
        Field allowOnlySystemUsersField = jcrSystemUserValidator.getClass().getDeclaredField("allowOnlySystemUsers");
        allowOnlySystemUsersField.setAccessible(true);
        allowOnlySystemUsersField.set(jcrSystemUserValidator, true);
        
        //testing null user
        assertFalse(jcrSystemUserValidator.isValid(null, null, null));
        //testing not existing user     
        assertFalse(jcrSystemUserValidator.isValid("notExisting", null, null));
        //administrators group is not a valid user  (also not a system user)
        assertFalse(jcrSystemUserValidator.isValid(GROUP_ADMINISTRATORS, null, null));
    }
    
    @Test
    public void testIsValidWithEnforcementOfSystemUsersDisabled() throws Exception {
        Field allowOnlySystemUsersField = jcrSystemUserValidator.getClass().getDeclaredField("allowOnlySystemUsers");
        allowOnlySystemUsersField.setAccessible(true);
        allowOnlySystemUsersField.set(jcrSystemUserValidator, false);
        
        //testing null user
        assertFalse(jcrSystemUserValidator.isValid(null, null, null));
        //testing not existing user (is considered valid here)
        assertTrue(jcrSystemUserValidator.isValid("notExisting", null, null));
        // administrators group is not a user at all (but considered valid)
        assertTrue(jcrSystemUserValidator.isValid(GROUP_ADMINISTRATORS, null, null));
    }
}
