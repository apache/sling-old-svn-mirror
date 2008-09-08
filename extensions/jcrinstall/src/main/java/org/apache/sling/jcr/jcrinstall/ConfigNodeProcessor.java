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
package org.apache.sling.jcr.jcrinstall;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/** Process nodes that look like configs, based on their node name */
class ConfigNodeProcessor extends AbstractNodeProcessor {
    private static final String ALIAS_KEY = "_alias_factory_pid";
    
	private final ConfigurationAdmin cadmin;

	/** Configration PIDs are built out of filenames, examples:
	 * 		o.a.s.foo.bar.cfg -> pid = o.a.s.foo.bar
	 * 		o.a.s.foo.bar-a.cfg -> pid = .a.s.foo.bar, factory pid = a 
	 */
	static class ConfigPid {
		final String configPid;
		final String factoryPid;

		ConfigPid(String path) {
	        // cut off path and extension
	        String pid = path;
	        final int lastSlash = path.lastIndexOf('/');
	        if(lastSlash >= 0) {
		        pid = path.substring(lastSlash + 1);
	        }
	        final int lastDot = pid.lastIndexOf('.');
	        if(lastDot >= 0) {
	        	pid = pid.substring(0, lastDot);
	        }

	        // split pid and factory pid alias
	        int n = pid.indexOf('-');
	        if (n > 0) {
	            factoryPid = pid.substring(n + 1);
	            configPid = pid.substring(0, n);
	        } else {
	        	factoryPid = null;
	        	configPid = pid;
	        }
		}
		
		@Override
		public String toString() {
			return "Configuration (configPid=" + configPid + ", factoryPid=" + factoryPid + ")";
		}
	};
	
	public ConfigNodeProcessor(ConfigurationAdmin ca) {
		super("[a-zA-Z0-9].*\\.cfg$");
		cadmin = ca;
	}
	
	public void process(Node n, Map<String, Boolean> flags) throws RepositoryException, IOException, InvalidSyntaxException {
		
		// For now we support only file-based configs
		final InputStream is = getInputStream(n);
		if(is == null) {
			log.warn("Cannot get InputStream for node {}, Node will be ignored", n.getPath());
			return;
		}
		
        // Do nothing if config didn't change
        final Node status = getStatusNode(n, true);
		final Calendar lastModified = getLastModified(n);
		Calendar savedLastModified = null;
		if(status.hasProperty(JCR_LAST_MODIFIED)) {
			savedLastModified = status.getProperty(JCR_LAST_MODIFIED).getDate();
		}
		
		boolean changed = 
			savedLastModified == null 
			|| lastModified == null 
			|| !(lastModified.equals(savedLastModified))
		;
		
		if(!changed) {
	        log.debug("Config {} unchanged, no update needed", n.getPath());
	        return;
		}
		if(lastModified != null) {
			status.setProperty(JCR_LAST_MODIFIED, lastModified);
		}
		n.getSession().save();

		// Load configuration properties
        final Properties p = new Properties();
        try {
        	p.load(is);
        } finally {
        	is.close();
        }
        
        // Get pids from node name
        final ConfigPid pid = new ConfigPid(n.getPath());
        log.debug("{} created for node {}", pid, n.getPath());

        // prepare configuration data
        Hashtable<Object, Object> ht = new Hashtable<Object, Object>();
        ht.putAll(p);
        if(pid.factoryPid != null) {
            ht.put(ALIAS_KEY, pid.factoryPid);
        }

        // get or create configuration
        Configuration config = getConfiguration(pid, true);
        if (config.getBundleLocation() != null) {
            config.setBundleLocation(null);
        }
        config.update(ht);
        log.info("Configuration {} created or updated", config.getPid());
	}
	
    public void checkDeletions(Node statusNode, Map<String, Boolean> flags) throws Exception {
    	final Node mainNode = getMainNode(statusNode);
    	if(mainNode == null) {
    		final String mainPath = getMainNodePath(statusNode.getPath());
    		final ConfigPid pid = new ConfigPid(mainPath);
    		final Configuration config = getConfiguration(pid, false);
    		if(config == null) {
    			log.info("Node {} has been deleted, but {} not found - deleting status node only", 
    					mainPath, pid);
    		} else {
    			config.delete();
    			log.info("Node {} has been deleted: {} deleted", 
    					mainPath, pid);
    		}
    		
    		statusNode.remove();
    		statusNode.getSession().save();
    	}
    }

    Configuration getConfiguration(ConfigPid cp, boolean createIfNeeded) throws IOException, InvalidSyntaxException {
    	Configuration result = null;
    	
        if (cp.factoryPid == null) {
        	result = cadmin.getConfiguration(cp.configPid, null);
        } else {
            Configuration configs[] = cadmin.listConfigurations("(|(" + ALIAS_KEY
                + "=" + cp.factoryPid + ")(.alias_factory_pid=" + cp.factoryPid
                + "))");
            
            if (configs == null || configs.length == 0) {
            	if(createIfNeeded) {
            		result = cadmin.createFactoryConfiguration(cp.configPid, null);
            	}
            } else {
            	result = configs[0];
            }
        }
        
        return result;
    }
}
