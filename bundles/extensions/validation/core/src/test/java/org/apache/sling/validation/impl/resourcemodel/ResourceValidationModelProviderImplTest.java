package org.apache.sling.validation.impl.resourcemodel;

import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.jcr.MockQuery;
import org.apache.sling.testing.mock.jcr.MockQueryResult;
import org.apache.sling.testing.mock.jcr.MockQueryResultHandler;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.validation.api.ValidationModel;
import org.apache.sling.validation.api.Validator;
import org.apache.sling.validation.impl.validators.RegexValidator;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

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
    
    // extract resource type from strings like "/jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and @validatedResourceType="<some-resource-type>"]"
    private static final Pattern RESOURCE_TYPE_PATTERN = Pattern.compile(".*@validatedResourceType=\"([^\"]*)\".*");
    
    @Rule
    SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK); // we need the search capability

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
                // query looks like /jcr:root/apps//validation//*[@sling:resourceType="sling/validation/model" and @validatedResourceType="<some-resource-type>"]
                if (statement.startsWith("/jcr:root/")) {
                    statement = statement.substring("/jcr:root/".length() - 1);
                }
                // extract the prefix from the statement
                String prefix = Text.getAbsoluteParent(statement, 0);
                
                // extract the resource type from the statement
                Matcher matcher = RESOURCE_TYPE_PATTERN.matcher(statement);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Can only process query statements which contain a validatedResourceType but the statement is: " + statement);
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
        rr.close();
    }

    @Test
    public void testGetValidationModel() throws Exception {
        // compare models
        
        modelProvider.getModel(rr, relativeResourceType, validatorMap);
        

        TestProperty property = new TestProperty("field1");
        property.addValidator("org.apache.sling.validation.impl.validators.RegexValidator");
        Resource model1 = null, model2 = null;
        try {
            model1 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel1",
                    "sling/validation/test", new String[] { "/apps/validation" }, property);
            model2 = createValidationModelResource(rr, libsValidatorsRoot.getPath(), "testValidationModel2",
                    "sling/validation/test", new String[] { "/apps/validation/1", "/apps/validation/2" }, property);

            // BEST MATCHING PATH = /apps/validation/1; assume the applicable paths contain /apps/validation/2
            ValidationModel vm = validationService.getValidationModel("sling/validation/test",
                    "/apps/validation/1/resource");
            Assert.assertNotNull("Could not find validation model for 'sling/validation/test'", vm);
            assertThat(vm.getApplicablePaths(), Matchers.hasItemInArray("/apps/validation/2"));

            // BEST MATCHING PATH = /apps/validation; assume the applicable paths contain /apps/validation but not
            // /apps/validation/1
            vm = validationService.getValidationModel("sling/validation/test", "/apps/validation/resource");
            Assert.assertNotNull("Could not find validation model for 'sling/validation/test'", vm);
            assertThat(vm.getApplicablePaths(), Matchers.hasItemInArray("/apps/validation"));
            assertThat(vm.getApplicablePaths(), Matchers.not(Matchers.hasItemInArray("/apps/validation/1")));
        } finally {
            if (model1 != null) {
                rr.delete(model1);
            }
            if (model2 != null) {
                rr.delete(model2);
            }
        }
    }

    @Test
    public void testGetValidationModelWithOverlay() throws Exception {
        validationService.validators.put("org.apache.sling.validation.impl.validators.RegexValidator",
                new RegexValidator());

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
            validationService.getValidationModel("sling/validation/test",
                    "/apps/validation/1/resource");
        } finally {
            if (model != null) {
                rr.delete(model);
            }
        }
    }
}
