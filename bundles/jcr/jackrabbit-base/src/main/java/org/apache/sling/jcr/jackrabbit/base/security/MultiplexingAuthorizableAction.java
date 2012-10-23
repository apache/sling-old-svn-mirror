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

package org.apache.sling.jcr.jackrabbit.base.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.user.action.AbstractAuthorizableAction;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class MultiplexingAuthorizableAction extends AbstractAuthorizableAction implements ServiceTrackerCustomizer{
    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<Comparable,AuthorizableAction> actionMap =
            new ConcurrentSkipListMap<Comparable, AuthorizableAction>(Collections.reverseOrder());
    private final ServiceTracker tracker;
    private final BundleContext context;
    private final ServiceRegistration reg;

    public MultiplexingAuthorizableAction(BundleContext context){
        this.context = context;
        this.tracker = new ServiceTracker(context, createFilter(context),this);
        this.tracker.open();

        Properties p = new Properties();
        p.setProperty("jackrabbit.extension","true");
        reg = context.registerService(AuthorizableAction.class.getName(),this,p);
    }

    //~----------------------------------------<AuthorizableAction>

    public void onCreate(User user, String password, Session session) throws RepositoryException {
        log.debug("Created user {}", user.getID());
        for(AuthorizableAction a : getActions()){
            a.onCreate(user,password,session);
        }
    }

    @Override
    public void onCreate(Group group, Session session) throws RepositoryException {
        log.debug("Created group {}", group.getID());
        for(AuthorizableAction a : getActions()){
            a.onCreate(group,session);
        }
    }

    @Override
    public void onRemove(Authorizable authorizable, Session session) throws RepositoryException {
        log.debug("Removed authorizable {}", authorizable.getID());
        for(AuthorizableAction a : getActions()){
            a.onRemove(authorizable,session);
        }
    }

    @Override
    public void onPasswordChange(User user, String newPassword, Session session) throws RepositoryException {
        log.debug("Password changed for user {}", user.getID());
        for(AuthorizableAction a : getActions()){
            a.onPasswordChange(user,newPassword,session);
        }
    }

    //~----------------------------------------<LifeCycle methods>

    public void open(){
        tracker.open();
    }

    public void close(){
        if(reg != null){
            reg.unregister();
        }
        tracker.close();
        actionMap.clear();
    }

    //~----------------------------------------- < ServiceTrackerCustomizer >

    public Object addingService(ServiceReference reference) {
        AuthorizableAction action = (AuthorizableAction) context.getService(reference);
        actionMap.put(reference,action);
        return action;
    }

    public void modifiedService(ServiceReference reference, Object service) {
        actionMap.put(reference, (AuthorizableAction) service);
    }

    public void removedService(ServiceReference reference, Object service) {
        actionMap.remove(reference);
    }

    private Collection<AuthorizableAction> getActions() {
        return actionMap.values();
    }

    private Filter createFilter(BundleContext context) {
        try {
            //Create a filter such that we track all service excluding ourselves
            return context.createFilter("(&(!(jackrabbit.extension=true))" +
                    "(objectClass=org.apache.jackrabbit.core.security.user.action.AuthorizableAction))");
        } catch (InvalidSyntaxException e) {
            //Should not happen as Filter is hardcoded and should work
            throw new RuntimeException(e);
        }
    }
}
