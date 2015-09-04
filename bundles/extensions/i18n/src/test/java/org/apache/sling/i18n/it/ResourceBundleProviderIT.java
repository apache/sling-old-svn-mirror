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
package org.apache.sling.i18n.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.i18n.ResourceBundleProvider;
import org.apache.sling.i18n.impl.Message;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.paxexam.util.SlingPaxOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ResourceBundleProviderIT {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final int RETRY_TIMEOUT_MSEC = 10000;
    public static final String MSG_KEY = "foo";

    @Inject
    private SlingRepository repository;

    @Inject
    private ResourceBundleProvider resourceBundleProvider;

    private Session session;
    private Node i18nRoot;
    private Node deRoot;
    private Node frRoot;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final File thisProjectsBundle = new File(System.getProperty( "bundle.file.name", "BUNDLE_FILE_NOT_SET" ));
        final String launchpadVersion = System.getProperty("sling.launchpad.version", "LAUNCHPAD_VERSION_NOT_SET");
        return new DefaultCompositeOption(
                SlingPaxOptions.defaultLaunchpadOptions(launchpadVersion),
                CoreOptions.provision(CoreOptions.bundle(thisProjectsBundle.toURI().toString()))
                ).getOptions();
    }

    static abstract class Retry {
        Retry(int timeoutMsec) {
            final long timeout = System.currentTimeMillis() + timeoutMsec;
            Throwable lastT = null;
            while(System.currentTimeMillis() < timeout) {
                try {
                    lastT = null;
                    exec();
                    break;
                } catch(Throwable t) {
                    lastT = t;
                }
            }

            if(lastT != null) {
                fail("Failed after " + timeoutMsec + " msec: " + lastT);
            }
        }

        protected abstract void exec() throws Exception;
    }

    @Test
    public void test_dummy() {
        System.err.println("All tests are disabled for now ....");
    }

    @Before
    public void setup() throws RepositoryException {
        session = repository.loginAdministrative(null);
        final Node root = session.getRootNode();
        Node libs = null;
        if(root.hasNode("libs")) {
           libs = root.getNode("libs");
        } else {
           libs = root.addNode("libs", "nt:unstructured");
        }
        i18nRoot = libs.addNode("i18n", "nt:unstructured");
        deRoot = addLanguageNode(i18nRoot, "de");
        frRoot = addLanguageNode(i18nRoot, "fr");
        session.save();
    }

    @After
    public void cleanup() throws RepositoryException {
        i18nRoot.remove();
        session.save();
        session.logout();
    }

    private Node addLanguageNode(Node parent, String language) throws RepositoryException {
        final Node child = parent.addNode(language, "nt:folder");
        child.addMixin("mix:language");
        child.setProperty("jcr:language", language);
        return child;
    }

    private void assertMessages(final String deMessage, final String frMessage) {
        new Retry(RETRY_TIMEOUT_MSEC) {
            protected void exec() {
                {
                    final ResourceBundle de = resourceBundleProvider.getResourceBundle(Locale.GERMAN);
                    assertNotNull(de);
                    assertEquals(deMessage, de.getString(MSG_KEY));
                }
                {
                    final ResourceBundle fr = resourceBundleProvider.getResourceBundle(Locale.FRENCH);
                    assertNotNull(fr);
                    assertEquals(frMessage, fr.getString(MSG_KEY));
                }
            }
        };
    }

    @Test
    public void testRepositoryName() {
        final String name = repository.getDescriptor("jcr.repository.name");
        log.info("Test running on  {} repository {}",
                name,
                repository.getDescriptor("jcr.repository.version"));

        // We could use JUnit categories to select tests, as we
        // do in our integration tests, but let's avoid a dependency on
        // that in this module
        if(System.getProperty("sling.run.modes", "").contains("oak")) {
            assertEquals("Apache Jackrabbit Oak", name);
        } else {
            assertEquals("Jackrabbit", name);
        }
    }

    @Test
    public void testChangesDetection() throws RepositoryException {
        new Message("", MSG_KEY, "DE_message", false).add(deRoot);
        new Message("", MSG_KEY, "FR_message", false).add(frRoot);
        session.save();
        assertMessages("DE_message", "FR_message");

        new Message("", MSG_KEY, "DE_changed", false).add(deRoot);
        new Message("", MSG_KEY, "FR_changed", false).add(frRoot);
        session.save();
        assertMessages("DE_changed", "FR_changed");
    }
}
