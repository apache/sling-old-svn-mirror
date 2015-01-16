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

package org.apache.sling.scripting.jsp.jasper.compiler;

import java.io.File;
import java.io.FilePermission;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.sling.scripting.jsp.jasper.Constants;
import org.apache.sling.scripting.jsp.jasper.IOProvider;
import org.apache.sling.scripting.jsp.jasper.Options;
import org.apache.sling.scripting.jsp.jasper.runtime.JspFactoryImpl;
import org.apache.sling.scripting.jsp.jasper.security.SecurityClassLoad;
import org.apache.sling.scripting.jsp.jasper.servlet.JspServletWrapper;

/**
 * Class for tracking JSP compile time file dependencies when the
 * &060;%@include file="..."%&062; directive is used.
 *
 * A background thread periodically checks the files a JSP page
 * is dependent upon.  If a dpendent file changes the JSP page
 * which included it is recompiled.
 *
 * Only used if a web application context is a directory.
 *
 * @author Glenn L. Nielsen
 * @version $Revision: 505593 $
 */
public final class JspRuntimeContext {

    // Logger
    private final Log log = LogFactory.getLog(JspRuntimeContext.class);

    /** The {@link IOProvider} used to get access to output */
    private final IOProvider ioProvider;

    /** This is a delegate forwarding either to our own factory
     * or the original one.
     * Depending on the USE_OWN_FACTORY flag.
     */
    public static final class JspFactoryHandler extends JspFactory {

        private final JspFactory original;

        private final JspFactory own;

        public JspFactoryHandler(final JspFactory orig, final JspFactory own) {
            this.original = (orig instanceof JspFactoryHandler ? ((JspFactoryHandler)orig).original : orig);
            this.own = own;
        }

        private JspFactory getFactory() {
            final Integer useOwnFactory = USE_OWN_FACTORY.get();
            if ( useOwnFactory == null || useOwnFactory.intValue() == 0 ) {
                return this.original;
            }
            return this.own;
        }

        @Override
        public PageContext getPageContext(Servlet paramServlet,
                ServletRequest paramServletRequest,
                ServletResponse paramServletResponse, String paramString,
                boolean paramBoolean1, int paramInt, boolean paramBoolean2) {
            return this.getFactory().getPageContext(paramServlet, paramServletRequest,
                    paramServletResponse, paramString, paramBoolean1,
                    paramInt, paramBoolean2);
        }

        @Override
        public void releasePageContext(PageContext paramPageContext) {
            this.getFactory().releasePageContext(paramPageContext);
        }

        @Override
        public JspEngineInfo getEngineInfo() {
            return this.getFactory().getEngineInfo();
        }

        @Override
        public JspApplicationContext getJspApplicationContext(
                ServletContext paramServletContext) {
            return this.getFactory().getJspApplicationContext(paramServletContext);
        }

        /**
         * Reset the jsp factory.
         */
        public void destroy() {
            final JspFactory current = JspFactory.getDefaultFactory();
            if ( current == this ) {
                JspFactory.setDefaultFactory(this.original);
            }
        }

        public void incUsage() {
            final Integer count = JspRuntimeContext.USE_OWN_FACTORY.get();
            int newCount = 1;
            if ( count != null ) {
                newCount = count + 1;
            }
            JspRuntimeContext.USE_OWN_FACTORY.set(newCount);
        }

        public void decUsage() {
            final Integer count = JspRuntimeContext.USE_OWN_FACTORY.get();
            JspRuntimeContext.USE_OWN_FACTORY.set(count - 1);
        }

        public int resetUsage() {
            final Integer count = JspRuntimeContext.USE_OWN_FACTORY.get();
            JspRuntimeContext.USE_OWN_FACTORY.set(0);
            return count;
        }

        public void setUsage(int count) {
            JspRuntimeContext.USE_OWN_FACTORY.set(count);
        }
    }

