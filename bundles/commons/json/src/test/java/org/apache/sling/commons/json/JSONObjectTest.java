package org.apache.sling.commons.json;

import junit.framework.TestCase;

/**
 * @author vidar@idium.no
 * @since Apr 17, 2009 6:04:00 PM
 */
public class JSONObjectTest extends TestCase {
    private static final String KEY = "key";

    /**
     * See <a href="https://issues.apache.org/jira/browse/SLING-929">SLING-929</a>
     */
    public void testAppend() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.append(KEY, "value1");
        obj.append(KEY, "value2");
        Object result = obj.get(KEY);
        assertTrue("Did not create an array", result instanceof JSONArray);
    }

    /**
     * See <a href="https://issues.apache.org/jira/browse/SLING-929">SLING-929</a>
     */
    public void testFailAppend() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(KEY, "value1");
        try {
            obj.append(KEY, "value2");
            TestCase.fail("Accepted append() to a non-array property");
        } catch (JSONException ignore) {
            // this is expected
        }
    }

}
