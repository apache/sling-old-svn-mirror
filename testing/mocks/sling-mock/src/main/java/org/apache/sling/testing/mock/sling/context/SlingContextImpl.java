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
package org.apache.sling.testing.mock.sling.context;

import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.resourcebuilder.api.ResourceBuilderFactory;
import org.apache.sling.resourcebuilder.impl.ResourceBuilderFactoryService;
import org.apache.sling.scripting.core.impl.BindingsValuesProvidersByContextImpl;
import org.apache.sling.scripting.core.impl.ScriptEngineManagerFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.loader.ContentLoader;
import org.apache.sling.testing.mock.sling.services.MockMimeTypeService;
import org.apache.sling.testing.mock.sling.services.MockSlingSettingService;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Defines Sling context objects with lazy initialization. Should not be used
 * directly but via the {@link org.apache.sling.testing.mock.sling.junit.SlingContext} JUnit
 * rule.
 */
@ConsumerType
public class SlingContextImpl extends OsgiContextImpl {

    // default to publish instance run mode
    static final Set<String> DEFAULT_RUN_MODES = ImmutableSet.<String> builder().add("publish").build();

    private static final String RESOURCERESOLVERFACTORYACTIVATOR_PID = "org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl";
    
    protected ResourceResolverFactory resourceResolverFactory;
    protected ResourceResolverType resourceResolverType;
    protected ResourceResolver resourceResolver;
    protected MockSlingHttpServletRequest request;
    protected MockSlingHttpServletResponse response;
    protected SlingScriptHelper slingScriptHelper;
    protected ContentLoader contentLoader;
    protected ContentLoader contentLoaderAutoCommit;
    protected ContentBuilder contentBuilder;
    protected ResourceBuilder resourceBuilder;
    protected UniqueRoot uniqueRoot;
    
    private Map<String, Object> resourceResolverFactoryActivatorProps;

    /**
     * @param resourceResolverType Resource resolver type
     */
    protected void setResourceResolverType(final ResourceResolverType resourceResolverType) {
        this.resourceResolverType = resourceResolverType;
    }

    protected void setResourceResolverFactoryActivatorProps(Map<String, Object> props) {
        this.resourceResolverFactoryActivatorProps = props;
    }
    
    /**
     * Setup actions before test method execution
     */
    protected void setUp() {
        super.setUp();
        MockSling.setAdapterManagerBundleContext(bundleContext());
        
        if (this.resourceResolverFactoryActivatorProps != null) {
            // use OSGi ConfigurationAdmin to pass over customized configuration to Resource Resolver Factory Activator service
            MockOsgi.setConfigForPid(bundleContext(), RESOURCERESOLVERFACTORYACTIVATOR_PID, this.resourceResolverFactoryActivatorProps);
        }
        
        // automatically register resource resolver factory when ResourceResolverType != NONE,
        // so the ResourceResolverFactory is available as OSGi service immediately
        if (resourceResolverType != ResourceResolverType.NONE) {
            resourceResolverFactory();
        }
        
        registerDefaultServices();
    }
    
    /**
     * Initialize mocked resource resolver factory.
     * @return Resource resolver factory
     */
    protected ResourceResolverFactory newResourceResolverFactory() {
        return ContextResourceResolverFactory.get(this.resourceResolverType, bundleContext());
    }
    
    private ResourceResolverFactory resourceResolverFactory() {
        if (this.resourceResolverFactory == null) {
            this.resourceResolverFactory = newResourceResolverFactory();
        }
        return this.resourceResolverFactory;
    }

    /**
     * Default services that should be available for every unit test
     */
    protected void registerDefaultServices() {

        // scripting services (required by sling models impl since 1.3.6)
        registerInjectActivateService(new ScriptEngineManagerFactory());
        registerInjectActivateService(new BindingsValuesProvidersByContextImpl());
        
        // sling models
        registerInjectActivateService(new ModelAdapterFactory());
        registerInjectActivateServiceByClassName(
                "org.apache.sling.models.impl.FirstImplementationPicker",
                "org.apache.sling.models.impl.ResourceTypeBasedResourcePicker",
                "org.apache.sling.models.impl.injectors.BindingsInjector",
                "org.apache.sling.models.impl.injectors.ChildResourceInjector",
                "org.apache.sling.models.impl.injectors.OSGiServiceInjector",
                "org.apache.sling.models.impl.injectors.RequestAttributeInjector",
                "org.apache.sling.models.impl.injectors.ResourcePathInjector",
                "org.apache.sling.models.impl.injectors.SelfInjector",
                "org.apache.sling.models.impl.injectors.SlingObjectInjector",
                "org.apache.sling.models.impl.injectors.ValueMapInjector",
                "org.apache.sling.models.impl.via.BeanPropertyViaProvider",
                "org.apache.sling.models.impl.via.ChildResourceViaProvider",
                "org.apache.sling.models.impl.via.ForcedResourceTypeViaProvider",
                "org.apache.sling.models.impl.via.ResourceSuperTypeViaProvider");

        // other services
        registerService(SlingSettingsService.class, new MockSlingSettingService(DEFAULT_RUN_MODES));
        registerService(MimeTypeService.class, new MockMimeTypeService());
        registerInjectActivateService(new ResourceBuilderFactoryService());
        
        // scan for models defined via bundle headers in classpath
        ModelAdapterFactoryUtil.addModelsForManifestEntries(this.bundleContext());
    }
    
