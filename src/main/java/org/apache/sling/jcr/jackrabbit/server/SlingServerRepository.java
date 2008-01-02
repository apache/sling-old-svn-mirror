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
package org.apache.sling.jcr.jackrabbit.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Dictionary;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.sling.jcr.api.AbstractSlingRepository;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * The <code>RepositorySPIImpl</code> TODO
 *
 * @scr.component label="%repository.name" description="%repository.description"
 *          factory="org.apache.sling.jcr.jackrabbit.server.SlingServerRepositoryFactory"
 *
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="Factory for embeded Jackrabbit Repository Instances"
 *
 * @scr.service
 *
 * @scr.property value="default" name="defaultWorkspace"
 * @scr.property value="anonymous" name="anonymous.name"
 * @scr.property value="anonymous" name="anonymous.password"
 * @scr.property value="admin" name="admin.name"
 * @scr.property value="admin" name="admin.password"
 * @scr.property value="-1" type="Integer" name="pool.maxActive"
 * @scr.property value="10" type="Integer" name="pool.maxIdle"
 * @scr.property value="1" type="Integer" name="pool.maxActiveWait"
 */
public class SlingServerRepository extends AbstractSlingRepository
        implements Repository, SlingRepository {

    /**
     * @scr.property value="true" type="Boolean"
     */
    public static final String PROPERTY_REPOSITORY_AUTOSTART = "autostart";

    /**
     * The name of the configuration property defining the URL to the
     * repository configuration file (value is
     * "config").
     * <p>
     * If the configuration file is located in the local file system, the
     * "file:" scheme must still be specified.
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     *
     * @scr.property value=""
     */
    public static final String REPOSITORY_CONFIG_URL = "config";

    /**
     * The name of the configuration property defining the file system
     * directory where the repository files are located (value is
     * "home").
     * <p>
     * This parameter is mandatory for this activator to start the repository.
     *
     * @scr.property value=""
     */
    public static final String REPOSITORY_HOME_DIR = "home";

    /**
     * @scr.property value=""
     */
    public static final String REPOSITORY_REGISTRATION_NAME = "name";

    /**
     * @scr.reference
     */
    private LogService log;

    private RepositoryImpl delegatee;

    //---------- AbstractSlingRepository methods ------------------------------

    protected Repository getDelegatee() throws RepositoryException {
        if (this.delegatee == null) {
            try {
                this.delegatee = (RepositoryImpl) this.getRepository();
            } catch (IOException ioe) {
                throw new RepositoryException(ioe.getMessage(), ioe);
            }
        }

        return this.delegatee;
    }

    protected LogService getLog() {
        return this.log;
    }

    //---------- SCR integration ----------------------------------------------

    // activate this service
    protected void activate(ComponentContext componentContext) throws Exception {
        // set up the base class (session pooling etc)
        super.activate(componentContext);

        // setup the repository from descriptor
        Object autoStart = componentContext.getProperties().get(PROPERTY_REPOSITORY_AUTOSTART);
        if (autoStart instanceof Boolean && ((Boolean) autoStart).booleanValue()) {
            // have the exception thrown go up the chain ...
            this.getDelegatee();
        }
    }

    // deactivate this service
    protected void deactivate(ComponentContext componentContext) {
        if (this.delegatee != null) {
            try {
                this.delegatee.shutdown();
            } catch (Throwable t) {
                this.log(LogService.LOG_ERROR, "Unexpected problem shutting down repository", t);
            }
        }

        // deactivate the base class (session pooling etc.)
        super.deactivate(componentContext);
    }

    //---------- Repository Publication ---------------------------------------

    private Repository getRepository() throws RepositoryException, IOException {
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> environment = this.getComponentContext().getProperties();

        String configURLObj = (String) environment.get(REPOSITORY_CONFIG_URL);
        String home = (String) environment.get(REPOSITORY_HOME_DIR);

        InputStream ins = null;

        // check whether the URL is a file path
        File configFile = new File(configURLObj);
        if (configFile.canRead()) {
            ins = new FileInputStream(configFile);
        } else {
            URL configURL = new URL(configURLObj);
            ins = configURL.openStream();
        }

        try {
            RepositoryConfig crc = RepositoryConfig.create(ins, home);
            return RepositoryImpl.create(crc);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }


    //---------- Helper -------------------------------------------------------

    public static void copyFile(Bundle bundle, String entryPath, File destFile) throws FileNotFoundException, IOException {
        if (destFile.canRead()) {
            // nothing to do, file exists
            return;
        }

        // copy from property
        URL entryURL = bundle.getEntry(entryPath);
        if (entryURL == null) {
            throw new FileNotFoundException(entryPath);
        }

        // check for a file property
        InputStream source = entryURL.openStream();

        OutputStream dest = null;
        try {

            // ensure path to parent folder of licFile
            destFile.getParentFile().mkdirs();

            dest = new FileOutputStream(destFile);
            byte[] buf = new byte[2048];
            int rd;
            while ( (rd = source.read(buf)) >= 0) {
                dest.write(buf, 0, rd);
            }


        } finally {
            if (dest != null) {
                try {
                    dest.close();
                } catch (IOException ignore) {
                }
            }

            // licSource is not null
            try {
                source.close();
            } catch (IOException ignore) {
            }
        }
    }
}
