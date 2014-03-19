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

package org.apache.sling.resourceaccesssecurity.it.impl.gates;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceaccesssecurity.AllowingResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate.GateResult;

import java.util.Map;

public abstract class AResourceAccessGate extends AllowingResourceAccessGate implements ResourceAccessGate {


    @Activate
    protected void activate(final Map<String, Object> props) {
    }
    
    /**
     * gets the gate id which will be used to distinguish the different implementations
     * of the gate
     * @return
     */
    protected abstract String getGateId ();

    @Override
    public GateResult canRead(Resource resource) {
        GateResult returnValue = GateResult.CANT_DECIDE;
        
        if ( resource.getPath().contains( getGateId() + "-denyread") )
        {
            returnValue = GateResult.DENIED;
        }
        else if ( resource.getPath().contains( getGateId() + "-allowread") )
        {
            returnValue = GateResult.GRANTED;
        }
        
        return returnValue;
    }

    @Override
    public boolean hasReadRestrictions(ResourceResolver resourceResolver) {
        return true;
    }
    
    @Override
    public GateResult canUpdate(Resource resource) {
        GateResult returnValue = GateResult.CANT_DECIDE;
        
        if ( resource.getPath().contains( getGateId() + "-denyupdate") )
        {
            returnValue = GateResult.DENIED;
        }
        else if ( resource.getPath().contains( getGateId() + "-allowupdate") )
        {
            returnValue = GateResult.GRANTED;
        }
        
        return returnValue;
    }

    @Override
    public boolean hasUpdateRestrictions(ResourceResolver resourceResolver) {
        return true;
    }
}
