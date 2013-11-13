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

package org.apache.sling.commons.log.logback.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.sling.commons.log.logback.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import static org.apache.sling.commons.log.logback.internal.ConfigSourceTracker.ConfigSourceInfo;

/**
 * Joran action enabling integration between OSGi and Logback. It is based on
 * {@link ch.qos.logback.core.joran.action.IncludeAction}. It supports including
 * config fragments provided through OSGi ServiceRegistry
 */
public class OsgiInternalAction extends Action {
    private static final String INCLUDED_TAG = "included";


    @SuppressWarnings("unchecked")
    @Override
    public void begin(InterpretationContext ec, String name, Attributes attributes) throws ActionException {
        ec.addInPlayListener(new ConfigCompleteListener(ec));

        populateSubstitutionProperties(ec);

        // TO CHECK Should we add the config fragment at end
        final Collection<ConfigSourceInfo> providers = getFragmentProviders();
        List<SaxEvent> consolidatedEventList = new ArrayList<SaxEvent>();
        for (ConfigSourceInfo cp : providers) {
            InputSource is = cp.getConfigProvider().getConfigSource();
            try {
                SaxEventRecorder recorder = new SaxEventRecorder(context);
                recorder.recordEvents(is);
                // remove the <included> tag from the beginning and </included>
                // from the end
                trimHeadAndTail(recorder);
                consolidatedEventList.addAll(recorder.getSaxEventList());
            } catch (JoranException e) {
                addError("Error while parsing xml obtained from  [" + cp + "]", e);
            } finally {
                Util.close(is);
            }
        }

        // offset = 2, because we need to get past this element as well as the
        // end element
        ec.getJoranInterpreter().getEventPlayer().addEventsDynamically(consolidatedEventList, 2);

    }

    private void populateSubstitutionProperties(InterpretationContext ec) {
        getLogbackManager().addSubsitutionProperties(ec);
    }

    @Override
    public void end(InterpretationContext ec, String name) throws ActionException {
        // do nothing
    }

    private Collection<ConfigSourceInfo> getFragmentProviders() {
        ConfigSourceTracker tracker = (ConfigSourceTracker) getContext().getObject(ConfigSourceTracker.class.getName());
        if (tracker != null) {
            return tracker.getSources();
        }
        return Collections.emptyList();
    }

    private LogbackManager getLogbackManager() {
        LogbackManager lm = (LogbackManager) getContext().getObject(LogbackManager.class.getName());
        if (lm == null) {
            throw new IllegalStateException("LogbackManager not found in Context map");
        }
        return lm;
    }

    private static void trimHeadAndTail(SaxEventRecorder recorder) {
        // Let's remove the two <included> events before
        // adding the events to the player.
        List<SaxEvent> saxEventList = recorder.saxEventList;

        if (saxEventList.size() == 0) {
            return;
        }

        SaxEvent first = saxEventList.get(0);
        if (first != null && first.qName.equalsIgnoreCase(INCLUDED_TAG)) {
            saxEventList.remove(0);
        }

        SaxEvent last = saxEventList.get(recorder.saxEventList.size() - 1);
        if (last != null && last.qName.equalsIgnoreCase(INCLUDED_TAG)) {
            saxEventList.remove(recorder.saxEventList.size() - 1);
        }
    }

    /**
     * Logback does not provide any standard hook point through which we can be
     * notified of config complete. Hence as a work around we listen for EndEvent for
     * 'configuration' tag and then fire the listeners
     *
     * Also Logback does not expose the configured appenders map which it maintains
     * in InterpretationContext. ConfigCompleteListener would extract that map
     * and would export it to LoggerContext Object map so that any
     * listener can make use of that
     */
    private class ConfigCompleteListener implements InPlayListener {
        private static final String CONFIG_TAG = "configuration";
        private final String[] OBJECT_NAMES = {
                ActionConst.APPENDER_BAG,
                OsgiAppenderRefInternalAction.OSGI_APPENDER_REF_BAG,
        };

        private final InterpretationContext ic;

        public ConfigCompleteListener(InterpretationContext ec) {
            this.ic = ec;
        }

        @Override
        public void inPlay(SaxEvent event) {
            if(event instanceof EndEvent
                    && event.qName.equalsIgnoreCase(CONFIG_TAG)){
                //Export the appender bag to LoggerContext object
                transferObjectsToContext();

                getLogbackManager().fireResetCompleteListeners();

                //Clear the appender bag entry
                removeTransferredObjects();
            }
        }

        private void transferObjectsToContext(){
            for(String name : OBJECT_NAMES){
                getContext().putObject(name,ic.getObjectMap().get(name));
            }
        }

        private void removeTransferredObjects(){
            for(String name : OBJECT_NAMES){
                getContext().putObject(name,null);
            }
        }
    }


}
