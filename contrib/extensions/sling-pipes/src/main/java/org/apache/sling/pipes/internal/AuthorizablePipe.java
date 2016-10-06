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
package org.apache.sling.pipes.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.pipes.BasePipe;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Iterator;

/**
 * pipe that outputs an authorizable resource based on the id set in expr
 */
public class AuthorizablePipe extends BasePipe {
    private static Logger logger = LoggerFactory.getLogger(AuthorizablePipe.class);
    public static final String RESOURCE_TYPE = "slingPipes/authorizable";
    public static final String PN_AUTOCREATEGROUP = "createGroup";
    public static final String PN_ADDTOGROUP = "addToGroup";
    public static final String PN_ADDMEMBERS = "addMembers";
    public static final String PN_BINDMEMBERS = "bindMembers";

    UserManager userManager;
    ResourceResolver resolver;
    boolean autoCreateGroup;
    boolean bindMembers;
    String addToGroup;
    String addMembers;
    Object outputBinding;

    @Override
    public Object getOutputBinding() {
        if (outputBinding != null) {
            return outputBinding;
        }
        return super.getOutputBinding();
    }

    @Override
    public boolean modifiesContent() {
        return autoCreateGroup || StringUtils.isNotBlank(addToGroup) || StringUtils.isNotBlank(addMembers);
    }

    public AuthorizablePipe(Plumber plumber, Resource resource) throws Exception {
        super(plumber, resource);
        resolver = resource.getResourceResolver();
        userManager = resolver.adaptTo(UserManager.class);
        if (getConfiguration() != null) {
            ValueMap properties = getConfiguration().adaptTo(ValueMap.class);
            autoCreateGroup = properties.get(PN_AUTOCREATEGROUP, false);
            bindMembers = properties.get(PN_BINDMEMBERS, false);
            addToGroup = properties.get(PN_ADDTOGROUP, String.class);
            addMembers = properties.get(PN_ADDMEMBERS, String.class);
        }
    }

    @Override
    public Iterator<Resource> getOutput() {
        try {
            Authorizable auth = getAuthorizable();
            if (auth != null) {
                logger.debug("Retrieved authorizable {}", auth.getID());
                if (StringUtils.isNotBlank(addToGroup)){
                    addToGroup(auth);
                }
                if (StringUtils.isNotBlank(addMembers)){
                    addMembers(auth);
                }
                if (bindMembers){
                    bindMembers(auth);
                }
                Resource resource = resolver.getResource(auth.getPath());
                return Collections.singleton(resource).iterator();
            }
        } catch (Exception e){
        }
        return EMPTY_ITERATOR;
    }

    /**
     * Returns the authorizable configured by its expression, creating it if
     * not present and if <code>autoCreateGroup</code> is set to true, or, if
     * no expression, tries to resolve getInput() as an authorizable
     * @return
     * @throws RepositoryException
     */
    protected Authorizable getAuthorizable() {
        Authorizable auth = null;
        try {
            String authId = getExpr();
            if (StringUtils.isNotBlank(authId)) {
                logger.debug("try to find authorizable {}", authId);
                auth = userManager.getAuthorizable(authId);
                if (auth == null && autoCreateGroup) {
                    logger.info("authorizable {} does not exist, creating", authId);
                    auth = userManager.createGroup(authId);
                }
            } else {
                Resource resource = getInput();
                if (resource != null) {
                    auth = userManager.getAuthorizableByPath(resource.getPath());
                }
            }
        } catch (Exception e){
            logger.error("unable to output authorizable based on configuration", e);
        }
        return auth;
    }

    /**
     * Add current authorizable to configured addToGroup expression (should resolve as a group id)
     * @param auth
     */
    protected void addToGroup(Authorizable auth){
        try {
            //if addToGroup is set to true, we try to find the corresponding
            //group and to add current auth to it as a member
            String groupId = bindings.instantiateExpression(addToGroup);
            Authorizable groupAuth = (Group) userManager.getAuthorizable(groupId);
            if (groupAuth != null && groupAuth.isGroup()) {
                logger.info("adding {} to {}", auth.getID(), groupId);
                if (! isDryRun()) {
                    ((Group) groupAuth).addMember(auth);
                }
            }
        } catch (Exception e){
            logger.error("Unable to add current authorizable to group {}", addToGroup, e);
        }
    }

    /**
     * Add to current authorizable (that should be a group) the configured members in addMembers expression
     * @param auth
     */
    protected void addMembers(Authorizable auth) {
        try {
            if (auth.isGroup()) {
                Group group = (Group)auth;
                String uids = bindings.instantiateExpression(addMembers);
                JSONArray array = new JSONArray(uids);
                for (int index = 0; index < array.length(); index ++){
                    String uid = array.getString(index);
                    Authorizable member = userManager.getAuthorizable(uid);
                    if (member != null) {
                        logger.info("adding {} to group {}", member.getID(), group.getID());
                        if (!isDryRun()) {
                            group.addMember(member);
                        }
                    } else {
                        logger.error("computed uid {} doesn't exist, doing nothing", uid);
                    }
                }
            } else {
                logger.error("{} is not a group, can't add members", auth.getID());
            }
        } catch (Exception e){
            logger.error("unable to add members {}", addMembers, e);
        }
    }

    /**
     * add current group's members to the bindings
     * @param auth
     */
    protected void bindMembers(Authorizable auth){
        try {
            if (auth.isGroup()){
                Group group = (Group)auth;
                Iterator<Authorizable> memberIterator = group.getMembers();
                JSONArray array = new JSONArray();
                while (memberIterator.hasNext()){
                    array.put(memberIterator.next().getID());
                }
                outputBinding = array.toString();
            } else {
                logger.error("{} is not a group, unable to bind members", auth.getID());
            }
        } catch (Exception e){
            logger.error("unable to bind members");
        }
    }
}
