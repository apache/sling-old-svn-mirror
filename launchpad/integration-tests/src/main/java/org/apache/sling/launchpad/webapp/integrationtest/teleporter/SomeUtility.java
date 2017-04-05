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
package org.apache.sling.launchpad.webapp.integrationtest.teleporter;

import java.util.ArrayList;
import java.util.List;

/** Utility class used directly by RequiredDependenciesTest */ 
public class SomeUtility {
    int getSum() {
        final List<SomePojo> list = new ArrayList<SomePojo>();
        list.add(new SomePojo());
        list.add(new SomePojo());
        int sum = 0;
        for(SomePojo p : list) {
            sum += p.getValue();
        }
        return sum;
    }
    
    public String getOk() {
        final List<SomeString> list = new ArrayList<SomeString>();
        list.add(new SomeString());
        list.add(new SomeString());
        String result = "";
        for(SomeString p : list) {
            result += p;
        }
        return result;
    }
}
