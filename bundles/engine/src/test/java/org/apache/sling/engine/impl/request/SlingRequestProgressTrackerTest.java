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
package org.apache.sling.engine.impl.request;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class SlingRequestProgressTrackerTest {

    private SlingRequestProgressTracker tracker;
    
    @Before
    public void setup() {
        tracker = new SlingRequestProgressTracker();
    }
    
    private void addTestData() {
        tracker.startTimer("foo");
        tracker.log("one {0}, two {1}, three {2}", "eins", "zwei", "drei");
        tracker.startTimer("bar");
        tracker.logTimer("bar");
        tracker.logTimer("foo");
        tracker.done();
    }
    
    @Test
    public void messageFormatting() {
        final String[] expected = {
                "TIMER_START{Request Processing}\n",
                "COMMENT timer_end format is {<elapsed microseconds>,<timer name>} <optional message>\n",
                "TIMER_START{foo}\n",
                "LOG one eins, two zwei, three drei\n",
                "TIMER_START{bar}\n",
                "TIMER_END{?,bar}\n",
                "TIMER_END{?,foo}\n",
                "TIMER_END{?,Request Processing} Request Processing\n"
        };

        addTestData();
        final Iterator<String> messages = tracker.getMessages();
        int messageCounter = 0;
        while (messages.hasNext()) {
            final String m = messages.next();
            final String e = expected[messageCounter++];
            if (e.startsWith("TIMER_END{")) {
                // account for the counter in the string
                assertEquals(substringAfter(e, ','), substringAfter(m, ','));
            } else {
                // strip off counter
                assertEquals(e, m.substring(8));
            }
        }

        assertEquals(expected.length, messageCounter);
    }
    
    @Test
    public void dump() throws IOException {
        addTestData();
        final StringWriter w = new StringWriter();
        tracker.dump(new PrintWriter(w));
        w.flush();
        final String result = w.toString();
        
        final String [] expected = {
                "TIMER_START{Request Processing}",
                "TIMER_START{foo}",
                "Dumping SlingRequestProgressTracker Entries"
        };
        for(String exp : expected) {
            if(!result.contains(exp)) {
                fail("Expected result to contain [" + exp + "] but was [" + result + "]");
            }
        }
        
        int lineCount = 0;
        final BufferedReader br = new BufferedReader(new StringReader(result));
        while(br.readLine() != null) {
            lineCount++;
        }
        assertEquals(9, lineCount);
    }
    
    @Test
    public void duration() throws InterruptedException {
        Thread.sleep(50);
        tracker.log("after the wait");
        assertTrue(tracker.getDuration() >= 50);
    }
    
    @Test
    public void durationWithDone() throws InterruptedException {
        Thread.sleep(25);
        tracker.done();
        final long d = tracker.getDuration();
        assertTrue(d >= 25);
        Thread.sleep(25);
        tracker.log("Some more stuff");
        assertEquals(d, tracker.getDuration());
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void removeFails() {
        tracker.getMessages().remove();
    }

    private String substringAfter(String string, char ch) {
        final int pos = string.indexOf(ch);
        return string.substring(pos);
    }

}
