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
package org.apache.sling.junit.impl.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.runner.BaseTestRunner;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.Renderer;
import org.apache.sling.junit.RendererFactory;
import org.apache.sling.junit.TestSelector;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/** XML renderer for JUnit servlet */
@Component(immediate=false)
@Service
public class XmlRenderer extends RunListener implements Renderer, RendererFactory {
    
    /**
     * This renderer's extension
     */
    public static final String EXTENSION = "xml";

	/**
	 * Writer used for output.
	 */
	private PrintWriter output;

	/**
	 * The XML document.
	 */
	private Document doc;

	/**
	 * The wrapper for the testsuites.
	 */
	private Element suitesElement;

	/**
	 * The wrapper for the whole testsuite.
	 */
	private Element rootElement;

	/**
	 * Table to track tests.
	 */
	private Hashtable<Description, Element> testElements = new Hashtable<Description, Element>();

	/**
	 * List to track falures.
	 */
	private ArrayList<Description> failures = new ArrayList<Description>();

	/**
	 * Table to track test run times.
	 */
	Hashtable<Description, Long> tests = new Hashtable<Description, Long>();

	/**
	 * Test Suite name.
	 */
	private String name;

	/**
	 * Start time for the test suite.
	 */
	private long suiteStartTime = 0;

	/**
	 * Counter of test suites.
	 */
	private int testSuiteCount = 0;

    public Renderer createRenderer() { 
        return new XmlRenderer();
    }

    public boolean appliesTo(TestSelector selector) {
        return EXTENSION.equals(selector.getExtension());
    }

    public String getExtension() {
        return EXTENSION;
    }

	public void setup(HttpServletResponse response, String pageTitle) throws IOException, UnsupportedEncodingException {
        if(output != null) {
            throw new IllegalStateException("Output Writer already set");
        }
		suiteStartTime = System.currentTimeMillis();

		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		output = response.getWriter();

		doc = getDocumentBuilder().newDocument();

		suitesElement = doc.createElement("testsuites");

	}   

	public void info(String cssClass, String str) {
	}

	public void list(String cssClass, Collection<String> data) {
	}

	public void title(int level, String title) {
		if (level == 3)
			name = title;
	}
	
    public void link(String info, String url, String method) {
    }

	public void cleanup() {
		if (testSuiteCount > 1) {
			output.println(getStringFromElement(suitesElement));
		} else {
			output.println(getStringFromElement(rootElement));
		}
		output = null;
	}
	
    public RunListener getRunListener() {
        return this;
    }

	@Override
	public void testFailure(Failure failure) throws Exception {
		super.testFailure(failure);
		failures.add(failure.getDescription());

		Element nested = doc.createElement("failure");
		Element currentTest = testElements.get(failure.getDescription());

		currentTest.appendChild(nested);

		String message = failure.getMessage();
		if (message != null && message.length() > 0) {
			nested.setAttribute("message", message);
		}
		nested.setAttribute("type", failure.getClass().getName());

		String strace = getException(failure.getException());
		strace = BaseTestRunner.getFilteredTrace(strace);
		Text trace = doc.createTextNode(strace);
		nested.appendChild(trace);        

	}

	@Override
	public void testFinished(Description description) throws Exception {
		super.testFinished(description);

		Long startTime = tests.get(description);
		long totalTime = System.currentTimeMillis() - startTime.longValue();

		Element currentTest = (Element) testElements.get(description);

		currentTest.setAttribute("time", String.valueOf(totalTime / 1000.0));   

	}

	@Override
	public void testIgnored(Description description) throws Exception {
		super.testIgnored(description);
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		super.testRunFinished(result);
		String cssClass = "testRun ";
		if(result.getFailureCount() > 0) {
			cssClass += "failure";
		} else if(result.getIgnoreCount() > 0) {
			cssClass += "ignored";
		} else {
			cssClass += "success";
		}

		long suiteEndTime = System.currentTimeMillis();

		rootElement.setAttribute("name", name);

		rootElement.setAttribute("timestamp", String.valueOf(suiteEndTime));

		rootElement.setAttribute("hostname", getHostname());

		rootElement.setAttribute("tests", "" + result.getRunCount());
		rootElement.setAttribute("failures", "" + result.getFailureCount());
		//rootElement.setAttribute("errors", "" + result.getIgnoreCount());
		rootElement.setAttribute(
				"time", "" + ((suiteEndTime - suiteStartTime) / 1000.0));


	}

	@Override
	public void testRunStarted(Description description)
	throws Exception {
		super.testRunStarted(description);

		testSuiteCount++;
		rootElement = doc.createElement("testsuite");
		suitesElement.appendChild(rootElement);

		// Output properties
		Element propsElement = doc.createElement("properties");
		rootElement.appendChild(propsElement);        

	}

	@Override
	public void testStarted(Description description) throws Exception {
		super.testStarted(description);
		tests.put(description, new Long(System.currentTimeMillis()));

		Element currentTest = doc.createElement("testcase");
		String n = description.getDisplayName();
		n = n.substring(0, n.indexOf("("));
		currentTest.setAttribute("name",
				n == null ? "unknown" : n);
		currentTest.setAttribute("classname",description.getClassName());
		rootElement.appendChild(currentTest);
		testElements.put(description, currentTest);        

	}

	/**
	 * Create a DocumentBuilder.
	 * @return a DocumentBuilder.
	 */
	public static DocumentBuilder getDocumentBuilder() {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (Exception exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}   

	/**
	 * Convert an Element to a String representation
	 * @param element
	 * @return a String representation
	 */
	public static String getStringFromElement(Element element) {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer trans = tf.newTransformer();
			StringWriter sw = new StringWriter();
			trans.transform(new DOMSource(element), new StreamResult(sw));
			String elementString = sw.toString();
			return elementString;
		} catch (TransformerConfigurationException e) {
			System.err.println(getException(e));
		} catch (TransformerException e) {
			System.err.println(getException(e));
		}
		return "";
	}

	/**
	 * get the local hostname
	 * @return the name of the local host, or "localhost" if we cannot work it out
	 */
	private String getHostname()  {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}	

	/**
	 * Convert a Throwable object to its stack trace representation
	 * 
	 * @param t Throwable object
	 * @return String representation of the Throwable object
	 */
	public static String getException(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));

		return sw.getBuffer().toString();
	} // getException      

}
