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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.jcr.jcrinstall.jcr.NodeConverter;
import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.apache.sling.runmode.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts configuration nodes to InstallableData, taking
 * 	RunMode into account.
 */
class ConfigNodeConverter implements NodeConverter {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**	TODO making this dynamic and optional would be better, but
	 * 	that would probably create issues at startup 
	 * 	@scr.reference 
	 */
	private RunMode runMode;
	
	/** Convert n to an InstallableData, or return null
	 * 	if we don't know how to convert it.
	 */
	public InstallableData convertNode(Node n) throws Exception {
		InstallableData result = null;
		
		// TODO use a mixin to identify these nodes?
		if(n.isNodeType("nt:unstructured")) {
			final Dictionary<String, Object> config = load(n);
			result = new ConfigInstallableData(config);
			log.debug("Converted node {} to {}", n.getPath(), result);
		}
		return result;
	}
	
    /** Load config from node n */
    protected Dictionary<String, Object> load(Node n) throws RepositoryException {
        Dictionary<String, Object> result = new Hashtable<String, Object>();
        
        log.debug("Loading config from Node {}", n.getPath());
        
        // load default values from node itself
        log.debug("Loading {} properties", n.getPath());
        loadProperties(result, n);
        
        if(runMode != null) {
            final String [] modeStr = runMode.getCurrentRunModes();
            final SortedSet<String> modes = new TreeSet<String>();
            for(String s : modeStr) {
                modes.add(s);
            }
            for(String mode : modes) {
                if(n.hasNode(mode)) {
                    log.debug(
                            "Loading {}/{} properties for current run mode, overriding previous values", 
                            n.getPath(), mode);
                    loadProperties(result, n.getNode(mode));
                }
            }
        }
        
        return result;
    }
    
    /** Load properties of n into d */
    protected void loadProperties(Dictionary<String, Object> d, Node n) throws RepositoryException {
        final PropertyIterator pi = n.getProperties();
        while(pi.hasNext()) {
            final Property p = pi.nextProperty();
            final String name = p.getName();
            
            // ignore jcr: and similar properties
            if(name.contains(":")) {
                continue;
            }
            if(p.getDefinition().isMultiple()) {
                Object [] data = null;
                final Value [] values = p.getValues();
                int i = 0;
                for(Value v : values) {
                    Object o = convertValue(v);
                    if(i == 0) {
                        data = (Object[])Array.newInstance(o.getClass(), values.length);
                    }
                    data[i++] = o;
                }
                d.put(name, data);
                
            } else {
                final Object o = convertValue(p.getValue());
                if(o != null) {
                    d.put(name, o);
                }
            }
        }
    }

    /** Convert v according to its type */
    protected Object convertValue(Value v) throws RepositoryException {
        switch(v.getType()) {
        case PropertyType.STRING:
            return v.getString();
        case PropertyType.DATE:
            return v.getDate();
        case PropertyType.DOUBLE:
            return v.getDouble();
        case PropertyType.LONG:
            return v.getLong();
        case PropertyType.BOOLEAN:
            return v.getBoolean();
        }
        log.debug("Value of type {} ignored", v.getType());
        return null;
    }
}