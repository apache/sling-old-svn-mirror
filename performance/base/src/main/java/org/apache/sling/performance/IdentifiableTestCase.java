package org.apache.sling.performance;

/**
 * Interface to be implemented by a class with PerformanceTests.
 * <p></p>The provided method @{link #testCaseName()} exposes the possibility to give a name to each instance of the
 * implementing class</p>
 */
public interface IdentifiableTestCase {
    public String testCaseName();
}
