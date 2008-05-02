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
package org.apache.sling.osgi.console.web.internal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.sling.osgi.console.web.Action;
import org.apache.sling.osgi.console.web.Render;
import org.apache.sling.osgi.console.web.internal.BaseManagementPlugin;
import org.apache.sling.osgi.console.web.internal.Util;
import org.apache.sling.osgi.console.web.internal.compendium.AjaxConfigManagerAction;
import org.apache.sling.osgi.console.web.internal.compendium.ComponentConfigurationPrinter;
import org.apache.sling.osgi.console.web.internal.compendium.ComponentRenderAction;
import org.apache.sling.osgi.console.web.internal.compendium.ConfigManager;
import org.apache.sling.osgi.console.web.internal.core.AjaxBundleDetailsAction;
import org.apache.sling.osgi.console.web.internal.core.BundleListRender;
import org.apache.sling.osgi.console.web.internal.core.InstallAction;
import org.apache.sling.osgi.console.web.internal.core.RefreshPackagesAction;
import org.apache.sling.osgi.console.web.internal.core.SetStartLevelAction;
import org.apache.sling.osgi.console.web.internal.core.StartAction;
import org.apache.sling.osgi.console.web.internal.core.StopAction;
import org.apache.sling.osgi.console.web.internal.core.UninstallAction;
import org.apache.sling.osgi.console.web.internal.core.UpdateAction;
import org.apache.sling.osgi.console.web.internal.misc.ConfigurationRender;
import org.apache.sling.osgi.console.web.internal.system.GCAction;
import org.apache.sling.osgi.console.web.internal.system.ShutdownAction;
import org.apache.sling.osgi.console.web.internal.system.ShutdownRender;
import org.apache.sling.osgi.console.web.internal.system.VMStatRender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The <code>Sling Manager</code> TODO
 *
 * @scr.component ds="no" label="%manager.name"
 *                description="%manager.description"
 */
public class SlingManager extends GenericServlet {

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /**
     * The name and value of a parameter which will prevent redirection to a
     * render after the action has been executed (value is "_noredir_"). This
     * may be used by programmatic action submissions.
     */
    public static final String PARAM_NO_REDIRECT_AFTER_ACTION = "_noredir_";

    /**
     * @scr.property valueRef="DEFAULT_MANAGER_ROOT"
     */
    private static final String PROP_MANAGER_ROOT = "manager.root";

    /**
     * @scr.property value="list"
     */
    private static final String PROP_DEFAULT_RENDER = "default.render";

    /**
     * @scr.property value="Sling Management Console"
     */
    private static final String PROP_REALM = "realm";

    /**
     * @scr.property value="admin"
     */
    private static final String PROP_USER_NAME = "username";

    /**
     * @scr.property value="admin"
     */
    private static final String PROP_PASSWORD = "password";

    /**
     * The default value for the {@link #PROP_MANAGER_ROOT} configuration
     * property (value is "/system/console").
     */
    private static final String DEFAULT_MANAGER_ROOT = "/system/console";

    private static final Class<?>[] PLUGIN_CLASSES = {
        AjaxConfigManagerAction.class, ComponentConfigurationPrinter.class,
        ComponentRenderAction.class, ConfigManager.class,
        AjaxBundleDetailsAction.class, BundleListRender.class,
        InstallAction.class, RefreshPackagesAction.class,
        SetStartLevelAction.class, StartAction.class, StopAction.class,
        UninstallAction.class, UpdateAction.class,
        ConfigurationRender.class, GCAction.class,
        ShutdownAction.class, ShutdownRender.class, VMStatRender.class };

    private BundleContext bundleContext;

    private Logger log;

    private ServiceTracker httpServiceTracker;

    private HttpService httpService;

    private ServiceTracker operationsTracker;

    private ServiceTracker rendersTracker;

    private ServiceRegistration configurationListener;

    private Map<String, Action> operations = new HashMap<String, Action>();

    private SortedMap<String, Render> renders = new TreeMap<String, Render>();

    private Render defaultRender;

    private String defaultRenderName;

    private String webManagerRoot;

    private Dictionary<String, Object> configuration;

    public SlingManager(BundleContext bundleContext) {

        this.bundleContext = bundleContext;
        this.log = new Logger(bundleContext);

        updateConfiguration(null);

        try {
            this.configurationListener = ConfigurationListener.create(this);
        } catch (Throwable t) {
            // might be caused by CM not available
        }

        // track renders and operations
        operationsTracker = new OperationServiceTracker(this);
        operationsTracker.open();
        rendersTracker = new RenderServiceTracker(this);
        rendersTracker.open();
        httpServiceTracker = new HttpServiceTracker(this);
        httpServiceTracker.open();

        for (Class<?> pluginClass : PLUGIN_CLASSES) {
            try {
                Object plugin = pluginClass.newInstance();
                if (plugin instanceof BaseManagementPlugin) {
                    ((BaseManagementPlugin) plugin).setBundleContext(bundleContext);
                    ((BaseManagementPlugin) plugin).setLogger(log);
                }
                if (plugin instanceof Action) {
                    bindOperation((Action) plugin);
                }
                if (plugin instanceof Render) {
                    bindRender((Render) plugin);
                }
            } catch (Throwable t) {
                // todo: log
            }
        }
    }

