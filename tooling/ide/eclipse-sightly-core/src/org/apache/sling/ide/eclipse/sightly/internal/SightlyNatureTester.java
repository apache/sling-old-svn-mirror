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
package org.apache.sling.ide.eclipse.sightly.internal;

import org.apache.sling.ide.eclipse.sightly.SightlyFacetHelper;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

public class SightlyNatureTester extends PropertyTester{

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        
        if ( !"sightlyNature".equals(property)) {
            return false;
        }
        
        IResource res;
        
        if ( receiver instanceof IResource) {
            res = ((IResource) receiver);
        } else if ( receiver instanceof IAdaptable) {
            res = (IResource) ((IAdaptable) receiver).getAdapter(IResource.class);
        } else {
            return false;
        }
        
        return SightlyFacetHelper.hasSightlyFacet(res.getProject());
    }

}
