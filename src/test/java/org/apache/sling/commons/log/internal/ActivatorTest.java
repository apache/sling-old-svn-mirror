/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.log.internal;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

/**
 * A simple but fairly relaxed test of the activator. Also tests LogManager.
 */
public class ActivatorTest {
    
    @Mock
    private BundleContext context;
    
    public ActivatorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testActivator() throws Exception {
        Activator ac = new Activator();
        ac.start(context);
        ac.start(context);
    }
    
    @Test
    public void testActivatorWithJUL() throws Exception {
        Activator ac = new Activator();
        Mockito.when(context.getProperty("org.apache.sling.commons.log.julenabled")).thenReturn("true");
        ac.start(context);
        ac.start(context);
        
    }
    

}
