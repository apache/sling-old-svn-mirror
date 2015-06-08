package org.apache.sling.validation.impl.resourcemodel;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.sling.validation.api.ParameterizedValidator;
import org.apache.sling.validation.api.ResourceProperty;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.Constants;
import org.apache.sling.validation.impl.model.ResourcePropertyImpl;
import org.apache.sling.validation.impl.util.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.util.ValidationModelBuilder;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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

        public String getPrefix() {
            return prefix;
        }

        public String getResourceType() {
            return resourceType;
        }
    }

    /**
     * Assume the validation models are stored under (/libs|/apps) + / + VALIDATION_MODELS_RELATIVE_PATH.
     */
    private static final String VALIDATION_MODELS_RELATIVE_PATH = "sling/validation/models";
    private static final String APPS = "/apps";
    private static final String LIBS = "/libs";
    private static Resource appsValidatorsRoot;
    private static Resource libsValidatorsRoot;
    private static Map<String, Object> primaryTypeUnstructuredMap;
    private ResourceValidationModelProviderImpl modelProvider;
    private ResourceResolver rr;
    private MockQueryResultHandler prefixBasedResultHandler;
    private Map<PrefixAndResourceType, List<Node>> validatorModelNodesPerPrefixAndResourceType;
    private Map<String, Validator<?>> validatorMap;

    // extract resource type from strings like
    // "/jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and @validatedResourceType="<some-resource-type>"]"
    private static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile(".*@validatedResourceType=\"([^\"]*)\".*");

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK); // search capability necessary

    @BeforeClass
    public static void init() throws Exception {
        primaryTypeUnstructuredMap = new HashMap<String, Object>();
        primaryTypeUnstructuredMap.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
    }

    @Before
    public void setUp() throws LoginException, PersistenceException, RepositoryException {
        modelProvider = new ResourceValidationModelProviderImpl();
        validatorMap = new HashMap<String, Validator<?>>();
        validatorMap.put("org.apache.sling.validation.impl.validators.RegexValidator", new RegexValidator());

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
 
        // convert to test property?
        Resource modelResource = null;
        try {
            
            // compare models
            ValidationModelBuilder modelBuilder =  new ValidationModelBuilder();
            modelBuilder.applicablePath("/apps/validation");
            ResourcePropertyBuilder propertyBuilder = new ResourcePropertyBuilder();
            // TODO: test parametrisation
            propertyBuilder.validator(new RegexValidator());
            ResourceProperty property = propertyBuilder.build("field1");
            modelBuilder.resourceProperty(property);
            
            modelResource = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1",
                    "sling/validation/test", new String[] { "/apps/validation" }, property);

            ValidationModel model = modelBuilder.build(libsValidatorsRoot.getPath()+"/testValidationModel1", "sling/validation/test");
            Collection<ValidationModel> models = modelProvider.getModel(rr, "sling/validation/test", validatorMap);
            
            Assert.assertThat(models, Matchers.contains(model));
        } finally {
            if (modelResource != null) {
                rr.delete(modelResource);
            }
        }
    }
