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
package org.apache.sling.serviceusermapping.impl;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.serviceusermapping.ServicePrincipalsValidator;
import org.apache.sling.serviceusermapping.ServiceUserValidator;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import junit.framework.TestCase;

public class ServiceUserMapperImplTest {
    private static final String BUNDLE_SYMBOLIC1 = "bundle1";

    private static final String BUNDLE_SYMBOLIC2 = "bundle2";

    private static final String BUNDLE_SYMBOLIC3 = "bundle3";

    private static final String BUNDLE_SYMBOLIC4 = "bundle4";

    private static final String BUNDLE_SYMBOLIC5 = "bundle5";

    private static final String SUB = "sub";

    private static final String NONE = "none";

    private static final String SAMPLE = "sample";

    private static final String ANOTHER = "another";

    private static final String SAMPLE_SUB = "sample_sub";

    private static final String ANOTHER_SUB = "another_sub";

    private static final Bundle BUNDLE1;

    private static final Bundle BUNDLE2;

    private static final Bundle BUNDLE3;

    private static final Bundle BUNDLE4;

    private static final Bundle BUNDLE5;

    static {
        BUNDLE1 = mock(Bundle.class);
        when(BUNDLE1.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC1);

        BUNDLE2 = mock(Bundle.class);
        when(BUNDLE2.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC2);

        BUNDLE3 = mock(Bundle.class);
        when(BUNDLE3.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC3);

        BUNDLE4 = mock(Bundle.class);
        when(BUNDLE4.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC4);

        BUNDLE5 = mock(Bundle.class);
        when(BUNDLE5.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC5);
    }

