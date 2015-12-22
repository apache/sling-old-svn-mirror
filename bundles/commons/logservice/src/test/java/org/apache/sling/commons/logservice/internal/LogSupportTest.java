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
package org.apache.sling.commons.logservice.internal;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;

public class LogSupportTest {
    
    private Bundle bundle;
    private LogSupport logSupport;
    private Logger testLogger;

    @Before
    @SuppressWarnings("unchecked")
    public void prepare() throws Exception {
        
        bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getSymbolicName()).thenReturn("foo.bundle");
        Mockito.when(bundle.getBundleId()).thenReturn(42L);
        
        StartLevel startLevel = Mockito.mock(StartLevel.class);
        logSupport = new LogSupport(startLevel);
        Field loggerField = LogSupport.class.getDeclaredField("loggers");
        loggerField.setAccessible(true);
        Map<Long, Logger> loggers = (Map<Long, Logger>) loggerField.get(logSupport);
        
        testLogger = getMockInfoLogger();
        loggers.put(bundle.getBundleId(), testLogger);
    }
    
    @Test
    public void testServiceEvent() throws Exception {


        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.OBJECTCLASS, new String [] {"some.class.Name"});
        props.put(Constants.SERVICE_ID, 999L);

        ServiceReference sr = Mockito.mock(ServiceReference.class);
        Mockito.when(sr.getBundle()).thenReturn(bundle);
        Mockito.when(sr.getProperty(Mockito.anyString())).then(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return props.get(invocation.getArguments()[0]);
            }
        });
        Mockito.when(sr.getPropertyKeys()).thenReturn(props.keySet().toArray(new String[] {}));
        ServiceEvent se = new ServiceEvent(ServiceEvent.REGISTERED, sr);

        logSupport.serviceChanged(se);

        Mockito.verify(testLogger).info("Service [999, [some.class.Name]] ServiceEvent REGISTERED", (Throwable) null);
    }

    @Test
    public void testEarlyExit() throws Exception {
        
        ServiceReference sr = Mockito.mock(ServiceReference.class);
        LogEntry le = new LogEntryImpl(bundle, sr, LogService.LOG_DEBUG, "test", null);

        logSupport.fireLogEvent(le);

        // The log message is on DEBUG level while the logger is set to INFO level
        // we don't want the actual log.info() call to be made, neither do we want
        // any preparatory work on the log message being made (which involves
        // inspecting the service reference).
        Mockito.verify(testLogger).isTraceEnabled();
        Mockito.verify(testLogger).isDebugEnabled();
        Mockito.verify(testLogger).isInfoEnabled();
        Mockito.verifyNoMoreInteractions(testLogger);
        Mockito.verifyZeroInteractions(sr);
    }

    @Test
    public void testErrorLogger() throws Exception {
        
        Exception e = new Exception();
        LogEntry le = new LogEntryImpl(bundle, null, LogService.LOG_ERROR, "my-error-msg", e);

        logSupport.fireLogEvent(le);
        
        Mockito.verify(testLogger).error("my-error-msg (java.lang.Exception)", e);
    }
    
	@Test
    public void testWarningLogger() throws Exception {
	    
        Exception e = new Exception();
        LogEntry le = new LogEntryImpl(bundle, null, LogService.LOG_WARNING, "my-warning-message", e);

        logSupport.fireLogEvent(le);
        
        Mockito.verify(testLogger).warn("my-warning-message (java.lang.Exception)", e);
    }
	
	@Test
	public void testInfoLogger() throws Exception {
	    
        LogEntry le = new LogEntryImpl(bundle, null, LogService.LOG_INFO, "my-info-message", null);

        logSupport.fireLogEvent(le);
        
        Mockito.verify(testLogger).info("my-info-message", (Throwable) null);
	}

	@Test
	public void testBundleChanges() throws Exception {

        logSupport.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bundle));

        Mockito.verify(testLogger).info("BundleEvent INSTALLED", (Throwable) null);
	}
	
	@Test
	public void testFrameworkEventStarted() throws Exception {
        
        FrameworkEvent frameworkEvent = new FrameworkEvent(FrameworkEvent.STARTED, bundle, null);
        
        logSupport.frameworkEvent(frameworkEvent);
        
        Mockito.verify(testLogger).info("FrameworkEvent STARTED", (Throwable) null);
	}
	
	@Test
	public void testFrameworkEventError() throws Exception {

	    BundleException bundleException = new BundleException("my bundle exception", BundleException.ACTIVATOR_ERROR);
        FrameworkEvent frameworkEvent = new FrameworkEvent(FrameworkEvent.ERROR, bundle, bundleException);
        
        logSupport.frameworkEvent(frameworkEvent);
        
        Mockito.verify(testLogger).error("FrameworkEvent ERROR (org.osgi.framework.BundleException: my bundle exception)", bundleException);
	}
	
    @Test
    public void testGetLevels() {
        Logger traceLogger = Mockito.mock(Logger.class);
        Mockito.when(traceLogger.isTraceEnabled()).thenReturn(true);
        assertEquals(5, LogSupport.getLevel(traceLogger));

        Logger debugLogger = Mockito.mock(Logger.class);
        Mockito.when(debugLogger.isDebugEnabled()).thenReturn(true);
        assertEquals(LogService.LOG_DEBUG, LogSupport.getLevel(debugLogger));

        Logger infoLogger = Mockito.mock(Logger.class);
        Mockito.when(infoLogger.isInfoEnabled()).thenReturn(true);
        assertEquals(LogService.LOG_INFO, LogSupport.getLevel(infoLogger));

        Logger warnLogger = Mockito.mock(Logger.class);
        Mockito.when(warnLogger.isWarnEnabled()).thenReturn(true);
        assertEquals(LogService.LOG_WARNING, LogSupport.getLevel(warnLogger));

        Logger errorLogger = Mockito.mock(Logger.class);
        Mockito.when(errorLogger.isErrorEnabled()).thenReturn(true);
        assertEquals(LogService.LOG_ERROR, LogSupport.getLevel(errorLogger));
    }

    private Logger getMockInfoLogger() {
        Logger testLogger = Mockito.mock(Logger.class);
        Mockito.when(testLogger.isTraceEnabled()).thenReturn(false);
        Mockito.when(testLogger.isDebugEnabled()).thenReturn(false);
        Mockito.when(testLogger.isInfoEnabled()).thenReturn(true);
        Mockito.when(testLogger.isWarnEnabled()).thenReturn(true);
        Mockito.when(testLogger.isErrorEnabled()).thenReturn(true);
        return testLogger;
    }
}
