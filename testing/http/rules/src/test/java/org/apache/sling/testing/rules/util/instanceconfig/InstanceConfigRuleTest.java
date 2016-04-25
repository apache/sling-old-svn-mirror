/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.rules.util.instanceconfig;

import org.apache.sling.testing.rules.InstanceConfigRule;
import org.apache.sling.testing.rules.util.Action;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

// TODO: Use a junit processor to test this
public class InstanceConfigRuleTest {
    public boolean actionCalled = false;
    
    class DebugAction implements Action {
        @Override
        public void call() throws Throwable {
            actionCalled = true;
        }
    }
    
    DebugInstanceConfig dic = new DebugInstanceConfig();
    
    @Rule
    public InstanceConfigRule myInstanceConfig = new InstanceConfigRule(dic).withAction(new DebugAction());
    
    @Test
    public void myTest() {
        Assert.assertEquals("Value should be the one set on save: 1", 1, dic.getValue());
        Assert.assertTrue(actionCalled);
    }


}
