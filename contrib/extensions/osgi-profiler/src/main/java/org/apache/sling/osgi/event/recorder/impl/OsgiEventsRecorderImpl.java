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
package org.apache.sling.osgi.event.recorder.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.osgi.event.recorder.OsgiEventsRecorder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Events recorder service implementation.
 *  
 * 	@scr.component immediate="true"
 * 	@scr.property name="service.vendor" value="The Apache Software Foundation"
 * 	@scr.property name="service.description" value="OSGi events recorder"
 * 	@scr.service
 */
public class OsgiEventsRecorderImpl 
	implements OsgiEventsRecorder, BundleListener, FrameworkListener, ServiceListener, ConfigurationListener {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private ServiceRegistration configReg;
	private BundleContext ctx;
	private long startupTime;
	private long lastTimestamp;
	private List<RecordedEvent> events;
	
	static final String ENTITY_FRAMEWORK = "Framework";
	static final String ENTITY_BUNDLE = "Bundle";
	static final String ENTITY_CONFIG = "Config";
	static final String ENTITY_SERVICE = "Service";
	
    /** @scr.property type="Integer" valueRef="DEFAULT_MAX_EVENTS_RECORDED" */
    public static final String PROP_MAX_EVENTS_RECORDED = "max.events.recorded";
    public static final int DEFAULT_MAX_EVENTS_RECORDED = 5000;
    private int maxEvents;
    
    /** @scr.property type="Boolean" valueRef="DEFAULT_ACTIVE" */
    public static final String PROP_ACTIVE = "recorder.active";
    public static final boolean DEFAULT_ACTIVE = false;
    private boolean active;
    
	public void activate(ComponentContext cc) {
        Dictionary<?, ?> cfg = cc.getProperties();
        Object o = cfg.get(PROP_ACTIVE);
        if(o != null) {
        	active = ((Boolean)o).booleanValue();
        } else {
        	active = DEFAULT_ACTIVE;
        }
        o = cfg.get(PROP_MAX_EVENTS_RECORDED);
        if(o != null) {
        	maxEvents = ((Integer)o).intValue();
        } else {
        	maxEvents = DEFAULT_MAX_EVENTS_RECORDED;
        }
        
		if(!active) {
			log.info("Recorder deactivated by configuration");
			return;
		}
		log.info("Activating recorder, a maximum of {} events will be recorded", maxEvents);
		
		startupTime = System.currentTimeMillis();
		ctx = cc.getBundleContext();
		ctx.addBundleListener(this);
		ctx.addFrameworkListener(this);
		ctx.addServiceListener(this);
		configReg = ctx.registerService(ConfigurationListener.class.getName(),
				this, null);
		
		events = new LinkedList<RecordedEvent>();

		log.warn("The OSGi event recorder consumes resources when recording events, turn it off if not using it");
	}

	public void deactivate(ComponentContext cc) {
		configReg.unregister();
		ctx.removeServiceListener(this);
		ctx.removeFrameworkListener(this);
		ctx.removeBundleListener(this);
		events = null;
	}
	
	public Iterator<RecordedEvent> getEvents() {
		if(events == null) {
			return new LinkedList<RecordedEvent>().iterator();
		}
		return events.iterator();
	}
	
	public void clear() {
		if(events != null) {
			synchronized (events) {
				events.clear();
				startupTime = System.currentTimeMillis();
			}
		}
	}

	public long getStartupTimestamp() {
		return startupTime;
	}
	
	public long getLastTimestamp() {
		return lastTimestamp;
	}
	
	public boolean isActive() {
		return active;
	}

	private void recordEvent(String entity, String id, String action) {
		final RecordedEvent r = new RecordedEvent(entity, id, action);
		synchronized (events) {
			while(events.size() > maxEvents) {
				events.remove(0);
			}
			// First event resets start time
			if(events.size() == 0) {
				startupTime = System.currentTimeMillis();
			}
			events.add(r);
			lastTimestamp = r.timestamp;
		}
	}
	
	public void frameworkEvent(FrameworkEvent e) {
		if(events != null) {
			recordEvent(ENTITY_FRAMEWORK, null, getFrameworkEventType(e.getType()));
		}
	}

	public void bundleChanged(BundleEvent e) {
		if(events != null) {
			recordEvent(ENTITY_BUNDLE, e.getBundle().getSymbolicName(), getBundleEventType(e.getType()));
		}
	}

	public void configurationEvent(ConfigurationEvent e) {
		if(events != null) {
			recordEvent(ENTITY_CONFIG, e.getPid(), "CHANGED");
		}
	}

	public void serviceChanged(ServiceEvent e) {
		if(events != null) {
			final ServiceReference ref = e.getServiceReference();
			final StringBuilder id = new StringBuilder();
			final Object pid = ref.getProperty("service.pid");
			final Object sid = ref.getProperty("service.id");
			if(pid != null) {
				id.append(pid.toString());
			} else {
				final Object o = ref.getProperty("objectClass");
				if(o instanceof String []) {
					id.append(Arrays.asList((String[])o).toString());
				} else {
					id.append(o.toString());
				}
				id.append(" (");
				id.append(ref.getBundle().getSymbolicName());
				id.append(" bundle)");
			}
			if(sid != null) {
				id.append(" (id=");
				id.append(sid);
				id.append(")");
			}
			recordEvent(ENTITY_SERVICE,  id.toString(), getServiceEventType(e.getType()));
		}
	}
	
	private static String getBundleEventType(int t) {
		if(t == BundleEvent.STARTED) {
			return "STARTED";
		} else if(t == BundleEvent.RESOLVED) {
			return "RESOLVED";
		} else if(t == BundleEvent.STOPPED) {
			return "STOPPED";
		} else if(t == BundleEvent.UNRESOLVED) {
			return "UNRESOLVED";
		} else if(t == BundleEvent.UNINSTALLED) {
			return "UNINSTALLED";
		} else if(t == BundleEvent.INSTALLED) {
			return "INSTALLED";
		} else if(t == BundleEvent.UPDATED) {
			return "UPDATED";
		}
		return String.valueOf(t);
	}
	
	private static String getFrameworkEventType(int t) {
		if(t == FrameworkEvent.STARTED) {
			return "STARTED";
		} else if(t == FrameworkEvent.ERROR) {
			return "ERROR";
		} else if(t == FrameworkEvent.INFO) {
			return "INFO";
		} else if(t == FrameworkEvent.PACKAGES_REFRESHED) {
			return "PACKAGES_REFRESHED";
		} else if(t == FrameworkEvent.STARTLEVEL_CHANGED) {
			return "STARTLEVEL_CHANGED";
		} else if(t == FrameworkEvent.WARNING) {
			return "WARNING";
		}
		return String.valueOf(t);
	}
	
	private static String getServiceEventType(int t) {
		if(t == ServiceEvent.MODIFIED) {
			return "MODIFIED";
		} else if(t == ServiceEvent.REGISTERED) {
			return "REGISTERED";
		} else if(t == ServiceEvent.UNREGISTERING) {
			return "UNREGISTERING";
		}
		return String.valueOf(t);
	}

}