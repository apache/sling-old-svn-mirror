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
package org.apache.sling.jcr.registration.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.Repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.sling.jcr.registration.AbstractRegistrationSupport;
import org.osgi.service.log.LogService;

/**
 * The <code>RmiRegistrationSupport</code> extends the
 * {@link AbstractRegistrationSupport} class to register repositories with an
 * RMI registry whose provider localhost port may be configured.
 * <p>
 * Note: Currently only registries in this Java VM are supported. In the future
 * support for external registries may be added.
 */
@Component(
        immediate = true,
        metatype = true,
        label = "%rmi.name",
        description = "%rmi.description",
        name = "org.apache.sling.jcr.jackrabbit.server.RmiRegistrationSupport",
        policy = ConfigurationPolicy.REQUIRE)
@org.apache.felix.scr.annotations.Properties({
    @Property(name = "service.vendor", value = "The Apache Software Foundation", propertyPrivate = true),
    @Property(name = "service.description", value = "RMI based Repository Registration", propertyPrivate = true)
})
public class RmiRegistrationSupport extends AbstractRegistrationSupport {

    @Property(intValue = 1099, label = "%rmi.port.name", description = "%rmi.port.description")
    public static final String PROP_REGISTRY_PORT = "port";

    private int registryPort;

    /** The private RMI registry, only defined if possible */
    private Registry registry;

    private boolean registryIsPrivate;

    // ---------- SCR intergration ---------------------------------------------

    /**
     * Read the registry port from the configuration properties. If the value is
     * invalid (higher than 65525), the RMI registry is disabled. Likewise the
     * registry is disabled, if the port property is negative. If the port is
     * zero or not a number, the default port (1099) is assumed.
     */
    protected boolean doActivate() {

        Object portProp = this.getComponentContext().getProperties().get(
            PROP_REGISTRY_PORT);
        if (portProp instanceof Number) {
            this.registryPort = ((Number) portProp).intValue();
        } else {
            try {
                this.registryPort = Integer.parseInt(String.valueOf(portProp));
            } catch (NumberFormatException nfe) {
                this.registryPort = 0;
            }
        }

        // ensure correct value
        if (this.registryPort < 0) {
            this.log(LogService.LOG_WARNING,
                "RMI registry disabled (no or negative RMI port configured)",
                null);
            return false;
        } else if (this.registryPort == 0) {
            this.registryPort = Registry.REGISTRY_PORT;
        } else if (this.registryPort == 0 || this.registryPort > 0xffff) {
            this.log(LogService.LOG_WARNING,
                "Illegal RMI registry port number " + this.registryPort
                    + ", disabling RMI registry", null);
            return false;
        }

        this.log(LogService.LOG_INFO, "Using RMI Registry port "
            + this.registryPort, null);
        return true;
    }

