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

package org.apache.sling.commons.osgi;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

public class PropertiesUtilTest extends TestCase{

    public void testToMap() {
        final String[] defaultValue = new String[] {"a1=b1","a2=b2"};
        Map<String,String> expected = asMap("a1","b1","a2","b2");
        assertEquals(expected,PropertiesUtil.toMap(new String[] {"a1=b1","a2=b2"},null));
        assertEquals(null,PropertiesUtil.toMap(null,null));
        assertEquals(expected,PropertiesUtil.toMap(null,defaultValue));

        //Trimming
        assertEquals(expected,PropertiesUtil.toMap(new String[] {"a1 = b1 "," a2 = b2 "},null));

        //Malformed handling
        assertEquals(expected,PropertiesUtil.toMap(new String[] {"a1 = b1 "," a2 = b2 ","a3"},null));
        assertEquals(asMap("a1","b1","a2","b2","a4",null),
                PropertiesUtil.toMap(new String[] {"a1 = b1 "," a2 = b2 ","a3","a4="},null));
    }


    private static Map<String,String> asMap(String ... entries){
        Map<String,String> m = new LinkedHashMap<String, String>();
        for(int i = 0; i < entries.length; i+=2){
            m.put(entries[i],entries[i+1]);
        }
        return m;
    }
}
