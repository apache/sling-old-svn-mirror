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
package org.apache.sling.test;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Dictionary;
import java.util.Enumeration;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.options.AbstractDelegateProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Spin the cache up in a container to verify we have a working system.
 * Parts shamelessly borrowed from the Integration Test class in commons.classloader.
 */
public  abstract class AbstractOSGiRunner {

	// the name of the system property providing the bundle file to be installed
	// and tested
	private static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";



	@ProbeBuilder
	public TestProbeBuilder _extendProbe(TestProbeBuilder builder) {
		System.err.println("Parent Builder");
		
		String imports = getImports();
		if ( !imports.startsWith(",")) {
			imports = ","+imports;
		}
		builder.setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework,org.apache.sling.test"+imports);
		builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.ops4j.pax.exam,org.junit,javax.inject,org.ops4j.pax.exam.options");
		builder.setHeader("Bundle-ManifestVersion", "2");
		
		return builder;
	}


	/**
	 * @return the artifact name to assist determining the correct jar to load from target/
	 */
	protected abstract String getArtifactName();



	/**
	 * @return a string of additional imports to the builder
	 */
	protected String getImports() {
		return "";
	}

	protected Option[] getOptions() {
		return options();
	}



	@Configuration
	public Option[] configuration() {

		String bundleFileName = getBundleFileName();
		final File bundleFile = new File(bundleFileName);
		if (!bundleFile.canRead()) {
			throw new IllegalArgumentException("Cannot read from bundle file "
					+ bundleFileName + " specified in the "
					+ BUNDLE_JAR_SYS_PROP + " system property");
		}

		return options(
				composite(getOptions()),
				provision(
						// url resolution for pax
						mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.3.5"),
						// to support declarative services
						mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.0"),
						// bound to need servlet api
						mavenBundle("org.apache.felix", "javax.servlet","1.0.0"),
						// standard items.
						mavenBundle("commons-collections", "commons-collections", "3.2.1"), // 3.2.1 is a bundle, 3.2 is not
						mavenBundle("commons-io", "commons-io", "1.4"),
						// export ourselves
						wrappedBundle(mavenBundle("org.apache.sling","org.apache.sling.commons.cache.container-test")).exports(AbstractOSGiRunner.class.getPackage().getName()),
						// add the project thats being tested.
						bundle(bundleFile.toURI().toString())
				),
				// below is instead of normal Pax Exam junitBundles() to deal
				// with build server issue
				new DirectURLJUnitBundlesOption(),
				systemProperty("pax.exam.invoker").value("junit"),
				bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link")
				);
	}











	private String getBundleFileName() {
		String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP);
		if (bundleFileName == null) {
			// are we in an IDE, can we guess the jar name. 
			File target = new File("target");
			final String artifactName = getArtifactName();
			System.err.println("Listing files in "+target.getAbsolutePath()+" looking for "+artifactName);
			
			File[] jars = target.listFiles(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					if (!name.endsWith(".jar")) {
						return false;
					}
					if (name.endsWith("sources.jar")) {
						return false;
					}
					return name
							.startsWith(artifactName);
				}
			});
			if (jars.length > 0) {
				bundleFileName = jars[0].getAbsolutePath();
			}
		}
		return bundleFileName;
	}



	
	protected void check() {
		try {
			Class<?> t = this.getClass().getClassLoader().loadClass("javax.transaction.TransactionManager");
			System.err.println("Loaded Class "+t+" from classloader "+t.getClassLoader());
			ClassLoader cl = t.getClassLoader();
			if ( cl instanceof URLClassLoader ) {
				URLClassLoader u = (URLClassLoader) cl;
				for ( URL url : u.getURLs() ) {
					System.err.println("Classloadr url"+url);
				}
			}
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Try and load a class from each bundle in turn.
	 * @param bundleContext
	 * @param classname
	 */
	protected void findClassHome(BundleContext bundleContext, String classname) {
		System.err.println("Bundle is "+bundleContext);
		for ( Bundle b : bundleContext.getBundles()) {
			System.err.println("Bundle is "+bundleContext);
			if ( classname != null ) {
				try{
					Class<?> t = b.loadClass(classname);
					ClassLoader cl = t.getClassLoader();
					System.err.println("Loaded  "+t+" "+cl);
				} catch ( Exception e ) {
				}
			}
		}
		
	}
	
	/**
	 * List all the bundles showing services and headers.
	 * @param bundleContext
	 */
	protected void listBundles(BundleContext bundleContext) {
		System.err.println("Bundle is "+bundleContext);
		for ( Bundle b : bundleContext.getBundles()) {
			System.err.println("Bundle is "+bundleContext);
			Dictionary<String, String> headers = b.getHeaders();
			for( Enumeration<String> e = headers.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				System.err.println("	Header "+key+" "+headers.get(key));
			}
			ServiceReference[] srs = b.getRegisteredServices();
			if  (srs != null ) {
				for ( ServiceReference sr: srs) {
					System.err.println("	Service "+bundleContext);
				}
			}
		}
	}

	/**
	 * This code is taken from commons.classloader.it.DynamicClassLoaderIT Clone
	 * of Pax Exam's JunitBundlesOption which uses a direct URL to the
	 * SpringSource JUnit bundle to avoid some weird repository issues on the
	 * Apache build server.
	 */
	private static class DirectURLJUnitBundlesOption extends
			AbstractDelegateProvisionOption<DirectURLJUnitBundlesOption> {

		/**
		 * Constructor.
		 */
		public DirectURLJUnitBundlesOption() {
			super(
					bundle("http://repository.springsource.com/ivy/bundles/external/org.junit/com.springsource.org.junit/4.9.0/com.springsource.org.junit-4.9.0.jar"));
			noUpdate();
			startLevel(START_LEVEL_SYSTEM_BUNDLES);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.format("DirectURLJUnitBundlesOption{url=%s}",
					getURL());
		}

		/**
		 * {@inheritDoc}
		 */
		protected DirectURLJUnitBundlesOption itself() {
			return this;
		}
	}
}
