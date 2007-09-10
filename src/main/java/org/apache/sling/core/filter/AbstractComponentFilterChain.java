/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.filter;

import java.io.IOException;

import org.apache.sling.component.ComponentException;
import org.apache.sling.component.ComponentFilter;
import org.apache.sling.component.ComponentFilterChain;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;

public abstract class AbstractComponentFilterChain implements ComponentFilterChain {
    
    private ComponentFilter[] filters;
    private int current;
    
    protected AbstractComponentFilterChain(ComponentFilter[] filters) {
        this.filters = filters;
        this.current = -1;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.day.components.RenderFilterChain#doFilter(com.day.components.RenderRequest,
     *      com.day.components.ComponentResponse)
     */
    public void doFilter(ComponentRequest request, ComponentResponse response)
            throws IOException, ComponentException {
        current++;
        
        if (current < filters.length) {
            filters[current].doFilter(request, response, this);
        } else {
            render(request, response);
        }
    }
    
    protected abstract void render(ComponentRequest request, ComponentResponse response) throws IOException, ComponentException;
}