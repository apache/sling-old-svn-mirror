/*
 * $Id: BufferedPrintWriter_test.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @version $Revision: 1.5 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since
 * @audience
 */
public class BufferedPrintWriter_test extends TestCase {

    protected TestPW basePW;
    protected BufferedPrintWriter bufferedPW;

    public BufferedPrintWriter_test(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        basePW = new TestPW(new StringWriter());
        bufferedPW = new BufferedPrintWriter(basePW, 10);
    }

    protected void tearDown() throws Exception {
        bufferedPW.close();
        basePW.close();

        bufferedPW = null;
        basePW = null;
    }

    //---------- test suite ----------------------------------------------------

    //--- single ---------------------------------------------------------------

    public void testWriteSingle1() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(1, 0, false);
    }

    public void testWriteSingle10() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(10, 0, false);
    }

    public void testWriteSingle11() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(11, 10, false);
    }

    public void testWriteSingle1Flush() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(1, 1, true);
    }

    public void testWriteSingle10Flush() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(10, 10, true);
    }

    public void testWriteSingle11Flush() throws IOException {
        basePW.reset();
        writeAndCheckSingleByte(11, 11, true);
    }

    //--- array ----------------------------------------------------------------

    public void testWriteArray1() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(1, 0, false);
    }

    public void testWriteArray10() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(10, 0, false);
    }

    public void testWriteArray11() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(11, 10, false);
    }

    public void testWriteArray1Flush() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(1, 1, true);
    }

    public void testWriteArray10Flush() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(10, 10, true);
    }

    public void testWriteArray11Flush() throws IOException {
        basePW.reset();
        writeAndCheckCharArray(11, 11, true);
    }

    //--- single / array -------------------------------------------------------

    public void testWriteSingle5Array4() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 4, 0, false, false);
    }

    public void testWriteSingle5Array5() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 5, 0, false, false);
    }

    public void testWriteSingle5Array6() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 6, 10, false, false);
    }

    public void testWriteSingle5Array12() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 12, 10, false, false);
    }

    public void testWriteSingle5Array4InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 4, 5, true, false);
    }

    public void testWriteSingle5Array5InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 5, 5, true, false);
    }

    public void testWriteSingle5Array6InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 6, 5, true, false);
    }

    public void testWriteSingle5Array12InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 12, 15, true, false);
    }

    public void testWriteSingle5Array4EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 4, 9, false, true);
    }

    public void testWriteSingle5Array5EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 5, 10, false, true);
    }

    public void testWriteSingle5Array6EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 6, 11, false, true);
    }

    public void testWriteSingle5Array12EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 12, 17, false, true);
    }

    public void testWriteSingle5Array4BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 4, 9, true, true);
    }

    public void testWriteSingle5Array5BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 5, 10, true, true);
    }

    public void testWriteSingle5Array6BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 6, 11, true, true);
    }

    public void testWriteSingle5Array12BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleCharArray(5, 12, 17, true, true);
    }

    //--- string ---------------------------------------------------------------

    public void testWriteString1() throws IOException {
        basePW.reset();
        writeAndCheckString(1, 0, false);
    }

    public void testWriteString10() throws IOException {
        basePW.reset();
        writeAndCheckString(10, 0, false);
    }

    public void testWriteString11() throws IOException {
        basePW.reset();
        writeAndCheckString(11, 10, false);
    }

    public void testWriteString1Flush() throws IOException {
        basePW.reset();
        writeAndCheckString(1, 1, true);
    }

    public void testWriteString10Flush() throws IOException {
        basePW.reset();
        writeAndCheckString(10, 10, true);
    }

    public void testWriteString11Flush() throws IOException {
        basePW.reset();
        writeAndCheckString(11, 11, true);
    }

    //--- single / string ------------------------------------------------------

    public void testWriteSingle5String4() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 4, 0, false, false);
    }

    public void testWriteSingle5String5() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 5, 0, false, false);
    }

    public void testWriteSingle5String6() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 6, 10, false, false);
    }

    public void testWriteSingle5String12() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 12, 10, false, false);
    }

    public void testWriteSingle5String4InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 4, 5, true, false);
    }

    public void testWriteSingle5String5InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 5, 5, true, false);
    }

    public void testWriteSingle5String6InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 6, 5, true, false);
    }

    public void testWriteSingle5String12InterFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 12, 15, true, false);
    }

    public void testWriteSingle5String4EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 4, 9, false, true);
    }

    public void testWriteSingle5String5EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 5, 10, false, true);
    }

    public void testWriteSingle5String6EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 6, 11, false, true);
    }

    public void testWriteSingle5String12EndFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 12, 17, false, true);
    }

    public void testWriteSingle5String4BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 4, 9, true, true);
    }

    public void testWriteSingle5String5BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 5, 10, true, true);
    }

    public void testWriteSingle5String6BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 6, 11, true, true);
    }

    public void testWriteSingle5String12BothFlush() throws IOException {
        basePW.reset();
        writeAndCheckSingleString(5, 12, 17, true, true);
    }

    //---------- main ----------------------------------------------------------

    public static void main(String[] args) throws Exception {
        BufferedPrintWriter_test tester =
                new BufferedPrintWriter_test("test");
        tester.setUp();

        tester.testWriteArray11();
        tester.testWriteArray11Flush();

        tester.tearDown();
    }

    //---------- internal ------------------------------------------------------

    private int writeSingleByte(int writeNum, int add) {
        for (int i=0; i < writeNum; i++, add++) {
            bufferedPW.write('a'+add);
        }

        return add;
    }

    private int writeCharArray(int writeNum, int add) {
        char[] buf = new char[writeNum];
        for (int i=0; i < writeNum; i++, add++) {
            buf[i] = (char) ('a' + add);
        }
        bufferedPW.write(buf);

        return add;
    }

    private int writeString(int writeNum, int add) {
        char[] buf = new char[writeNum];
        for (int i=0; i < writeNum; i++, add++) {
            buf[i] = (char) ('a' + add);
        }
        bufferedPW.write(new String(buf));

        return add;
    }

    private void checkData(int expectNum) {
        String data = basePW.getString();

        assertEquals("Output size", expectNum, data.length());
        for (int i=0; i < data.length(); i++) {
            assertEquals("Output data "+i, 'a'+i, data.charAt(i));
        }
    }

    private void writeAndCheckSingleByte(int writeNum, int expectNum, boolean flush) {
        writeSingleByte(writeNum, 0);
        if (flush) {
            bufferedPW.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckCharArray(int writeNum, int expectNum, boolean flush) {
        writeCharArray(writeNum, 0);
        if (flush) {
            bufferedPW.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckString(int writeNum, int expectNum, boolean flush) {
        writeString(writeNum, 0);
        if (flush) {
            bufferedPW.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckSingleCharArray(int writeSingleNum, int writeArrayNum, int expectNum, boolean interFlush, boolean endFlush) {
        int add = writeSingleByte(writeSingleNum, 0);

        if (interFlush) {
            bufferedPW.flush();
        }

        writeCharArray(writeArrayNum, add);

        if (endFlush) {
            bufferedPW.flush();
        }

        checkData(expectNum);
    }

    private void writeAndCheckSingleString(int writeSingleNum, int writeStringNum, int expectNum, boolean interFlush, boolean endFlush) {
        int add = writeSingleByte(writeSingleNum, 0);

        if (interFlush) {
            bufferedPW.flush();
        }

        writeString(writeStringNum, add);

        if (endFlush) {
            bufferedPW.flush();
        }

        checkData(expectNum);
    }

    //---------- internal ServletOutputStream helper ---------------------------

    static class TestPW extends PrintWriter {

        private StringBuffer buf;

        TestPW(StringWriter sw) {
            super(new StringWriter());
            this.buf = ((StringWriter) out).getBuffer();
        }

        public void reset() {
            buf.delete(0, buf.length());
        }

        public String getString() {
            return buf.toString();
        }
    }
}
