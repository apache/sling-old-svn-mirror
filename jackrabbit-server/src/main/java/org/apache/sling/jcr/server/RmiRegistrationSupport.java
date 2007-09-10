/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.server;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.Repository;

import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.osgi.service.log.LogService;


/**
 * The <code>RmiRegistrationSupport</code> extends the
 * {@link AbstractRegistrationSupport} class to register repositories with
 * an RMI registry whose provider localhost port may be configured.
 * <p>
 * Note: Currently only registries in this Java VM are supported. In the future
 * support for external registries may be added.
 * 
 * @scr.component immediate="true" label="%rmi.name"
 *                description="%rmi.description"
 * @scr.reference name="Repository" interface="javax.jcr.Repository"
 *                policy="dynamic" cardinality="0..n"
 * @scr.reference name="Log" interface="org.osgi.service.log.LogService"
 *                policy="dynamic" cardinality="0..1"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="RMI based Repository Registration"
 */
public class RmiRegistrationSupport extends AbstractRegistrationSupport {

    /**
     * @scr.property value="1099" type="Integer" label="%rmi.port.name"
     *               description="%rmi.port.description"
     */
    public static final String PROP_REGISTRY_PORT = "port";

    private int registryPort;
    
    /** The private RMI registry, only defined if possible */
    private Registry registry;
    private boolean registryIsPrivate;

    // ---------- SCR intergration ---------------------------------------------

    /**
     * Read the registry port from the configuration properties. If the value
     * is invalid (higher than 65525), the RMI registry is disabled.
     * Likewise the registry is disabled, if the port property is negative. If
     * the port is zero or not a number, the default port (1099) is assumed.
     */
    protected boolean doActivate() {
        
        Object portProp = getComponentContext().getProperties().get(PROP_REGISTRY_PORT);
        registryPort = (portProp instanceof Number)
                ? ((Number) portProp).intValue()
                : 0;
        
        // ensure correct value
        if (registryPort < 0) {
            log(LogService.LOG_WARNING, "RMI registry disabled (no or negative RMI port configured)", null);
            return false;
        } else if (registryPort == 0) {
            registryPort = Registry.REGISTRY_PORT;
        } else if (registryPort == 0 || registryPort > 0xffff) {
            log(LogService.LOG_WARNING, "Illegal RMI registry port number " + registryPort + ", disabling RMI registry", null);
            return false;
        }
        
        log(LogService.LOG_INFO, "Using RMI Registry port " + registryPort, null);
        return true;
    }
    
    /**
     * If a private registry has been acquired this method unexports the
     * registry object to free the RMI registry OID for later use.
     */
    protected void doDeactivate() {
        // if we have a private RMI registry, unexport it here to free
        // the RMI registry OID
        if (registry != null && registryIsPrivate) {
            try {
                UnicastRemoteObject.unexportObject(registry, true);
                log(LogService.LOG_INFO, "Unexported private RMI Registry at "
                    + registryPort, null);
            } catch (NoSuchObjectException nsoe) {
                // not expected, but don't really care either
                log(LogService.LOG_INFO, "Cannot unexport private RMI Registry reference",
                    nsoe);
            }
        }
        registry = null;
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
     * (for example because a registry already exists in the VM, a registry
     * stub for the port is returned. This latter stub may or may not connect
     * to a real registry, which may only be found out, when trying to 
     * register repositories.
     */
    private Registry getPrivateRegistry() {
        if (registry == null) {
            try {
                // no, so try to create first
                registry = LocateRegistry.createRegistry(registryPort);
                registryIsPrivate = true;
                log(LogService.LOG_INFO, "Using private RMI Registry at "
                    + registryPort, null);
                
            } catch (RemoteException re) {
                // creating failed, check whether there is already one
                log(LogService.LOG_INFO,
                    "Cannot create private registry, trying existing registry at "
                        + registryPort + ", reason: " + re.toString(), null);
                
                try {
                    registry = LocateRegistry.getRegistry(registryPort);
                    registryIsPrivate = false;
                    log(LogService.LOG_INFO, "Trying existing registry at "
                        + registryPort, null);
                    
                } catch (RemoteException pre) {
                    log(
                        LogService.LOG_ERROR,
                        "Cannot get existing registry, will not register repositories on RMI",
                        pre);
                }
            }
        }

        return registry;
    }
    
    //---------- Inner Class --------------------------------------------------
    
    private class RmiRegistration {
        
        private String rmiName;
        private Remote rmiRepository;
        
        RmiRegistration(String rmiName, Repository repository) {
            register(rmiName, repository);
        }
        
        public String getRmiName() {
            return rmiName;
        }
        
        public Remote getRmiRepository() {
            return rmiRepository;
        }
        
        public String getRmiURL() {
            String host;
            try {
                host = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (IOException ignore) {
                host = "localhost";
            }
            return "//" + host + ":" + registryPort + "/" + getRmiName();
        }
        
        private void register(String rmiName, Repository repository) {
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");

            // try to create remote repository and keep it to ensure it is
            // unexported in the unregister() method
            try {
                rmiRepository = new ServerAdapterFactory().getRemoteRepository(repository);
            } catch (RemoteException e) {
                log(LogService.LOG_ERROR, "Unable to create remote repository.", e);
                return;
            } catch (Exception e) {
                log(LogService.LOG_ERROR, "Unable to create RMI repository. jcr-rmi.jar might be missing.", e);
                return;
            }

            try {
                // check whether we have a private registry already
                Registry registry = getPrivateRegistry();
                if (registry != null) {
                    registry.bind(rmiName, rmiRepository);
                    this.rmiName = rmiName;
                    log(LogService.LOG_INFO, "Repository bound to " + getRmiURL(), null);
                }
                
            } catch (NoSuchObjectException nsoe) {
                // the registry does not really exist
                log(LogService.LOG_WARNING, "Cannot contact RMI registry at "
                    + registryPort + ", repository not registered", null);
            } catch (Exception e) {
                log(LogService.LOG_ERROR, "Unable to bind repository via RMI.", e);
            }
        }
        
        public void unregister() {
            // unregister repository
            if (rmiName != null) {
                try {
                    getPrivateRegistry().unbind(rmiName);
                    log(LogService.LOG_INFO, "Repository unbound from " + getRmiURL(), null);
                } catch (Exception e) {
                    log(LogService.LOG_ERROR, "Error while unbinding repository from JNDI: " + e, null);
                }
            }

            // drop strong reference to remote repository
            if (rmiRepository != null) {
                try {
                    UnicastRemoteObject.unexportObject(rmiRepository, true);
                } catch (NoSuchObjectException nsoe) {
                    // not expected, but don't really care either
                    log(LogService.LOG_INFO, "Cannot unexport remote Repository reference", nsoe);
                }
            }
        }
    }
}
