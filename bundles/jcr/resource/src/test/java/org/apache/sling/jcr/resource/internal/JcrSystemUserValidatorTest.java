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
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.junit.Test;


public class JcrSystemUserValidatorTest extends RepositoryTestBase {
    
    private static final String GROUP_ADMINISTRATORS = "administrators";

    private static final String SYSTEM_USER = "systemUser";
    
    private JcrSystemUserValidator jcrSystemUserValidator;
    
    
    @Test
    public void testIsValid_notValid() throws Exception {
        jcrSystemUserValidator = new JcrSystemUserValidator();
        Field repositoryField = jcrSystemUserValidator.getClass().getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(jcrSystemUserValidator, getRepository());
        
        //testing null user
        assertFalse(jcrSystemUserValidator.isValid(null, null, null));
        //testing not existing user     
        assertFalse(jcrSystemUserValidator.isValid("notExisting", null, null));
        //administrators group is not a valid user  (also not a system user)
        assertFalse(jcrSystemUserValidator.isValid(GROUP_ADMINISTRATORS, null, null));
    }
}