    public void dispose() {

        if (configurationListener != null) {
            configurationListener.unregister();
            configurationListener = null;
        }

        if (operationsTracker != null) {
            operationsTracker.close();
            operationsTracker = null;
        }

        if (rendersTracker != null) {
            rendersTracker.close();
            rendersTracker = null;
        }

        if (httpServiceTracker != null) {
            httpServiceTracker.close();
            httpServiceTracker = null;
        }

        // simply remove all operations, we should not be used anymore
        this.defaultRender = null;
        this.operations.clear();
        this.renders.clear();

        if (log != null) {
            log.dispose();
        }

        this.bundleContext = null;
    }

    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // handle the request action, terminate if done
        if (this.handleAction(request, response)) {
            return;
        }

        // check whether we are not at .../{webManagerRoot}
        if (request.getRequestURI().endsWith(this.webManagerRoot)) {
            response.sendRedirect(request.getRequestURI() + "/"
                + this.defaultRender.getName());
            return;
        }

        // otherwise we render the response
        Render render = this.getRender(request);
        if (render == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String current = render.getName();
        boolean disabled = false; // should take action==shutdown into
        // account:
        // Boolean.valueOf(request.getParameter("disabled")).booleanValue();

        PrintWriter pw = Util.startHtml(response, render.getLabel());
        Util.navigation(pw, this.renders.values(), current, disabled);

        render.render(request, response);

        Util.endHhtml(pw);
    }

    protected boolean handleAction(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        // check action
        String actionName = this.getParameter(req, Util.PARAM_ACTION);
        if (actionName != null) {
            Action action = this.operations.get(actionName);
            if (action != null) {
                boolean redirect = true;
                try {
                    redirect = action.performAction(req, resp);
                } catch (IOException ioe) {
                    this.log(ioe.getMessage(), ioe);
                } catch (ServletException se) {
                    this.log(se.getMessage(), se.getRootCause());
                }

                // maybe overwrite redirect
                if (PARAM_NO_REDIRECT_AFTER_ACTION.equals(getParameter(req,
                    PARAM_NO_REDIRECT_AFTER_ACTION))) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/html");
                    resp.getWriter().println("Ok");
                    return true;
                }

                if (redirect) {
                    String uri = req.getRequestURI();
                    // Object pars =
                    // req.getAttribute(Action.ATTR_REDIRECT_PARAMETERS);
                    // if (pars instanceof String) {
                    // uri += "?" + pars;
                    // }
                    resp.sendRedirect(uri);
                }
                return true;
            }
        }

