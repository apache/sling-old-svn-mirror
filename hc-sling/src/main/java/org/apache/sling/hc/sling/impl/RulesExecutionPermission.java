/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Used to restrict the execution of health check rules
 *  to authorized users.
 */
public class RulesExecutionPermission {
    /** For now, to be authorized to execute our rules, the current
     *  user needs write access to /libs - it's a realistic
     *  way to check that they are admin, and if they have write access
     *  there they can create a lot of trouble anyway. 
     */
    public static final String REF_PATH = "/libs";
    public static final String REQUIRED_PERMISSION = "add_node";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** Check if the user to which r points is authorized to execute our Rules */
    public void checkPermission(Resource r) throws RepositoryException {
        final Session s = r.getResourceResolver().adaptTo(Session.class);
        if(s == null) {
            log.warn("Adapting {} to a Session returns null, cannot check permissions", r);
            throw new AccessDeniedException("No Session, cannot check permissions");
        }
        s.checkPermission(REF_PATH, REQUIRED_PERMISSION);
    }
}
