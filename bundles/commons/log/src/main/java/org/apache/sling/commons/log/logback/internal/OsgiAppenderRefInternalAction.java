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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import org.xml.sax.Attributes;

/**
 * Joran action enabling referring to OSGi based appenders from within Logback config file. It is based on
 * {@link ch.qos.logback.core.joran.action.AppenderRefAction}.
 */
public class OsgiAppenderRefInternalAction extends Action {
    static String OSGI_APPENDER_REF_BAG = "OSGI_APPENDER_BAG";

    boolean inError = false;

    @Override
    public void begin(InterpretationContext ec, String tagName, Attributes attributes) throws ActionException {
        // Let us forget about previous errors (in this object)
        inError = false;

        Object o = ec.peekObject();

        if (!(o instanceof Logger)) {
            String errMsg = "Could not find an Logger at the top of execution stack. Near ["
                    + tagName + "] line " + getLineNumber(ec);
            inError = true;
            addError(errMsg);
            return;
        }

        Logger logger = (Logger) o;

        String appenderName = ec.subst(attributes.getValue(ActionConst.REF_ATTRIBUTE));

        if (OptionHelper.isEmpty(appenderName)) {
            // print a meaningful error message and return
            String errMsg = "Missing appender ref attribute in <appender-ref> tag.";
            inError = true;
            addError(errMsg);

            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> appenderBag =
                (Map<String, Set<String>>) ec.getObjectMap().get(OSGI_APPENDER_REF_BAG);

        if(appenderBag == null){
            appenderBag = new HashMap<String, Set<String>>();
            ec.getObjectMap().put(OSGI_APPENDER_REF_BAG,appenderBag);
        }

        Set<String> loggers = appenderBag.get(appenderName);
        if(loggers == null){
            loggers = new HashSet<String>();
            appenderBag.put(appenderName,loggers);
        }

        loggers.add(logger.getName());
    }

    @Override
    public void end(InterpretationContext ic, String name) throws ActionException {

    }
}
