/*
 * $Id: BufferedServletOutputStream_test.java 22189 2006-09-07 11:47:26Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.hermes.legacy;

import junit.framework.TestCase;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @version $Revision: 1.5 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since
 * @audience
 */
public class BufferedServletOutputStream_test extends TestCase {

    protected TestSOS baseSOS;
    protected BufferedServletOutputStream bufferedSOS;

    public BufferedServletOutputStream_test(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        baseSOS = new TestSOS();
        bufferedSOS = new BufferedServletOutputStream(baseSOS, 10);
    }

    protected void tearDown() throws Exception {
        bufferedSOS.close();
        baseSOS.close();

        bufferedSOS = null;
        baseSOS = null;
    }

    //---------- test suite ----------------------------------------------------

    //--- single ---------------------------------------------------------------

    public void testWriteSingle1() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(1, 0, false);
    }

    public void testWriteSingle10() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(10, 0, false);
    }

    public void testWriteSingle11() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(11, 10, false);
    }

    public void testWriteSingle1Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(1, 1, true);
    }

    public void testWriteSingle10Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(10, 10, true);
    }

    public void testWriteSingle11Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByte(11, 11, true);
    }

    //--- array ----------------------------------------------------------------

    public void testWriteArray1() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(1, 0, false);
    }

    public void testWriteArray10() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(10, 0, false);
    }

    public void testWriteArray11() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(11, 10, false);
    }

    public void testWriteArray1Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(1, 1, true);
    }

    public void testWriteArray10Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(10, 10, true);
    }

    public void testWriteArray11Flush() throws IOException {
        baseSOS.reset();
        writeAndCheckByteArray(11, 11, true);
    }

    //--- single / array -------------------------------------------------------

    public void testWriteSingle5Array4() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 4, 0, false, false);
    }

    public void testWriteSingle5Array5() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 5, 0, false, false);
    }

    public void testWriteSingle5Array6() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 6, 10, false, false);
    }

    public void testWriteSingle5Array12() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 12, 10, false, false);
    }

    public void testWriteSingle5Array4InterFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 4, 5, true, false);
    }

    public void testWriteSingle5Array5InterFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 5, 5, true, false);
    }

    public void testWriteSingle5Array6InterFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 6, 5, true, false);
    }

    public void testWriteSingle5Array12InterFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 12, 15, true, false);
    }

    public void testWriteSingle5Array4EndFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 4, 9, false, true);
    }

    public void testWriteSingle5Array5EndFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 5, 10, false, true);
    }

    public void testWriteSingle5Array6EndFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 6, 11, false, true);
    }

    public void testWriteSingle5Array12EndFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 12, 17, false, true);
    }

    public void testWriteSingle5Array4BothFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 4, 9, true, true);
    }

    public void testWriteSingle5Array5BothFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 5, 10, true, true);
    }

    public void testWriteSingle5Array6BothFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 6, 11, true, true);
    }

    public void testWriteSingle5Array12BothFlush() throws IOException {
        baseSOS.reset();
        writeAndCheckSingleByteArray(5, 12, 17, true, true);
    }

    //---------- main ----------------------------------------------------------

    public static void main(String[] args) throws Exception {
        BufferedServletOutputStream_test tester =
                new BufferedServletOutputStream_test("test");
        tester.setUp();

        tester.testWriteArray11();
        tester.testWriteArray11Flush();

        tester.tearDown();
    }

    //---------- internal ------------------------------------------------------

    private int writeSingleByte(int writeNum, int add) throws IOException {
        for (int i=0; i < writeNum; i++, add++) {
            bufferedSOS.write('a'+add);
        }

        return add;
    }

    private int writeByteArray(int writeNum, int add) throws IOException {
        byte[] buf = new byte[writeNum];
        for (int i=0; i < writeNum; i++, add++) {
            buf[i] = (byte) ('a' + add);
        }
        bufferedSOS.write(buf);

        return add;
    }

    private void checkData(int expectNum) {
        byte[] data = baseSOS.getBytes();

        assertEquals("Output size", expectNum, data.length);
        for (int i=0; i < data.length; i++) {
            assertEquals("Output data "+i, 'a'+i, data[i]);
        }
    }

    private void writeAndCheckSingleByte(int writeNum, int expectNum, boolean flush) throws IOException {
        writeSingleByte(writeNum, 0);
        if (flush) {
            bufferedSOS.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckByteArray(int writeNum, int expectNum, boolean flush) throws IOException {
        writeByteArray(writeNum, 0);
        if (flush) {
            bufferedSOS.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckSingleByteArray(int writeSingleNum, int writeArrayNum, int expectNum, boolean interFlush, boolean endFlush) throws IOException {
        int add = writeSingleByte(writeSingleNum, 0);

        if (interFlush) {
            bufferedSOS.flush();
        }

        writeByteArray(writeArrayNum, add);

        if (endFlush) {
            bufferedSOS.flush();
        }

        checkData(expectNum);
    }

    //---------- internal ServletOutputStream helper ---------------------------

    static class TestSOS extends ServletOutputStream {

        private ByteArrayOutputStream bos = new ByteArrayOutputStream();

        public void write(int b) {
            bos.write(b);
        }

        public void reset() {
            bos.reset();
        }

        public byte[] getBytes() {
            return bos.toByteArray();
        }
    }
}