/
    @Test
    public void testGetValidationModelWithOverlay() throws Exception {
      

        TestProperty field = new TestProperty("field1");
        field.addValidator("org.apache.sling.validation.impl.validators.RegexValidator");
        Resource model1 = null, model2 = null, model3 = null;
        try {
            model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1",
                    "sling/validation/test", new String[] { "/apps/validation/1" }, field);
            model2 = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1",
                    "sling/validation/test", new String[] { "/apps/validation/1", "/apps/validation/2" }, field);
            model3 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2",
                    "sling/validation/test", new String[] { "/apps/validation/3" }, field);

            // BEST MATCHING PATH = /apps/validation/1; assume the applicable paths contain /apps/validation/2
            ValidationModel vm = validationService.getValidationModel("sling/validation/test",
                    "/apps/validation/1/resource");
            Assert.assertNotNull("Could not find validation model for 'sling/validation/test'", vm);
            assertThat(vm.getApplicablePaths(), Matchers.hasItemInArray("/apps/validation/2"));

            vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/3/resource");
            Assert.assertNotNull("Could not find validation model for 'sling/validation/test'", vm);
            assertThat(vm.getApplicablePaths(), Matchers.hasItemInArray("/apps/validation/3"));

        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (model2 != null) {
                rr.delete(model2);
            }
            if (model3 != null) {
                rr.delete(model3);
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetValidationModelWithInvalidValidator() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator",
                new RegexValidator());

        TestProperty field = new TestProperty("field1");
        // invalid validator name
        field.addValidator("org.apache.sling.validation.impl.validators1.RegexValidator");
        Resource model = null;
        try {
            model = createValidationModelResource(rr, appsValidatorsRoot.getPath(), "testValidationModel1",
                    "sling/validation/test", new String[] { "/apps/validation/1", "/apps/validation/2" }, field);
            validationService.getValidationModel("sling/validation/test", "/apps/validation/1/resource");
        } finally {
            if (model != null) {
                rr.delete(model);
            }
        }
    }*/

    private Resource createValidationModelResource(ResourceResolver rr, String root, String name,
            String validatedResourceType, String[] applicableResourcePaths, ResourceProperty... properties)
            throws Exception {
        Map<String, Object> modelProperties = new HashMap<String, Object>();
        modelProperties.put(Constants.VALIDATED_RESOURCE_TYPE, validatedResourceType);
        modelProperties.put(Constants.APPLICABLE_PATHS, applicableResourcePaths);
        modelProperties
                .put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, Constants.VALIDATION_MODEL_RESOURCE_TYPE);
        modelProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        Resource model = ResourceUtil.getOrCreateResource(rr, root + "/" + name, modelProperties,
                JcrResourceConstants.NT_SLING_FOLDER, true);
        if (model != null) {
            createValidationModelProperties(model, properties);

            // add to search handler (with root path)
            String prefix = Text.getAbsoluteParent(root, 0);
            PrefixAndResourceType prefixAndResourceType = new PrefixAndResourceType(prefix, validatedResourceType);
            List<Node> nodes;
            nodes = validatorModelNodesPerPrefixAndResourceType.get(prefixAndResourceType);
            if (nodes == null) {
                nodes = new ArrayList<Node>();
                validatorModelNodesPerPrefixAndResourceType.put(prefixAndResourceType, nodes);
            }
            nodes.add(model.adaptTo(Node.class));
        }
        return model;
    }

    private void createValidationModelProperties(Resource model, ResourceProperty... properties)
            throws PersistenceException {
        ResourceResolver rr = model.getResourceResolver();
        if (properties.length == 0) {
            return;
        }
        Resource propertiesResource = ResourceUtil.getOrCreateResource(rr,
                model.getPath() + "/" + Constants.PROPERTIES, JcrConstants.NT_UNSTRUCTURED, null, true);
        if (propertiesResource != null) {
            for (ResourceProperty property : properties) {
                Map<String, Object> modelPropertyJCRProperties = new HashMap<String, Object>();
                modelPropertyJCRProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                Resource propertyResource = ResourceUtil.getOrCreateResource(rr, propertiesResource.getPath() + "/"
                        + property.getName(), modelPropertyJCRProperties, null, true);
                if (propertyResource != null) {
                    ModifiableValueMap values = propertyResource.adaptTo(ModifiableValueMap.class);
                    if (property.getNamePattern() != null) {
                        values.put(Constants.NAME_REGEX, property.getNamePattern().pattern());
                    }
                    values.put(Constants.PROPERTY_MULTIPLE, property.isMultiple());
                    values.put(Constants.OPTIONAL, !property.isRequired());
                    Resource validators = ResourceUtil.getOrCreateResource(rr, propertyResource.getPath() + "/"
                            + Constants.VALIDATORS, JcrConstants.NT_UNSTRUCTURED, null, true);
                    if (validators != null) {
                        for (ParameterizedValidator validator : property.getValidators()) {
                            Map<String, Object> validatorProperties = new HashMap<String, Object>();
                            validatorProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                            if (validator.getParameters() != null) {
                                validatorProperties.put(Constants.VALIDATOR_ARGUMENTS, validator.getParameters());
                            }
                            ResourceUtil.getOrCreateResource(rr, validators.getPath() + "/" + validator.getValidator().getClass().getName(),
                                    validatorProperties, null, true);
                        }
                    }
                }
            }
        }
    }

    private Resource createValidationModelChildResource(Resource parentResource, String name, String nameRegex,
            boolean isOptional, ResourceProperty... properties) throws PersistenceException {
        ResourceResolver rr = parentResource.getResourceResolver();
        Resource modelChildren = rr.create(parentResource, Constants.CHILDREN, primaryTypeUnstructuredMap);
        Resource child = rr.create(modelChildren, name, primaryTypeUnstructuredMap);
        ModifiableValueMap mvm = child.adaptTo(ModifiableValueMap.class);
        if (nameRegex != null) {
            mvm.put(Constants.NAME_REGEX, nameRegex);
        }
        mvm.put(Constants.OPTIONAL, isOptional);
        createValidationModelProperties(child, properties);
        return child;
    }
/*
    private class TestProperty {
        public boolean optional;
        public boolean multiple;
        final String name;
        String nameRegex;
        final Map<String, String[]> validators;

        TestProperty(String name) {
            validators = new HashMap<String, String[]>();
            this.name = name;
            this.nameRegex = null;
            this.optional = false;
            this.multiple = false;
        }

        TestProperty setNameRegex(String nameRegex) {
            this.nameRegex = nameRegex;
            return this;
        }

        TestProperty addValidator(String name, String... parameters) {
            if (parameters.length == 0) {
                validators.put(name, null);
            } else {
                validators.put(name, parameters);
            }
            return this;
        }
    }*/
}