    /**
     * If a private registry has been acquired this method unexports the
     * registry object to free the RMI registry OID for later use.
     */
    protected void doDeactivate() {
        // if we have a private RMI registry, unexport it here to free
        // the RMI registry OID
        if (this.registry != null && this.registryIsPrivate) {
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
                this.log(LogService.LOG_INFO,
                    "Unexported private RMI Registry at " + this.registryPort,
                    null);
            } catch (NoSuchObjectException nsoe) {
                // not expected, but don't really care either
                this.log(LogService.LOG_INFO,
                    "Cannot unexport private RMI Registry reference", nsoe);
            }
        }
        this.registry = null;
    }

    protected Object bindRepository(String name, Repository repository) {
        return new RmiRegistration(name, repository);
    }

    protected void unbindRepository(String name, Object data) {
        RmiRegistration rr = (RmiRegistration) data;
        rr.unregister();
    }

    // ---------- support for private rmi registries ---------------------------

    /**
     * Tries to create a private registry at the configured port. If this fails
     * (for example because a registry already exists in the VM, a registry stub
     * for the port is returned. This latter stub may or may not connect to a
     * real registry, which may only be found out, when trying to register
     * repositories.
     */
    private Registry getPrivateRegistry() {
        if (this.registry == null) {
            try {
                // no, so try to create first
                this.registry = LocateRegistry.createRegistry(this.registryPort);
                this.registryIsPrivate = true;
                this.log(LogService.LOG_INFO, "Using private RMI Registry at "
                    + this.registryPort, null);

            } catch (RemoteException re) {
                // creating failed, check whether there is already one
                this.log(LogService.LOG_INFO,
                    "Cannot create private registry, trying existing registry at "
                        + this.registryPort + ", reason: " + re.toString(),
                    null);

                try {
                    this.registry = LocateRegistry.getRegistry(this.registryPort);
                    this.registryIsPrivate = false;
                    this.log(LogService.LOG_INFO,
                        "Trying existing registry at " + this.registryPort,
                        null);

                } catch (RemoteException pre) {
                    this.log(
                        LogService.LOG_ERROR,
                        "Cannot get existing registry, will not register repositories on RMI",
                        pre);
                }
            }
        }

        return this.registry;
    }

    /**
     * Returns a Jackrabbit JCR RMI <code>RemoteAdapterFactory</code> to be
     * used to publish local (server-side) JCR objects to a remote client.
     * <p>
     * This method returns an instance of the
     * <code>JackrabbitServerAdapterFactory</code> class to enable the use of
     * the Jackrabbit API over RMI. Extensions of this class may overwrite this
     * method to return a different implementation to provide different JCR
     * extension API depending on the server implementation.
     */
    protected RemoteAdapterFactory getRemoteAdapterFactory() {
        return new ServerAdapterFactory();
    }

    // ---------- Inner Class --------------------------------------------------

    private class RmiRegistration {

        private String rmiName;

        private Remote rmiRepository;

        RmiRegistration(String rmiName, Repository repository) {
            this.register(rmiName, repository);
        }

        public String getRmiName() {
            return this.rmiName;
        }

        public String getRmiURL() {
            String host;
            try {
                host = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (IOException ignore) {
                host = "localhost";
            }
            return "//" + host + ":" + RmiRegistrationSupport.this.registryPort
                + "/" + this.getRmiName();
        }

        private void register(String rmiName, Repository repository) {
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");

            // try to create remote repository and keep it to ensure it is
            // unexported in the unregister() method
            try {
                RemoteAdapterFactory raf = getRemoteAdapterFactory();
                this.rmiRepository = raf.getRemoteRepository(repository);
            } catch (RemoteException e) {
                RmiRegistrationSupport.this.log(LogService.LOG_ERROR,
                    "Unable to create remote repository.", e);
                return;
            } catch (Exception e) {
                RmiRegistrationSupport.this.log(
                    LogService.LOG_ERROR,
                    "Unable to create RMI repository. jcr-rmi.jar might be missing.",
                    e);
                return;
            }

            try {
                // check whether we have a private registry already
                Registry registry = RmiRegistrationSupport.this.getPrivateRegistry();
                if (registry != null) {
                    registry.bind(rmiName, this.rmiRepository);
                    this.rmiName = rmiName;
                    RmiRegistrationSupport.this.log(LogService.LOG_INFO,
                        "Repository bound to " + this.getRmiURL(), null);
                }

            } catch (NoSuchObjectException nsoe) {
                // the registry does not really exist
                RmiRegistrationSupport.this.log(LogService.LOG_WARNING,
                    "Cannot contact RMI registry at "
                        + RmiRegistrationSupport.this.registryPort
                        + ", repository not registered", null);
            } catch (Exception e) {
                RmiRegistrationSupport.this.log(LogService.LOG_ERROR,
                    "Unable to bind repository via RMI.", e);
            }
        }

        public void unregister() {
            // unregister repository
            if (this.rmiName != null) {
                try {
                    RmiRegistrationSupport.this.getPrivateRegistry().unbind(
                        this.rmiName);
                    RmiRegistrationSupport.this.log(LogService.LOG_INFO,
                        "Repository unbound from " + this.getRmiURL(), null);
                } catch (Exception e) {
                    RmiRegistrationSupport.this.log(LogService.LOG_ERROR,
                        "Error while unbinding repository from JNDI: " + e,
                        null);
                }
            }

            // drop strong reference to remote repository
            if (this.rmiRepository != null) {
                try {
                    UnicastRemoteObject.unexportObject(this.rmiRepository, true);
                } catch (NoSuchObjectException nsoe) {
                    // not expected, but don't really care either
                    RmiRegistrationSupport.this.log(LogService.LOG_INFO,
                        "Cannot unexport remote Repository reference", nsoe);
                }
            }
        }
    }
}