    /**
     * Preload classes required at runtime by a JSP servlet so that
     * we don't get a defineClassInPackage security exception.
     * And set jsp factory
     */
    public static JspFactoryHandler initFactoryHandler() {
        JspFactoryImpl factory = new JspFactoryImpl();
        SecurityClassLoad.securityClassLoad(factory.getClass().getClassLoader());
        if( System.getSecurityManager() != null ) {
            String basePackage = "org.apache.sling.scripting.jsp.jasper.";
            try {
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "runtime.JspFactoryImpl$PrivilegedGetPageContext");
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "runtime.JspFactoryImpl$PrivilegedReleasePageContext");
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "runtime.JspRuntimeLibrary");
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "runtime.JspRuntimeLibrary$PrivilegedIntrospectHelper");
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "runtime.ServletResponseWrapperInclude");
                factory.getClass().getClassLoader().loadClass( basePackage +
                                                               "servlet.JspServletWrapper");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        }

        final JspFactoryHandler key = new JspFactoryHandler(JspFactory.getDefaultFactory(), factory);
        JspFactory.setDefaultFactory(key);
        return key;
    }

    private static final ThreadLocal<Integer> USE_OWN_FACTORY = new ThreadLocal<Integer>();

    // ----------------------------------------------------------- Constructors

    /**
     * Create a JspRuntimeContext for a web application context.
     *
     * Loads in any previously generated dependencies from file.
     *
     * @param context ServletContext for web application
     */
    public JspRuntimeContext(ServletContext context, Options options, final IOProvider ioProvider) {

        this.context = context;
        this.options = options;
        this.ioProvider = ioProvider;

        if (Constants.IS_SECURITY_ENABLED) {
            initSecurity();
        }
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * This web applications ServletContext
     */
    private ServletContext context;
    private Options options;
    private PermissionCollection permissionCollection;

    /**
     * Maps JSP pages to their JspServletWrapper's
     */
    private final ConcurrentHashMap<String, JspServletWrapper> jsps = new ConcurrentHashMap<String, JspServletWrapper>();

    /**
     * Maps dependencies to the using jsp.
     */
    private final Map<String, Set<String>> depToJsp = new HashMap<String, Set<String>>();

    /**
     * Locks for loading tag files.
     */
    private final ConcurrentHashMap<String, Lock> tagFileLoadingLocks = new ConcurrentHashMap<String, Lock>();

    // ------------------------------------------------------ Public Methods

    public void addJspDependencies(final JspServletWrapper jsw, final List<String> deps) {
        if ( deps != null ) {
            final String jspUri = jsw.getJspUri();
            synchronized ( depToJsp ) {
                for(final String dep : deps) {
                    Set<String> set = depToJsp.get(dep);
                    if ( set == null ) {
                        set = new HashSet<String>();
                        depToJsp.put(dep, set);
                    }
                    set.add(jspUri);
                }
            }
        }
    }

    /**
     * Handle jsp modifications
     */
    public boolean handleModification(final String scriptName) {
        if ( log.isDebugEnabled() ) {
            log.debug("Handling modification " + scriptName);
        }

        JspServletWrapper wrapper = jsps.remove(scriptName);

        // first check if jsps contains this
        boolean removed = this.invalidate(wrapper);

        final Set<String> deps;
        synchronized ( depToJsp ) {
            deps = depToJsp.remove(scriptName);
        }
        if ( deps != null ) {
            for(final String dep : deps) {
                wrapper = jsps.remove(dep);
                removed |= this.invalidate(wrapper);
            }
        }
        return removed;
    }

    /**
     * Invalidate a wrapper and destroy it.
     */
    private boolean invalidate(final JspServletWrapper wrapper) {
        if ( wrapper != null ) {
            if ( log.isDebugEnabled() ) {
                log.debug("Invalidating jsp " + wrapper.getJspUri());
            }
            wrapper.destroy(true);
            return true;
        }
        return false;
    }

    /**
     * Add a new wrapper
     *
     * @param jspUri JSP URI
     * @param jsw Servlet wrapper for JSP
     */
    public JspServletWrapper addWrapper(final String jspUri, final JspServletWrapper jsw) {
        final JspServletWrapper previous = jsps.putIfAbsent(jspUri, jsw);
        if ( previous == null ) {
            addJspDependencies(jsw, jsw.getDependants());
            return jsw;
        }
        return previous;
    }

    /**
     * Get an already existing JspServletWrapper.
     *
     * @param jspUri JSP URI
     * @return JspServletWrapper for JSP
     */
    public JspServletWrapper getWrapper(final String jspUri) {
        return jsps.get(jspUri);
    }

    /**
     * Locks a tag file path. Use this before loading it.
     * @param tagFilePath Tag file path
     */
    public void lockTagFileLoading(final String tagFilePath) {
        final Lock lock = getTagFileLoadingLock(tagFilePath);
        lock.lock();
    }

    /**
     * Unlocks a tag file path. Use this after loading it.
     * @param tagFilePath Tag file path
     */
    public void unlockTagFileLoading(final String tagFilePath) {
        final Lock lock = getTagFileLoadingLock(tagFilePath);
        lock.unlock();
    }

    /**
     * Process a "destroy" event for this web application context.
     */
    public void destroy() {
        Iterator<JspServletWrapper> servlets = jsps.values().iterator();
        while (servlets.hasNext()) {
            servlets.next().destroy(false);
        }
        jsps.clear();
        synchronized ( depToJsp ) {
            depToJsp.clear();
        }
    }

    /**
     * Returns the current {@link IOProvider} of this context.
     */
    public IOProvider getIOProvider() {
        return ioProvider;
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Method used to initialize SecurityManager data.
     */
    private void initSecurity() {

        // Setup the PermissionCollection for this web app context
        // based on the permissions configured for the root of the
        // web app context directory, then add a file read permission
        // for that directory.
        Policy policy = Policy.getPolicy();
        if( policy != null ) {
            try {
                // Get the permissions for the web app context
                String docBase = context.getRealPath("/");
                if( docBase == null ) {
                    docBase = options.getScratchDir().toString();
                }
                String codeBase = docBase;
                if (!codeBase.endsWith(File.separator)){
                    codeBase = codeBase + File.separator;
                }
                File contextDir = new File(codeBase);
                URL url = contextDir.getCanonicalFile().toURL();
                final CodeSource codeSource = new CodeSource(url,(Certificate[])null);
                permissionCollection = policy.getPermissions(codeSource);

                // Create a file read permission for web app context directory
                if (!docBase.endsWith(File.separator)){
                    permissionCollection.add
                        (new FilePermission(docBase,"read"));
                    docBase = docBase + File.separator;
                } else {
                    permissionCollection.add
                        (new FilePermission
                            (docBase.substring(0,docBase.length() - 1),"read"));
                }
                docBase = docBase + "-";
                permissionCollection.add(new FilePermission(docBase,"read"));

                // Create a file read permission for web app tempdir (work)
                // directory
                String workDir = options.getScratchDir().toString();
                if (!workDir.endsWith(File.separator)){
                    permissionCollection.add
                        (new FilePermission(workDir,"read"));
                    workDir = workDir + File.separator;
                }
                workDir = workDir + "-";
                permissionCollection.add(new FilePermission(workDir,"read"));

                // Allow the JSP to access org.apache.sling.scripting.jsp.jasper.runtime.HttpJspBase
                permissionCollection.add( new RuntimePermission(
                    "accessClassInPackage.org.apache.jasper.runtime") );
            } catch (final Exception e) {
                context.log("Security Init for context failed",e);
            }
        }
    }

    /**
     * Returns and optionally creates a lock to load a tag file.
     */
    private Lock getTagFileLoadingLock(final String tagFilePath) {
        Lock lock = tagFileLoadingLocks.get(tagFilePath);
        if (lock == null) {
            lock = new ReentrantLock();
            final Lock existingLock = tagFileLoadingLocks.putIfAbsent(tagFilePath, lock);
            if (existingLock != null) {
                lock = existingLock;
            }
        }

        return lock;
    }

}