        return false;
    }

    protected Render getRender(HttpServletRequest request) {

        String page = request.getRequestURI();

        // remove trailing slashes
        while (page.endsWith("/")) {
            page = page.substring(0, page.length() - 1);
        }

        // take last part of the name
        int lastSlash = page.lastIndexOf('/');
        if (lastSlash >= 0) {
            page = page.substring(lastSlash + 1);
        }

        Render render = this.renders.get(page);
        return (render == null) ? this.defaultRender : render;
    }

    private String getParameter(HttpServletRequest request, String name) {
        // just get the parameter if not a multipart/form-data POST
        if (!ServletFileUpload.isMultipartContent(new ServletRequestContext(
            request))) {
            return request.getParameter(name);
        }

        // check, whether we alread have the parameters
        @SuppressWarnings("unchecked")
        Map<String, FileItem[]> params = (Map<String, FileItem[]>) request.getAttribute(Util.ATTR_FILEUPLOAD);
        if (params == null) {
            // parameters not read yet, read now
            // Create a factory for disk-based file items
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(256000);

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(-1);

            // Parse the request
            params = new HashMap<String, FileItem[]>();
            try {
                @SuppressWarnings("unchecked")
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem fi : items) {
                    FileItem[] current = params.get(fi.getFieldName());
                    if (current == null) {
                        current = new FileItem[] { fi };
                    } else {
                        FileItem[] newCurrent = new FileItem[current.length + 1];
                        System.arraycopy(current, 0, newCurrent, 0,
                            current.length);
                        newCurrent[current.length] = fi;
                        current = newCurrent;
                    }
                    params.put(fi.getFieldName(), current);
                }
            } catch (FileUploadException fue) {
                // TODO: log
            }
            request.setAttribute(Util.ATTR_FILEUPLOAD, params);
        }

        FileItem[] param = params.get(name);
        if (param != null) {
            for (int i = 0; i < param.length; i++) {
                if (param[i].isFormField()) {
                    return param[i].getString();
                }
            }
        }

        // no valid string parameter, fail
        return null;
    }

    BundleContext getBundleContext() {
        return bundleContext;
    }

    private static class HttpServiceTracker extends ServiceTracker {

        private final SlingManager slingManager;

        HttpServiceTracker(SlingManager slingManager) {
            super(slingManager.getBundleContext(), HttpService.class.getName(),
                null);
            this.slingManager = slingManager;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object operation = super.addingService(reference);
            if (operation instanceof HttpService) {
                slingManager.bindHttpService((HttpService) operation);
            }
            return operation;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof HttpService) {
                slingManager.unbindHttpService((HttpService) service);
            }

            super.removedService(reference, service);
        }
    }

    private static class OperationServiceTracker extends ServiceTracker {

        private final SlingManager slingManager;

        OperationServiceTracker(SlingManager slingManager) {
            super(slingManager.getBundleContext(), Action.SERVICE, null);
            this.slingManager = slingManager;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object operation = super.addingService(reference);
            if (operation instanceof Action) {
                slingManager.bindOperation((Action) operation);
            }
            return operation;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof Action) {
                slingManager.bindOperation((Action) service);
            }

            super.removedService(reference, service);
        }
    }

    private static class RenderServiceTracker extends ServiceTracker {

        private final SlingManager slingManager;

        RenderServiceTracker(SlingManager slingManager) {
            super(slingManager.getBundleContext(), Render.SERVICE, null);
            this.slingManager = slingManager;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            Object operation = super.addingService(reference);
            if (operation instanceof Render) {
                slingManager.bindRender((Render) operation);
            }
            return operation;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (service instanceof Render) {
                slingManager.bindRender((Render) service);
            }

            super.removedService(reference, service);
        }
    }

    protected synchronized void bindHttpService(HttpService httpService) {
        Dictionary<String, Object> config = getConfiguration();

        // get authentication details
        String realm = this.getProperty(config, PROP_REALM,
            "Sling Management Console");
        String userId = this.getProperty(config, PROP_USER_NAME, null);
        String password = this.getProperty(config, PROP_PASSWORD, null);

        // register the servlet and resources
        try {
            HttpContext httpContext = new SlingHttpContext(httpService, realm,
                userId, password);

            Dictionary<String, String> servletConfig = toStringConfig(config);

            // rest of sling
            httpService.registerServlet(this.webManagerRoot, this,
                servletConfig, httpContext);
            httpService.registerResources(this.webManagerRoot + "/res", "/res",
                httpContext);

        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Problem setting up", e);
        }

        this.httpService = httpService;
    }

    protected synchronized void unbindHttpService(HttpService httpService) {
        httpService.unregister(this.webManagerRoot + "/res");
        httpService.unregister(this.webManagerRoot);

        if (this.httpService == httpService) {
            this.httpService = null;
        }
    }

    protected void bindOperation(Action operation) {
        this.operations.put(operation.getName(), operation);
    }

    protected void unbindOperation(Action operation) {
        this.operations.remove(operation.getName());
    }

    protected void bindRender(Render render) {
        this.renders.put(render.getName(), render);

        if (this.defaultRender == null) {
            this.defaultRender = render;
        } else if (render.getName().equals(this.defaultRenderName)) {
            this.defaultRender = render;
        }
    }

    protected void unbindRender(Render render) {
        this.renders.remove(render.getName());

        if (this.defaultRender == render) {
            if (this.renders.isEmpty()) {
                this.defaultRender = null;
            } else {
                this.defaultRender = this.renders.values().iterator().next();
            }
        }
    }

    private Dictionary<String, Object> getConfiguration() {
        return configuration;
    }

    void updateConfiguration(Dictionary<String, Object> config) {
        if (config == null) {
            config = new Hashtable<String, Object>();
        }

        configuration = config;

        defaultRenderName = (String) config.get(PROP_DEFAULT_RENDER);
        if (defaultRenderName != null && renders.get(defaultRenderName) != null) {
            defaultRender = renders.get(defaultRenderName);
        }

        // get the web manager root path
        webManagerRoot = this.getProperty(config, PROP_MANAGER_ROOT, DEFAULT_MANAGER_ROOT);
        if (!webManagerRoot.startsWith("/")) {
            webManagerRoot = "/" + webManagerRoot;
        }

        // might update http service registration
        HttpService httpService = this.httpService;
        if (httpService != null) {
            synchronized (this) {
                unbindHttpService(httpService);
                bindHttpService(httpService);
            }
        }
    }

    /**
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    private String getProperty(Dictionary<String, Object> config, String name,
            String def) {
        Object value = config.get(name);
        if (value instanceof String) {
            return (String) value;
        }

        if (value == null) {
            return def;
        }

        return String.valueOf(value);
    }

    private Dictionary<String, String> toStringConfig(Dictionary<?, ?> config) {
        Dictionary<String, String> stringConfig = new Hashtable<String, String>();
        for (Enumeration<?> ke = config.keys(); ke.hasMoreElements();) {
            Object key = ke.nextElement();
            stringConfig.put(key.toString(), String.valueOf(config.get(key)));
        }
        return stringConfig;
    }
}
