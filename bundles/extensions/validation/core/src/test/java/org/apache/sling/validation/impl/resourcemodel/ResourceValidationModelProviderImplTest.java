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
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.jcr.MockQuery;
import org.apache.sling.testing.mock.jcr.MockQueryResult;
import org.apache.sling.testing.mock.jcr.MockQueryResultHandler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.validation.impl.Constants;
import org.apache.sling.validation.impl.model.ChildResourceImpl;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.apache.sling.validation.model.ChildResource;
import org.apache.sling.validation.model.ParameterizedValidator;
import org.apache.sling.validation.model.ResourceProperty;
import org.apache.sling.validation.model.ValidationModel;
import org.apache.sling.validation.spi.Validator;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
    private MockQueryResultHandler prefixBasedResultHandler;
    private Map<PrefixAndResourceType, List<Node>> validatorModelNodesPerPrefixAndResourceType;
    private Map<String, Validator<?>> validatorMap;
    private Map<String, Object> regexValdidatorParametrization;
    private ValidationModelBuilder modelBuilder;

    // extract resource type from strings like
    // "/jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and @validatedResourceType="<some-resource-type>"]"
    private static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile(".*@validatedResourceType=\"([^\"]*)\".*");

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK); // search capability necessary

    @Before
    public void setUp() throws LoginException, PersistenceException, RepositoryException {
        primaryTypeUnstructuredMap = new HashMap<String, Object>();
        primaryTypeUnstructuredMap.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        
        modelProvider = new ResourceValidationModelProviderImpl();
        validatorMap = new HashMap<String, Validator<?>>();
        validatorMap.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

        // one default model
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.setApplicablePath("/content/site1");
        ResourcePropertyBuilder propertyBuilder = new ResourcePropertyBuilder();
        propertyBuilder.validator(new RegexValidator(), RegexValidator.REGEX_PARAM, "prefix.*");
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
                // @validatedResourceType="<some-resource-type>"]
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
    public void testGetValidationModel() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ValidationModel model1 = modelBuilder.build("sling/validation/test");
        modelBuilder.setApplicablePath("/content/site2");
        ValidationModel model2 = modelBuilder.build("sling/validation/test");

        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2", model2);

        // check that both models are returned
        Collection<ValidationModel> models = modelProvider.getModel("sling/validation/test", validatorMap, rr);
        Assert.assertThat(models, Matchers.containsInAnyOrder(model1, model2));
    }

    @Test
    public void testGetValidationModelOutsideSearchPath() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ValidationModel model1 = modelBuilder.build("sling/validation/test");

        Resource contentValidatorsRoot = ResourceUtil.getOrCreateResource(rr, "/content",
                (Map<String, Object>) null, "sling:Folder", true);
        try {
            // build models in JCR outside any search path /apps or /libs
            createValidationModelResource(rr, contentValidatorsRoot.getPath(), "testValidationModel1", model1);

            // check that no model is found
            Collection<ValidationModel> models = modelProvider.getModel("sling/validation/test", validatorMap, rr);
            Assert.assertThat("Model was placed outside resource resolver search path but still found", models, Matchers.empty());
        } finally {
            rr.delete(contentValidatorsRoot);
        }
    }

    @Test
    public void testGetValidationModelWithChildren() throws Exception {
        // build two models manually (which are identical except for the applicable path)
        ResourcePropertyBuilder resourcePropertyBuilder = new ResourcePropertyBuilder();
        resourcePropertyBuilder.multiple();
        resourcePropertyBuilder.optional();
        ResourceProperty childproperty = resourcePropertyBuilder.build("child1property");
        modelBuilder.childResource(new ChildResourceImpl("child1", null, true,
                Collections.singletonList(childproperty), Collections.<ChildResource> emptyList()));
        ValidationModel model1 = modelBuilder.build("sling/validation/test");

        // build models in JCR
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);

        // compare both models
        Collection<ValidationModel> models = modelProvider.getModel("sling/validation/test", validatorMap, rr);
        Assert.assertThat(models, Matchers.contains(model1));
    }

    @Test
    public void testGetValidationModelWithOverlay() throws Exception {
        // create two models manually (which are identical except for the applicable path)
        ValidationModel model1 = modelBuilder.build("sling/validation/test");
        modelBuilder.setApplicablePath("/content/site2");
        ValidationModel model2 = modelBuilder.build("sling/validation/test");

        // create two models in the JCR: one in libs and one in apps (distinguishable via applicablePath)
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);
        createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1", model2);

        // only the apps model should be returned
        Collection<ValidationModel> models = modelProvider.getModel("sling/validation/test", validatorMap, rr);
        Assert.assertThat(models, Matchers.contains(model2));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetValidationModelWithInvalidValidator() throws Exception {
        // create one default model
        ValidationModel model1 = modelBuilder.build("sling/validation/test");
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);

        // clear validator map to make the referenced validator unknown
        validatorMap.clear();
        modelProvider.getModel("sling/validation/test", validatorMap, rr);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetValidationModelWithMissingChildrenAndProperties() throws Exception {
        // create a model with neither children nor properties
        modelBuilder = new ValidationModelBuilder();
        modelBuilder.addApplicablePath("content/site1");
        ValidationModel model1 = modelBuilder.build("sling/validation/test");
        
        createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1", model1);

        modelProvider.getModel("sling/validation/test", validatorMap, rr);
    }

    private Resource createValidationModelResource(ResourceResolver rr, String root, String name, ValidationModel model)
            throws Exception {
        Map<String, Object> modelProperties = new HashMap<String, Object>();
        modelProperties.put(Constants.VALIDATED_RESOURCE_TYPE, model.getValidatedResourceType());
        modelProperties.put(Constants.APPLICABLE_PATHS, model.getApplicablePaths());
        modelProperties
                .put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, Constants.VALIDATION_MODEL_RESOURCE_TYPE);
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
                    model.getValidatedResourceType());
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

    private void createValidationModelProperties(Resource model, @Nonnull Collection<ResourceProperty> properties)
            throws PersistenceException {
        ResourceResolver rr = model.getResourceResolver();
        if (properties.isEmpty()) {
            return;
        }
        Resource propertiesResource = ResourceUtil.getOrCreateResource(rr,
                model.getPath() + "/" + Constants.PROPERTIES, JcrConstants.NT_UNSTRUCTURED, null, true);
        for (ResourceProperty property : properties) {
            Map<String, Object> modelPropertyJCRProperties = new HashMap<String, Object>();
            modelPropertyJCRProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
            Resource propertyResource = ResourceUtil.getOrCreateResource(rr, propertiesResource.getPath() + "/"
                    + property.getName(), modelPropertyJCRProperties, null, true);
            if (propertyResource != null) {
                ModifiableValueMap values = propertyResource.adaptTo(ModifiableValueMap.class);
                Pattern pattern = property.getNamePattern();
                if (pattern != null) {
                    values.put(Constants.NAME_REGEX, pattern.pattern());
                }
                values.put(Constants.PROPERTY_MULTIPLE, property.isMultiple());
                values.put(Constants.OPTIONAL, !property.isRequired());
                Resource validators = ResourceUtil.getOrCreateResource(rr, propertyResource.getPath() + "/"
                        + Constants.VALIDATORS, JcrConstants.NT_UNSTRUCTURED, null, true);
                if (validators != null) {
                    for (ParameterizedValidator validator : property.getValidators()) {
                        Map<String, Object> validatorProperties = new HashMap<String, Object>();
                        validatorProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                        Map<String, Object> parameters = validator.getParameters();
                        if (!parameters.isEmpty()) {
                            // convert to right format
                            validatorProperties.put(Constants.VALIDATOR_ARGUMENTS,
                                    convertMapToJcrValidatorArguments(parameters));
                        }
                        ResourceUtil.getOrCreateResource(rr, validators.getPath() + "/"
                                + validator.getValidator().getClass().getName(), validatorProperties, null, true);
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
    private String[] convertMapToJcrValidatorArguments(Map<String, Object> map) {
        List<String> parametersForJcr = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            parametersForJcr.add(entry.getKey() + "=" + entry.getValue());
        }
        return parametersForJcr.toArray(new String[0]);
    }

    private Resource createValidationModelChildResource(Resource parentResource, ChildResource child) throws PersistenceException {
        ResourceResolver rr = parentResource.getResourceResolver();
        Resource modelChildren = rr.create(parentResource, Constants.CHILDREN, primaryTypeUnstructuredMap);
        Resource modelResource = rr.create(modelChildren, child.getName(), primaryTypeUnstructuredMap);
        ModifiableValueMap mvm = modelResource.adaptTo(ModifiableValueMap.class);
        if (child.getNamePattern() != null) {
            mvm.put(Constants.NAME_REGEX, child.getNamePattern() );
        }
        mvm.put(Constants.OPTIONAL, !child.isRequired());
        createValidationModelProperties(modelResource, child.getProperties());
        // recursion for all childs
        for (ChildResource grandChild : child.getChildren()) {
            createValidationModelChildResource(modelResource, grandChild);
        }
        return modelResource;
    }
}