    @Test
    public void test_getServiceUserID() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
            BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
            BUNDLE_SYMBOLIC2 + "=" + ANOTHER, //
            BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
            BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB //
        });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
        TestCase.assertEquals(NONE, sum.getServiceUserID(BUNDLE3, null));
        TestCase.assertEquals(NONE, sum.getServiceUserID(BUNDLE3, SUB));
    }

    @Test
    public void test_getServiceUserIDwithDefaultMappingEnabledAndDefaultUser() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
            BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
            BUNDLE_SYMBOLIC2 + "=" + ANOTHER, //
            BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
            BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB //
        });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(true);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
        TestCase.assertEquals(NONE, sum.getServiceUserID(BUNDLE3, null));
        TestCase.assertEquals(NONE, sum.getServiceUserID(BUNDLE3, SUB));
    }

    @Test
    public void test_getServiceUserIDwithDefaultMappingEnabledAndNoDefaultUser() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
            BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
            BUNDLE_SYMBOLIC2 + "=" + ANOTHER, //
            BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
            BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB //
        });
        when(config.user_default()).thenReturn(null);
        when(config.user_enable_default_mapping()).thenReturn(true);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
        TestCase.assertEquals("serviceuser--" + BUNDLE_SYMBOLIC3, sum.getServiceUserID(BUNDLE3, null));
        TestCase.assertEquals("serviceuser--" + BUNDLE_SYMBOLIC3 + "--" + SUB, sum.getServiceUserID(BUNDLE3, SUB));
    }

    @Test
    public void test_getServiceUserID_WithServiceUserValidator() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
                BUNDLE_SYMBOLIC2 + "=" + ANOTHER, //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
                BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB //
        });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);
        ServiceUserValidator serviceUserValidator = new ServiceUserValidator() {

            @Override
            public boolean isValid(String serviceUserId, String serviceName,
                    String subServiceName) {
                if (SAMPLE.equals(serviceUserId)) {
                    return false;
                }
                return true;
            }
        };
        sum.bindServiceUserValidator(serviceUserValidator);

        TestCase.assertEquals(null, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(null, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
    }

    @Test
    public void test_getServicePrincipalNames() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=[" + SAMPLE + "]", //
                BUNDLE_SYMBOLIC2 + "=[ " + ANOTHER + " ]", //
                BUNDLE_SYMBOLIC3 + "=[" + SAMPLE + "," + ANOTHER + "]", //
                BUNDLE_SYMBOLIC4 + "=[ " + SAMPLE + ", " + ANOTHER + " ]", //
                BUNDLE_SYMBOLIC5 + "=[]", //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=[" + SAMPLE_SUB + "]", //
                BUNDLE_SYMBOLIC2 + ":" + SUB + "=[" + SAMPLE_SUB + "," + ANOTHER_SUB + "]" //
        });

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE1, null), SAMPLE);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE2, null), ANOTHER);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE3, null), SAMPLE, ANOTHER);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE4, null), SAMPLE, ANOTHER);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE5, null));
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE1, SUB), SAMPLE_SUB);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE2, SUB), SAMPLE_SUB, ANOTHER_SUB);
    }

    @Test
    public void test_getServicePrincipalNames_EmptySubService() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=[" + SAMPLE + "]", //
                BUNDLE_SYMBOLIC2 + "=[ " + ANOTHER + " ]", //
        });

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE1, ""), SAMPLE);
        assertEqualPrincipalNames(sum.getServicePrincipalNames(BUNDLE2, ""), ANOTHER);
    }

    @Test
    public void test_getServicePrincipalNames_WithUserNameConfig() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
        });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        assertNull(sum.getServicePrincipalNames(BUNDLE1, null));
        assertNull(SAMPLE_SUB, sum.getServicePrincipalNames(BUNDLE1, SUB));
    }

    @Test
    public void test_getServicePrincipalNames_IgnoresDefaultUser() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(true);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        assertNull(sum.getServicePrincipalNames(BUNDLE1, null));
        assertNull(sum.getServicePrincipalNames(BUNDLE1, SUB));
    }

    @Test
    public void test_getServicePrincipalnames_WithServicePrincipalsValidator() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=[" + SAMPLE + "]", //
                BUNDLE_SYMBOLIC2 + "=[" + SAMPLE + "," + ANOTHER + "]", //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=[" + SAMPLE + "," + SAMPLE_SUB + "]", //
                BUNDLE_SYMBOLIC2 + ":" + SUB + "=[" + ANOTHER_SUB + "," + SAMPLE_SUB + "," + SAMPLE + "]"//
        });

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);
        ServicePrincipalsValidator validator = new ServicePrincipalsValidator() {
            @Override
            public boolean isValid(Iterable<String> servicePrincipalNames, String serviceName, String subServiceName) {
                for (String pName : servicePrincipalNames) {
                    if (SAMPLE.equals(pName)) {
                        return false;
                    }
                }
                return true;
            }
        };
        sum.bindServicePrincipalsValidator(validator);

        assertNull(sum.getServicePrincipalNames(BUNDLE1, null));
        assertNull(sum.getServicePrincipalNames(BUNDLE2, null));
        assertNull(sum.getServicePrincipalNames(BUNDLE1, SUB));
        assertNull(sum.getServicePrincipalNames(BUNDLE2, SUB));
    }

    private static void assertEqualPrincipalNames(Iterable<String> result, String... expected) {
        if (expected == null) {
            assertNull(result);
        } else if (expected.length == 0) {
            assertFalse(result.iterator().hasNext());
        } else {
            Set<String> resultSet = new HashSet<>();
            Iterator<String> it = result.iterator();
            while (it.hasNext()) {
                resultSet.add(it.next());
            }
            Set<String> expectedSet = new HashSet<>();
            expectedSet.addAll(Arrays.asList(expected));
            assertEquals(expectedSet, resultSet);
        }
    }


    @Test
    public void test_amendment() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
        });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);
        final MappingConfigAmendment mca1 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca1Config = mock(MappingConfigAmendment.Config.class);
        when(mca1Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + "=" + ANOTHER});
        when(mca1Config.service_ranking()).thenReturn(100);
        Map<String, Object> mca1ConfigMap = new HashMap<>();
        mca1ConfigMap.put("user.mapping", mca1Config.user_mapping());
        mca1ConfigMap.put("service.ranking", mca1Config.service_ranking());
        mca1ConfigMap.put("service.id", 1L);

        mca1.configure(mca1Config);
        sum.bindAmendment(mca1, mca1ConfigMap);
        final MappingConfigAmendment mca2 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca2Config = mock(MappingConfigAmendment.Config.class);
        when(mca2Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB});
        when(mca2Config.service_ranking()).thenReturn(200);
        Map<String, Object> mca2ConfigMap = new HashMap<>();
        mca2ConfigMap.put("user.mapping", mca2Config.user_mapping());
        mca2ConfigMap.put("service.ranking", mca2Config.service_ranking());
        mca2ConfigMap.put("service.id", 2L);

        mca2.configure(mca2Config);
        sum.bindAmendment(mca2, mca2ConfigMap);

        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, null));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, null));
        TestCase.assertEquals(SAMPLE, sum.getServiceUserID(BUNDLE1, ""));
        TestCase.assertEquals(ANOTHER, sum.getServiceUserID(BUNDLE2, ""));
        TestCase.assertEquals(SAMPLE_SUB, sum.getServiceUserID(BUNDLE1, SUB));
        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, SUB));
    }

    @Test
    public void test_amendmentOverlap() {
        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {});
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.configure(null, config);

        final MappingConfigAmendment mca1 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca1Config = mock(MappingConfigAmendment.Config.class);
        when(mca1Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + "=" + ANOTHER});
        when(mca1Config.service_ranking()).thenReturn(100);
        Map<String, Object> mca1ConfigMap = new HashMap<>();
        mca1ConfigMap.put("user.mapping", mca1Config.user_mapping());
        mca1ConfigMap.put("service.ranking", mca1Config.service_ranking());

        mca1.configure(mca1Config);
        final MappingConfigAmendment mca2 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca2Config = mock(MappingConfigAmendment.Config.class);
        when(mca2Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + "=" + ANOTHER_SUB});
        when(mca2Config.service_ranking()).thenReturn(200);
        Map<String, Object> mca2ConfigMap = new HashMap<>();
        mca2ConfigMap.put("user.mapping", mca2Config.user_mapping());
        mca2ConfigMap.put("service.ranking", mca2Config.service_ranking());

        mca2.configure(mca2Config);

        sum.bindAmendment(mca1, mca1ConfigMap);
        sum.bindAmendment(mca2, mca2ConfigMap);

        TestCase.assertEquals(ANOTHER_SUB, sum.getServiceUserID(BUNDLE2, ""));
    }



    @Test
    public void test_amendmentServiceUserMapping() {

        ServiceUserMapperImpl.Config config = mock(ServiceUserMapperImpl.Config.class);
        when(config.user_mapping()).thenReturn(new String[] {
                BUNDLE_SYMBOLIC1 + "=" + SAMPLE, //
                BUNDLE_SYMBOLIC1 + ":" + SUB + "=" + SAMPLE_SUB, //
                });
        when(config.user_default()).thenReturn(NONE);
        when(config.user_enable_default_mapping()).thenReturn(false);

        final ServiceUserMapperImpl sum = new ServiceUserMapperImpl();
        sum.registerAsync = false;
        final ServiceRegistrationContextHelper context = new ServiceRegistrationContextHelper();
        sum.configure(context.getBundleContext(), config);

        TestCase.assertEquals(2, context.getRegistrations(ServiceUserMappedImpl.SERVICEUSERMAPPED).size());

        final MappingConfigAmendment mca1 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca1Config = mock(MappingConfigAmendment.Config.class);
        when(mca1Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + "=" + ANOTHER});
        when(mca1Config.service_ranking()).thenReturn(100);
        Map<String, Object> mca1ConfigMap = new HashMap<>();
        mca1ConfigMap.put("user.mapping", mca1Config.user_mapping());
        mca1ConfigMap.put("service.ranking", mca1Config.service_ranking());
        mca1ConfigMap.put("service.id", 1L);

        mca1.configure(mca1Config);
        sum.bindAmendment(mca1, mca1ConfigMap);

        TestCase.assertEquals(3, context.getRegistrations(ServiceUserMappedImpl.SERVICEUSERMAPPED).size());

        final MappingConfigAmendment mca2 = new MappingConfigAmendment();

        MappingConfigAmendment.Config mca2Config = mock(MappingConfigAmendment.Config.class);
        when(mca2Config.user_mapping()).thenReturn(new String[] {BUNDLE_SYMBOLIC2 + ":" + SUB + "=" + ANOTHER_SUB});
        when(mca2Config.service_ranking()).thenReturn(200);
        Map<String, Object> mca2ConfigMap = new HashMap<>();
        mca2ConfigMap.put("user.mapping", mca2Config.user_mapping());
        mca2ConfigMap.put("service.ranking", mca2Config.service_ranking());
        mca2ConfigMap.put("service.id", 2L);

        mca2.configure(mca2Config);
        sum.bindAmendment(mca2, mca2ConfigMap);

        TestCase.assertEquals(4, context.getRegistrations(ServiceUserMappedImpl.SERVICEUSERMAPPED).size());

        sum.unbindAmendment(mca1, mca1ConfigMap);

        TestCase.assertEquals(3, context.getRegistrations(ServiceUserMappedImpl.SERVICEUSERMAPPED).size());
    }


    private class ServiceRegistrationContextHelper {


        final BundleContext bundleContext = mock(BundleContext.class);
        final Map<String, Map<Object, Dictionary>> registrations = new HashMap<>();

        public ServiceRegistrationContextHelper() {
            when(bundleContext.registerService(any(String.class), any(Object.class), any(Dictionary.class)))
                    .then(new Answer<ServiceRegistration>() {
                        @Override
                        public ServiceRegistration answer(InvocationOnMock invocationOnMock) throws Throwable {

                            Object[] arguments = invocationOnMock.getArguments();
                            return registerService((String) arguments[0], arguments[1], (Dictionary) arguments[2]);
                        }
                    });
        }

        private ServiceRegistration registerService(String string, Object o, Dictionary dictionary) {
            if (!registrations.containsKey(string)) {
                registrations.put(string, new HashMap<Object, Dictionary>());
            }
            final Map<Object, Dictionary> serviceRegistrations = registrations.get(string);
            serviceRegistrations.put(o, dictionary);

            final Object registeredObject = o;


            return new ServiceRegistration() {
                @Override
                public ServiceReference getReference() {
                    return null;
                }

                @Override
                public void setProperties(Dictionary dictionary) {

                }

                @Override
                public void unregister() {
                    serviceRegistrations.remove(registeredObject);
                }
            };
        }

        public Map<Object, Dictionary> getRegistrations(String name) {
            return registrations.get(name);
        }

        public BundleContext getBundleContext() {
            return bundleContext;
        }

    }

}