    private void registerInjectActivateServiceByClassName(String... classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                registerInjectActivateService(clazz.newInstance());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                // ignore - probably not the latest sling models impl version
            }
        }
    }

    /**
     * Teardown actions after test method execution
     */
    protected void tearDown() {
        
        if (this.resourceResolver != null) {
            
            // revert potential unsaved changes in resource resolver/JCR session
            this.resourceResolver.revert();
            Session session = this.resourceResolver.adaptTo(Session.class);
            if (session != null) {
                try {
                    session.refresh(false);
                } catch (RepositoryException ex) {
                    // ignore
                }
            }
            
            // remove unique roots
            if (this.uniqueRoot != null) {
                this.uniqueRoot.cleanUp();
            }
            
            // close resource resolver
            this.resourceResolver.close();
        }

        MockSling.clearAdapterManagerBundleContext();
        
        this.resourceResolver = null;
        this.request = null;
        this.response = null;
        this.slingScriptHelper = null;
        this.contentLoader = null;
        this.contentLoaderAutoCommit = null;
        this.contentBuilder = null;
        this.resourceBuilder = null;
        this.uniqueRoot = null;
        this.resourceResolverFactory = null;
        
        super.tearDown();
    }

    /**
     * @return Resource resolver type
     */
    public final ResourceResolverType resourceResolverType() {
        return this.resourceResolverType;
    }
    
    /**
     * Returns the singleton resource resolver bound to this context.
     * It is automatically closed after the test.
     * @return Resource resolver
     */
    public final ResourceResolver resourceResolver() {
        if (this.resourceResolver == null) {
            try {
                this.resourceResolver = this.resourceResolverFactory().getAdministrativeResourceResolver(null);
            } catch (LoginException ex) {
                throw new RuntimeException("Creating resource resolver failed.", ex);
            }
        }
        return this.resourceResolver;
    }
    
    /**
     * @return Sling request
     */
    public final MockSlingHttpServletRequest request() {
        if (this.request == null) {
            this.request = new MockSlingHttpServletRequest(this.resourceResolver(), this.bundleContext());

            // initialize sling bindings
            SlingBindings bindings = new SlingBindings();
            bindings.put(SlingBindings.REQUEST, this.request);
            bindings.put(SlingBindings.RESPONSE, response());
            bindings.put(SlingBindings.SLING, slingScriptHelper());
            this.request.setAttribute(SlingBindings.class.getName(), bindings);
        }
        return this.request;
    }

    /**
     * @return Request path info
     */
    public final MockRequestPathInfo requestPathInfo() {
        return (MockRequestPathInfo) request().getRequestPathInfo();
    }

    /**
     * @return Sling response
     */
    public final MockSlingHttpServletResponse response() {
        if (this.response == null) {
            this.response = new MockSlingHttpServletResponse();
        }
        return this.response;
    }

    /**
     * @return Sling script helper
     */
    public final SlingScriptHelper slingScriptHelper() {
        if (this.slingScriptHelper == null) {
            this.slingScriptHelper = MockSling.newSlingScriptHelper(this.request(), this.response(),
                    this.bundleContext());
        }
        return this.slingScriptHelper;
    }

    /**
     * @return Content loader
     */
    public ContentLoader load() {
        return load(true);
    }

    /**
     * @param autoCommit Automatically commit changes after loading content (default: true)
     * @return Content loader
     */
    public ContentLoader load(boolean autoCommit) {
        if (autoCommit) {
            if (this.contentLoaderAutoCommit == null) {
                this.contentLoaderAutoCommit = new ContentLoader(resourceResolver(), bundleContext(), true);
            }
            return this.contentLoaderAutoCommit;
        }
        else {
            if (this.contentLoader == null) {
                this.contentLoader = new ContentLoader(resourceResolver(), bundleContext(), false);
            }
            return this.contentLoader;
        }
    }

    /**
     * Creates a {@link ContentBuilder} object for easily creating test content.
     * This API was part of Sling Mocks since version 1.x.
     * You can use alternatively the {@link #build()} method and use the {@link ResourceBuilder} API.
     * @return Content builder for building test content
     */
    public ContentBuilder create() {
        if (this.contentBuilder == null) {
            this.contentBuilder = new ContentBuilder(resourceResolver());
        }
        return this.contentBuilder;
    }
    
    /**
     * Creates a {@link ResourceBuilder} object for easily creating test content.
     * This is a separate API which can be used inside sling mocks or in a running instance.
     * You can use alternatively the {@link #create()} method to use the {@link ContentBuilder} API.
     * @return Resource builder for building test content.
     */
    public ResourceBuilder build() {
        if (this.resourceBuilder == null) {
            this.resourceBuilder = getService(ResourceBuilderFactory.class).forResolver(this.resourceResolver());
        }
        return this.resourceBuilder;
    }

    /**
     * @return Current resource
     */
    public final Resource currentResource() {
        return request().getResource();
    }

    /**
     * Set current resource in request.
     * @param resourcePath Resource path
     * @return Current resource
     */
    public final Resource currentResource(String resourcePath) {
        if (resourcePath != null) {
            Resource resource = resourceResolver().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalArgumentException("Resource does not exist: " + resourcePath);
            }
            return currentResource(resource);
        } else {
            return currentResource((Resource) null);
        }
    }

    /**
     * Set current resource in request.
     * @param resource Resource
     * @return Current resource
     */
    public final Resource currentResource(Resource resource) {
        request().setResource(resource);
        return resource;
    }

    /**
     * Search classpath for given java package names (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageName Java package name
     */
    public final void addModelsForPackage(String packageName) {
        ModelAdapterFactoryUtil.addModelsForPackages(bundleContext(),  packageName);
    }

    /**
     * Search classpath for given java package names (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageNames Java package names
     */
    public final void addModelsForPackage(String... packageNames) {
        ModelAdapterFactoryUtil.addModelsForPackages(bundleContext(), packageNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param classNames Java class names
     */
    public final void addModelsForClasses(String... classNames) {
        ModelAdapterFactoryUtil.addModelsForClasses(bundleContext(), classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Model annotation.
     * @param classes Java classes
     */
    public final void addModelsForClasses(Class... classes) {
        ModelAdapterFactoryUtil.addModelsForClasses(bundleContext(), classes);
    }

    /**
     * Set current run mode(s).
     * @param runModes Run mode(s).
     */
    public final void runMode(String... runModes) {
        Set<String> newRunModes = ImmutableSet.<String> builder().add(runModes).build();
        ServiceReference<SlingSettingsService> ref = bundleContext().getServiceReference(SlingSettingsService.class);
        if (ref != null) {
            MockSlingSettingService slingSettings = (MockSlingSettingService)bundleContext().getService(ref);
            slingSettings.setRunModes(newRunModes);
        }
    }
    
    /**
     * Create unique root paths for unit tests (and clean them up after the test run automatically).
     * @return Unique root path helper
     */
    public UniqueRoot uniqueRoot() {
        if (uniqueRoot == null) {
            uniqueRoot = new UniqueRoot(this);
        }
        return uniqueRoot;
    }
    
    /**
     * Create a Sling AdapterFactory on the fly which can adapt from <code>adaptableClass</code>
     * to <code>adapterClass</code> and just returns the given value as result.
     * @param adaptableClass Class to adapt from
     * @param adapterClass Class to adapt to
     * @param adapter Object which is always returned for this adaption.
     * @param <T1> Adaptable type
     * @param <T2> Adapter type
     */
    public final <T1, T2> void registerAdapter(final Class<T1> adaptableClass, final Class<T2> adapterClass,
            final T2 adapter) {
        registerAdapter(adaptableClass, adapterClass, new Function<T1, T2>() {
            @Override
            public T2 apply(T1 input) {
                return adapter;
            }
        });
    }

    /**
     * Create a Sling AdapterFactory on the fly which can adapt from <code>adaptableClass</code>
     * to <code>adapterClass</code> and delegates the adapter mapping to the given <code>adaptHandler</code> function.
     * @param adaptableClass Class to adapt from
     * @param adapterClass Class to adapt to
     * @param adaptHandler Function to handle the adaption
     * @param <T1> Adaptable type
     * @param <T2> Adapter type
     */
    public final <T1, T2> void registerAdapter(final Class<T1> adaptableClass, final Class<T2> adapterClass,
            final Function<T1,T2> adaptHandler) {
        AdapterFactory adapterFactory = new AdapterFactory() {
            @SuppressWarnings("unchecked")
            @Override
            public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
                return (AdapterType)adaptHandler.apply((T1)adaptable);
            }
        };
        registerService(AdapterFactory.class, adapterFactory, ImmutableMap.<String, Object>builder()
                .put(AdapterFactory.ADAPTABLE_CLASSES, new String[] {
                    adaptableClass.getName()
                })
                .put(AdapterFactory.ADAPTER_CLASSES, new String[] {
                    adapterClass.getName()
                })
                // make sure this overlay has higher ranking than other adapter factories
                .put(Constants.SERVICE_RANKING, Integer.MAX_VALUE)
                .build());
    }

}
