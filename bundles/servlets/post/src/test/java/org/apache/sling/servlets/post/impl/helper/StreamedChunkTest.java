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

package org.apache.sling.servlets.post.impl.helper;


import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ieb on 06/09/2016.
 */
public class StreamedChunkTest {

    @Test
    public void testContentRange() {
        checkRange("bytes 0-1234/1235", 0, 1235, 1235);
        checkRange("bytes 10-123/1235", 10,123-10+1,1235);
        checkRange("bytes 12-123/*", 12, 123 - 12+1, -1);
        checkInvalidRange("byte 10-123/1234"); // byte is not valid.
        checkInvalidRange("bytes 1000-123/1234"); // offset before end
        checkInvalidRange("bytes 1000-12300/1234"); // end before length
        checkInvalidRange("bytes 1000-12300/big"); // big not valid
        checkInvalidRange("bytes 1000-12300/"); // no length
        checkInvalidRange("bytes 1000-12300"); // no length
    }

    private void checkInvalidRange(String rangeHeader) {
        try {
            StreamedChunk.ContentRange cr = new StreamedChunk.ContentRange(rangeHeader);
            Assert.fail("Should have rejected "+rangeHeader);
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private void checkRange(String rangeHeader, long offset, long range, long length) {
        StreamedChunk.ContentRange cr = new StreamedChunk.ContentRange(rangeHeader);
        Assert.assertEquals(offset,cr.offset);
        Assert.assertEquals(range,cr.range);
        Assert.assertEquals(length,cr.length);

    }

}
