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
package org.apache.sling.commons.scheduler.impl;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class SettingsSupportTest {
    private SettingsSupport.Listener listener;
    private BundleContext context;

    @Before
    public void setUp() {
        context = MockOsgi.newBundleContext();
        listener = new SettingsSupport.Listener(context);
    }

    @Test
    public void testActivateDeactivate() throws NoSuchFieldException, IllegalAccessException {
        SettingsSupport support = new SettingsSupport();
        support.activate(context);

        Field settingsListenerPrivateField = SettingsSupport.class.getDeclaredField("settingsListener");
        settingsListenerPrivateField.setAccessible(true);

        assertNotNull(settingsListenerPrivateField.get(support));
        support.deactivate();
        assertNull(settingsListenerPrivateField.get(support));
    }

    @Test
    public void testStartAndStop() throws NoSuchFieldException, IllegalAccessException {
        listener.start();
        Field activeField = listener.getClass().getDeclaredField("active");
        activeField.setAccessible(true);
        AtomicBoolean isActive = (AtomicBoolean) activeField.get(listener);
        assertTrue(isActive.get());
        listener.stop();
        assertFalse(isActive.get());
    }
}
