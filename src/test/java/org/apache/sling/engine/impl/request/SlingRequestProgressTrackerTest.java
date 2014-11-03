package org.apache.sling.engine.impl.request;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class SlingRequestProgressTrackerTest {

    @Test
    public void messageFormatting() {
        final SlingRequestProgressTracker tracker = new SlingRequestProgressTracker();
        tracker.startTimer("foo");
        tracker.log("one {0}, two {1}, three {2}", "eins", "zwei", "drei");
        tracker.startTimer("bar");
        tracker.logTimer("bar");
        tracker.logTimer("foo");
        tracker.done();

        final String[] expected = {
                "TIMER_START{Request Processing}\n",
                "COMMENT timer_end format is {<elapsed msec>,<timer name>} <optional message>\n",
                "TIMER_START{foo}\n",
                "LOG one eins, two zwei, three drei\n",
                "TIMER_START{bar}\n",
                "TIMER_END{?,bar}\n",
                "TIMER_END{?,foo}\n",
                "TIMER_END{?,Request Processing} Request Processing\n"
        };

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

    private String substringAfter(String string, char ch) {
        final int pos = string.indexOf(ch);
        return string.substring(pos);
    }

}
