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
package org.apache.sling.osgi.installer.impl.tasks;

/** Install/remove task for configurations 
 *  TODO split into install / remove tasks and reimplement
 * */
public class ConfigInstallRemoveTask {

    static final String ALIAS_KEY = "_alias_factory_pid";
    static final String CONFIG_PATH_KEY = "_jcr_config_path";
    public static final String [] CONFIG_EXTENSIONS = { ".cfg", ".properties" };
    
    /**
    
	public ConfigInstallRemoveTask(String uri, RegisteredResource data, OsgiInstallerContext ocs) {
		super(uri, data, ocs);
	}
	
	@Override
	public String getSortKey() {
		if(isInstallOrUpdate()) {
			return TaskOrder.CONFIG_INSTALL_ORDER + uri;
		} else {
			return TaskOrder.CONFIG_UNINSTALL_ORDER + uri;
		}
	}

	@Override
	protected boolean doInstallOrUpdate(OsgiInstallerContext tctx, Map<String, Object> attributes) throws Exception {
    	// Convert data to a configuration Dictionary
    	Dictionary dict = data.getDictionary();

    	if (dict == null) {
    		throw new IllegalArgumentException("Null Dictionary for uri=" + uri);
    	}

    	// Add pseudo-properties
    	dict.put(CONFIG_PATH_KEY, uri);

        // Get pids from node name
        final ConfigurationPid pid = new ConfigurationPid(uri);
        if(ocs.getLogService() != null) {
    		ocs.getLogService().log(LogService.LOG_DEBUG,
    				pid + " created for uri " + uri);
        }

        if(pid.getFactoryPid() != null) {
            dict.put(ALIAS_KEY, pid.getFactoryPid());
        }

        // get or create configuration
        boolean created = false;
        Configuration config = TaskUtilities.getConfiguration(pid, false, ocs);
        if(config == null) {
            created = true;
            config = TaskUtilities.getConfiguration(pid, true, ocs);
        }
        if (config.getBundleLocation() != null) {
            config.setBundleLocation(null);
        }
        config.update(dict);
        if(ocs.getLogService() != null) {
    		ocs.getLogService().log(LogService.LOG_INFO,
    				"Configuration " + config.getPid() + " " + (created ? "created" : "updated"));
        }
        return true;
	}

	@Override
	protected void doUninstall(OsgiInstallerContext tctx, Map<String, Object> attributes) throws Exception {
        final ConfigurationPid pid = new ConfigurationPid(uri);
        final Configuration cfg = TaskUtilities.getConfiguration(pid, false, ocs);
        // TODO defer delete if ConfigAdmin not available?
        if(cfg == null) {
            if(ocs.getLogService() != null) {
        		ocs.getLogService().log(LogService.LOG_INFO,
        				"Cannot delete config " + uri + ", pid " + pid + " not found, ignored");
            }
        } else {
            if(ocs.getLogService() != null) {
        		ocs.getLogService().log(LogService.LOG_INFO,
        				"Deleting config " + pid + ", uri = " + uri);
            }
            cfg.delete();
        }
	}
	*/
}