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
package org.apache.sling.validation.impl.resourcemodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.jcr.MockQuery;
import org.apache.sling.testing.mock.jcr.MockQueryResult;
import org.apache.sling.testing.mock.jcr.MockQueryResultHandler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ValidatorInvocation;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceValidationModelProviderImplTest {

    private final static class PrefixAndResourceType {
        private final String prefix;
        private final String resourceType;

        public PrefixAndResourceType(String prefix, String resourceType) {
            super();
            this.prefix = prefix;
            this.resourceType = resourceType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
            result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PrefixAndResourceType other = (PrefixAndResourceType) obj;
            if (prefix == null) {
                if (other.prefix != null)
                    return false;
            } else if (!prefix.equals(other.prefix))
                return false;
            if (resourceType == null) {
                if (other.resourceType != null)
                    return false;
            } else if (!resourceType.equals(other.resourceType))
                return false;
            return true;
        }

    }

    /**
     * Assume the validation models are stored under (/libs|/apps) + / + VALIDATION_MODELS_RELATIVE_PATH.
     */
    private static final String VALIDATION_MODELS_RELATIVE_PATH = "sling/validation/models";
    private static final String APPS = "/apps";
    private static final String LIBS = "/libs";
    private Resource appsValidatorsRoot;
    private Resource libsValidatorsRoot;
    private static Map<String, Object> primaryTypeUnstructuredMap;
    private ResourceValidationModelProviderImpl modelProvider;
    private ResourceResolver rr;
    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    private MockQueryResultHandler prefixBasedResultHandler;
    private Map<PrefixAndResourceType, List<Node>> validatorModelNodesPerPrefixAndResourceType;
    private ValidationModelBuilder modelBuilder;

    // extract resource type from strings like
    // "/jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and @validatedResourceType="<some-resource-type>"]"
    private static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile(".*@validatingResourceType=\"([^\"]*)\".*");

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK); // search capability necessary

    @Before
    public void setUp() throws LoginException, PersistenceException, RepositoryException {
        primaryTypeUnstructuredMap = new HashMap<String, Object>();
        primaryTypeUnstructuredMap.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        
        modelProvider = new ResourceValidationModelProviderImpl();

        // one default model
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.setApplicablePath("/content/site1");
        ResourcePropertyBuilder propertyBuilder = new ResourcePropertyBuilder();
        propertyBuilder.validator("validatorId", 10, RegexValidator.REGEX_PARAM, "prefix.*");
        ResourceProperty property = propertyBuilder.build("field1");
        modelBuilder.resourceProperty(property);

        prefixBasedResultHandler = new MockQueryResultHandler() {
            @Override
            public MockQueryResult executeQuery(MockQuery query) {
                if (!"xpath".equals(query.getLanguage())) {
                    return null;
                }
                String statement = query.getStatement();
                // query looks like /jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and
                // @validatingResourceType="<some-resource-type>"]
                if (statement.startsWith("/jcr:root/")) {
                    statement = statement.substring("/jcr:root/".length() - 1);
                }
                // extract the prefix from the statement
                String prefix = Text.getAbsoluteParent(statement, 0);

                // extract the resource type from the statement
                Matcher matcher = RESOURCE_TYPE_PATTERN.matcher(statement);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException(
                            "Can only process query statements which contain a validatedResourceType but the statement is: "
                                    + statement);
                }
                String resourceType = matcher.group(1);

                PrefixAndResourceType prefixAndResourceType = new PrefixAndResourceType(prefix, resourceType);
                if (validatorModelNodesPerPrefixAndResourceType.keySet().contains(prefixAndResourceType)) {
                    return new MockQueryResult(validatorModelNodesPerPrefixAndResourceType.get(prefixAndResourceType));
                }
                return null;
            }
        };
        rr = context.resourceResolver();
        modelProvider.rrf = resourceResolverFactory;
        // create a wrapper resource resolver, which cannot be closed (as the SlingContext will take care of that)
        ResourceResolver nonClosableResourceResolverWrapper = Mockito.spy(rr);
        // intercept all close calls
        Mockito.doNothing().when(nonClosableResourceResolverWrapper).close();
        // always use the context's resource resolver (because we never commit)
        Mockito.when(resourceResolverFactory.getServiceResourceResolver(Mockito.anyObject())).thenReturn(nonClosableResourceResolverWrapper);
        MockJcr.addQueryResultHandler(rr.adaptTo(Session.class), prefixBasedResultHandler);

        validatorModelNodesPerPrefixAndResourceType = new HashMap<PrefixAndResourceType, List<Node>>();

        appsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, APPS + "/" + VALIDATION_MODELS_RELATIVE_PATH,
                (Map<String, Object>) null, "sling:Folder", true);
        libsValidatorsRoot = ResourceUtil.getOrCreateResource(rr, LIBS + "/" + VALIDATION_MODELS_RELATIVE_PATH,
                (Map<String, Object>) null, "sling:Folder", true);
    }

    @After
    public void tearDown() throws PersistenceException {
        if (appsValidatorsRoot != null) {
            rr.delete(appsValidatorsRoot);
        }
        if (libsValidatorsRoot != null) {
            rr.delete(libsValidatorsRoot);
        }
        rr.commit();
    }

    @Test
    public void testGetValidationModels() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        ValidationModel model1 = modelBuilder.resourceProperty(resourcePropertyBuilder.build("property1")).build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        modelBuilder.setApplicablePath("/content/site2");
        ValidationModel model2 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel2");

        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", model2);

        // check that both models are returned
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.containsInAnyOrder(model1, model2));
    }

    @Test
    public void testGetValidationModelsWithoutApplicablePath() throws Exception {
        modelBuilder = new ValidationModelBuilder();
        // build two models manually (which are identical except for the applicable path)
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        ValidationModel model1 = modelBuilder.resourceProperty(resourcePropertyBuilder.build("property1")).build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        
        // build models in JCR
        Resource modelResource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        ModifiableValueMap properties = modelResource.adaptTo(ModifiableValueMap.class);
        properties.remove("applicablePaths");

        // check that both models are returned
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.containsInAnyOrder(model1));
    }
    
    @Test
    public void testGetValidationModelsWithEmptyApplicablePath() throws Exception {
        modelBuilder = new ValidationModelBuilder();
        // build two models manually (which are identical except for the applicable path)
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        ValidationModel model1 = modelBuilder.resourceProperty(resourcePropertyBuilder.build("property1")).build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        
        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        
        // check that both models are returned
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.containsInAnyOrder(model1));
    }
    
    @Test
    public void testGetValidationModelsOutsideSearchPath() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ValidationModel model1 = modelBuilder.build("sling/validation/test", "some source");

        Resource contentValidatorsRoot = ResourceUtil.getOrCreateResource(rr, "/content",
                (Map<String, Object>) null, "sling:Folder", true);
        try {
            // build models in JCR outside any search path /apps or /libs
            createValidationModelResource(rr, contentValidatorsRoot.getPath(), "testValidationModel1", model1);

            // check that no model is found
            Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
            Assert.assertThat("Model was placed outside resource resolver search path but still found", models, Matchers.empty());
        } finally {
            rr.delete(contentValidatorsRoot);
        }
    }

    @Test
    public void testGetValidationModelsWithChildren() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        resourcePropertyBuilder.multiple();
        resourcePropertyBuilder.optional();
        ResourceProperty childproperty = resourcePropertyBuilder.build("child1property");
        modelBuilder.childResource(new ChildResourceImpl("child1", null, true,
                Collections.singletonList(childproperty), Collections.<ChildResource> emptyList()));
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");

        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);

        // compare both models
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test
    public void testGetValidationModelsWithOverlay() throws Exception {
        // create two models manually (which are identical except for the applicable path)
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        modelBuilder.setApplicablePath("/content/site2");
        ValidationModel model2 = modelBuilder.build("sling/validation/test", appsValidatorsRoot.getPath() + "/testValidationModel1");

        // create two models: one in libs and one in apps (distinguishable via applicablePath)
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", model2);

        // only the apps model should be returned
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model2));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetValidationModelsWithMissingChildrenAndProperties() throws Exception {
        // create a model with properties (otherwise build() will already throw an exception)
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(new ResourcePropertyBuilder().build("field1"));
        modelBuilder.addApplicablePath("content/site1");
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        
        Resource resource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        // make created model invalid by removing the properties sub resource
        rr.delete(resource.getChild("properties"));
        modelProvider.getValidationModels("sling/validation/test");
    }

    @Test
    public void testGetValidationModelsWithComplexValidatorArguments() throws Exception {
        // create a model with neither children nor properties
        Map<String, Object> validatorArguments = new HashMap<>();
        validatorArguments.put("key1", "value1");
        validatorArguments.put("key1", "value2");
        validatorArguments.put("key1", "value3");
        validatorArguments.put("key2", "value1");
        validatorArguments.put("key3", "value1=value2");
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.resourceProperty(new ResourcePropertyBuilder().validator("validatorId", 10, validatorArguments).build("field1"));
        modelBuilder.addApplicablePath("content/site1");
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test(expected=IllegalStateException.class)
    public void testGetValidationModelsWithInvalidValidatorArguments1() throws Exception {
        // create a model with neither children nor properties
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        // create valid model first
        Resource modelResource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        // and make parametrization of validator invalid afterwards
        Resource validatorResource = modelResource.getChild("properties/field1/validators/validatorId");
        ModifiableValueMap validatorArguments = validatorResource.adaptTo(ModifiableValueMap.class);
        validatorArguments.put("validatorArguments", "key1"); // value without "="
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test(expected=IllegalStateException.class)
    public void testGetValidationModelsWithInvalidValidatorArguments2() throws Exception {
        // create a model with neither children nor properties
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        // create valid model first
        Resource modelResource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        // and make parametrization of validator invalid afterwards
        Resource validatorResource = modelResource.getChild("properties/field1/validators/validatorId");
        ModifiableValueMap validatorArguments = validatorResource.adaptTo(ModifiableValueMap.class);
        validatorArguments.put("validatorArguments", "=value2"); // starting with "="
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test(expected=IllegalStateException.class)
    public void testGetValidationModelsWithInvalidValidatorArguments3() throws Exception {
        // create a model with neither children nor properties
        ValidationModel model1 = modelBuilder.build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        // create valid model first
        Resource modelResource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        // and make parametrization of validator invalid afterwards
        Resource validatorResource = modelResource.getChild("properties/field1/validators/validatorId");
        ModifiableValueMap validatorArguments = validatorResource.adaptTo(ModifiableValueMap.class);
        validatorArguments.put("validatorArguments", "key1="); // ending with "="
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test
    public void testCachingOfGetValidationModels() throws Exception {
        // build one model
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        ValidationModel model1 = modelBuilder.resourceProperty(resourcePropertyBuilder.build("property1")).build("sling/validation/test", libsValidatorsRoot.getPath() + "/testValidationModel1");
        modelBuilder.setApplicablePath("/content/site2");

        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);

        // check that both models are returned
        Collection<ValidationModel> models = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertThat(models, Matchers.containsInAnyOrder(model1));
        
        // the 2nd time the same instance should be returned
        Collection<ValidationModel> models2 = modelProvider.getValidationModels("sling/validation/test");
        Assert.assertEquals("Due to caching both models should be actually the same instance", System.identityHashCode(models), System.identityHashCode(models2));
    }

    /*--- the following methods create validation model resources from ValidationModel objects --*/

    private Resource createValidationModelResource(ResourceResolver rr, String root, String name, ValidationModel model)
            throws Exception {
        Map<String, Object> modelProperties = new HashMap<String, Object>();
        modelProperties.put(ResourceValidationModelProviderImpl.VALIDATING_RESOURCE_TYPE, model.getValidatingResourceType());
        modelProperties.put(ResourceValidationModelProviderImpl.APPLICABLE_PATHS, model.getApplicablePaths().toArray());
        modelProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, ResourceValidationModelProviderImpl.VALIDATION_MODEL_RESOURCE_TYPE);
        modelProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        Resource modelResource = ResourceUtil.getOrCreateResource(rr, root + "/" + name, modelProperties,
                JcrResourceConstants.NT_SLING_FOLDER, true);
        if (model != null) {
            createValidationModelProperties(modelResource, model.getResourceProperties());
            for (ChildResource child : model.getChildren()) {
                createValidationModelChildResource(modelResource, child);
            }
            // add to search handler (with root path)
            String prefix = Text.getAbsoluteParent(root, 0);
            PrefixAndResourceType prefixAndResourceType = new PrefixAndResourceType(prefix,
                    model.getValidatingResourceType());
            List<Node> nodes;
            nodes = validatorModelNodesPerPrefixAndResourceType.get(prefixAndResourceType);
            if (nodes == null) {
                nodes = new ArrayList<Node>();
                validatorModelNodesPerPrefixAndResourceType.put(prefixAndResourceType, nodes);
            }
            nodes.add(modelResource.adaptTo(Node.class));
        }
        return modelResource;
    }

    /**
     * Always uses the validator's class name as validator resource name.
     * @param model
     * @param properties
     * @throws PersistenceException
     */
    private void createValidationModelProperties(Resource model, @Nonnull Collection<ResourceProperty> properties)
            throws PersistenceException {
        ResourceResolver rr = model.getResourceResolver();
        if (properties.isEmpty()) {
            return;
        }
        Resource propertiesResource = ResourceUtil.getOrCreateResource(rr,
                model.getPath() + "/" + ResourceValidationModelProviderImpl.PROPERTIES, JcrConstants.NT_UNSTRUCTURED, null, true);
        for (ResourceProperty property : properties) {
            Map<String, Object> modelPropertyJCRProperties = new HashMap<String, Object>();
            modelPropertyJCRProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
            Resource propertyResource = ResourceUtil.getOrCreateResource(rr, propertiesResource.getPath() + "/"
                    + property.getName(), modelPropertyJCRProperties, null, true);
            if (propertyResource != null) {
                ModifiableValueMap values = propertyResource.adaptTo(ModifiableValueMap.class);
                Pattern pattern = property.getNamePattern();
                if (pattern != null) {
                    values.put(ResourceValidationModelProviderImpl.NAME_REGEX, pattern.pattern());
                }
                values.put(ResourceValidationModelProviderImpl.PROPERTY_MULTIPLE, property.isMultiple());
                values.put(ResourceValidationModelProviderImpl.OPTIONAL, !property.isRequired());
                Resource validators = ResourceUtil.getOrCreateResource(rr, propertyResource.getPath() + "/"
                        + ResourceValidationModelProviderImpl.VALIDATORS, JcrConstants.NT_UNSTRUCTURED, null, true);
                if (validators != null) {
                    for (ValidatorInvocation validatorIncovation : property.getValidatorInvocations()) {
                        Map<String, Object> validatorProperties = new HashMap<String, Object>();
                        validatorProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                        ValueMap parameters = validatorIncovation.getParameters();
                        if (!parameters.isEmpty()) {
                            // convert to right format
                            validatorProperties.put(ResourceValidationModelProviderImpl.VALIDATOR_ARGUMENTS,
                                    convertMapToJcrValidatorArguments(parameters));
                        }
                        Integer severity = validatorIncovation.getSeverity();
                        if (severity != null) {
                            validatorProperties.put(ResourceValidationModelProviderImpl.SEVERITY, severity);
                        }
                        ResourceUtil.getOrCreateResource(rr, validators.getPath() + "/"
                                + validatorIncovation.getValidatorId(), validatorProperties, null, true);
                    }
                }
            }
        }
    }

    /**
     * Convert to right format : String array of "<key>=<value>"
     * 
     * @param map
     * @return
     */
    private String[] convertMapToJcrValidatorArguments(ValueMap map) {
        List<String> parametersForJcr = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                for (String value : (String[]) entry.getValue()) {
                    parametersForJcr.add(value + "=" + entry.getValue());
                }
            } else {
                parametersForJcr.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return parametersForJcr.toArray(new String[0]);
    }

    private Resource createValidationModelChildResource(Resource parentResource, ChildResource child) throws PersistenceException {
        ResourceResolver rr = parentResource.getResourceResolver();
        Resource modelChildren = rr.create(parentResource, ResourceValidationModelProviderImpl.CHILDREN, primaryTypeUnstructuredMap);
        Resource modelResource = rr.create(modelChildren, child.getName(), primaryTypeUnstructuredMap);
        ModifiableValueMap mvm = modelResource.adaptTo(ModifiableValueMap.class);
        if (child.getNamePattern() != null) {
            mvm.put(ResourceValidationModelProviderImpl.NAME_REGEX, child.getNamePattern() );
        }
        mvm.put(ResourceValidationModelProviderImpl.OPTIONAL, !child.isRequired());
        createValidationModelProperties(modelResource, child.getProperties());
        // recursion for all childs
        for (ChildResource grandChild : child.getChildren()) {
            createValidationModelChildResource(modelResource, grandChild);
        }
        return modelResource;
    }
}
