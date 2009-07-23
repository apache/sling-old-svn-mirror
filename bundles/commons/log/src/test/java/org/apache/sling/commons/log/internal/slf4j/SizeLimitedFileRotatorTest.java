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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SizeLimitedFileRotatorTest extends AbstractSlingLogTest {

    private File theFile;

    private List<File> rotatedFiles = new ArrayList<File>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        theFile = getBaseFile();
        fillFile(theFile);
    }

    public void test_size_check() {
        // basic requirements for size cmparison test
        assertTrue("Expect file to contain 10 bytes", theFile.length() == 10);

        final SizeLimitedFileRotator slfr05 = new SizeLimitedFileRotator(-1, 5);
        assertTrue("Expect isRotationDue for 5 bytes",
            slfr05.isRotationDue(theFile));

        final SizeLimitedFileRotator slfr09 = new SizeLimitedFileRotator(-1, 9);
        assertTrue("Expect isRotationDue for 9 bytes",
            slfr09.isRotationDue(theFile));

        final SizeLimitedFileRotator slfr10 = new SizeLimitedFileRotator(-1, 10);
        assertFalse("Not expecting isRotationDue for 10 bytes",
            slfr10.isRotationDue(theFile));

        final SizeLimitedFileRotator slfr15 = new SizeLimitedFileRotator(-1, 15);
        assertFalse("Not expecting isRotationDue for 15 bytes",
            slfr15.isRotationDue(theFile));
    }

    public void test_no_rotation_with_negative_maxNum() {
        // basic requirement: theFile must exist
        assertTrue("Require test file", theFile.exists());

        // no rotation, just remove the log file on rotation
        final SizeLimitedFileRotator slfr_none = new SizeLimitedFileRotator(-1,
            100);
        slfr_none.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure no rotation exists
        assertFalse("No rotation file expected", new File(
            theFile.getAbsolutePath() + ".0").exists());
    }

    public void test_no_rotation_with_zero_maxNum() {
        // basic requirement: theFile must exist
        assertTrue("Require test file", theFile.exists());

        // no rotation, just remove the log file on rotation
        final SizeLimitedFileRotator slfr_none = new SizeLimitedFileRotator(0,
            100);
        slfr_none.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure no rotation exists
        assertFalse("No rotation file expected", new File(
            theFile.getAbsolutePath() + ".0").exists());
    }

    public void test_single_file_rotation() {
        // basic requirement: theFile must exist
        assertTrue("Require test file", theFile.exists());

        // no rotation, just remove the log file on rotation
        final SizeLimitedFileRotator slfr_single = new SizeLimitedFileRotator(
            1, 100);
        slfr_single.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure one rotation exists
        assertTrue("Rotation file 0 expected", getRotatedFile(0).exists());
        assertFalse("No rotation file 1 expected", getRotatedFile(1).exists());

        // fill and rotate the file again
        fillFile(theFile);
        slfr_single.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure one rotations exists
        assertTrue("Rotation file 0 expected", getRotatedFile(0).exists());
        assertFalse("No rotation file 1 expected", getRotatedFile(1).exists());
        assertFalse("No rotation file 2 expected", getRotatedFile(2).exists());

    }

    public void test_two_file_rotation() {
        // basic requirement: theFile must exist
        assertTrue("Require test file", theFile.exists());

        // no rotation, just remove the log file on rotation
        final SizeLimitedFileRotator slfr_two = new SizeLimitedFileRotator(2,
            100);
        slfr_two.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure one rotation exists
        assertTrue("Rotation file 0 expected", getRotatedFile(0).exists());
        assertFalse("No rotation file 1 expected", getRotatedFile(1).exists());

        // fill and rotate the file again
        fillFile(theFile);
        slfr_two.rotate(theFile);
        assertFalse("The file must be removed", theFile.exists());

        // ensure one rotations exists
        assertTrue("Rotation file 0 expected", getRotatedFile(0).exists());
        assertTrue("Rotation file 1 expected", getRotatedFile(1).exists());
        assertFalse("No rotation file 2 expected", getRotatedFile(2).exists());

    }

    private static void fillFile(File file) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write("cafebabe!!".getBytes("ISO-8859-1"));
        } catch (IOException ioe) {
            System.err.println("Failed prefilling " + file);
            ioe.printStackTrace(System.err);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private File getRotatedFile(int index) {
        File rotatedFile = new File(theFile.getAbsolutePath() + "." + index);
        rotatedFiles.add(rotatedFile);
        return rotatedFile;
    }
}
