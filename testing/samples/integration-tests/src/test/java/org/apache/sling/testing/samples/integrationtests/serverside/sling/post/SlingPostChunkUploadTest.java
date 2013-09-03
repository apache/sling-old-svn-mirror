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
package org.apache.sling.testing.samples.integrationtests.serverside.sling.post;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingPostChunkUploadTest extends SlingTestBase {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CHUNK_NODE_NAME = "chunk";

    private static final String JCR_CONTENT = "jcr:content";

    FileCutter fileCutter = new FileCutter();

    String parentPath = "/tmp";

    /**
     * Test chunk upload without interruption.
     */
    @Test
    public void testChunkUpload() {
        try {
            // create 1000 byte file
            File file = createFile("helloworld", 100);
            int chunkSize = 400;
            String nodeName = file.getName();
            uploadChunks(parentPath, file, nodeName, 0, chunkSize,
                Integer.MAX_VALUE);

            // retrieve the stream on get and validate its content with uploaded
            // file
            HttpResponse response = httpGet(parentPath + "/" + nodeName);
            InputStream fis = new FileInputStream(file);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();
            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            response = uploadMultiPart(parentPath + "/" + nodeName, reqParams,
                null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + nodeName);
            Assert.assertEquals("status should be 404 not found ", 404,
                response.getStatusLine().getStatusCode());
            file.delete();
        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        }
    }

    /**
     * Test chunk upload after interruption. Test the use of variable chunk
     * size. After interruption, client retrieves chunk upload information and
     * resume upload with variable chunk size.
     */
    @Test
    public void testInterruptedChunkUpload() {
        try {
            // create 1700 bytes file
            File file = createFile("helloworld", 170);
            String nodeName = file.getName();
            int chunkSize = 200;
            // uplaod first chunk 200 bytes uploaded
            uploadChunks(parentPath, file, nodeName, 0, chunkSize, 1);
            JSONObject json = getChunkJson(parentPath + "/" + nodeName);
            validate(json, 200, 1);

            chunkSize = 300;
            // upload next two chunks of 300 each.total 800 bytes
            // uploaded
            uploadChunks(parentPath, file, nodeName, 200, chunkSize, 2);
            json = getChunkJson(parentPath + "/" + nodeName);
            validate(json, 800, 3);

            chunkSize = 400;
            // upload two chunk of 400 each. total 1600 bytes and 5 chunks
            // uploaded
            uploadChunks(parentPath, file, nodeName,
                json.getInt(SlingPostConstants.NT_SLING_CHUNKS_LENGTH),
                chunkSize, 2);
            json = getChunkJson(parentPath + "/" + nodeName);
            validate(json, 1600, 5);

            chunkSize = 500;
            uploadChunks(parentPath, file, nodeName,
                json.getInt(SlingPostConstants.NT_SLING_CHUNKS_LENGTH),
                chunkSize, Integer.MAX_VALUE);

            HttpResponse response = httpGet(parentPath + "/" + nodeName);
            InputStream fis = new FileInputStream(file);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();

            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            response = uploadMultiPart(parentPath + "/" + nodeName, reqParams,
                null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + nodeName);
            Assert.assertEquals("status should be 404 not found ", 404,
                response.getStatusLine().getStatusCode());
            file.delete();
        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        }
    }

    /**
     * Test two concurrent chunk upload. Second will fail. Test deletion of
     * incomplete upload and test new upload on the same path.
     */
    @Test
    public void testConcurrentChunkUpload() {
        try {
            // create 1700 bytes file
            File file = createFile("helloworld", 170);

            String nodeName = file.getName();
            int chunkSize = 200;
            // uplaod 3 chunk of 200 bytes uploaded
            uploadChunks(parentPath, file, nodeName, 0, chunkSize, 3);
            JSONObject json = getChunkJson(parentPath + "/" + file.getName());
            validate(json, 600, 3);

            // create 1000 bytes file
            File secondFile = createFile("helloearth", 100);
            chunkSize = 300;
            // upload next two chunks of 300 each.total 800 bytes
            // uploaded
            try {
                uploadChunks(parentPath, secondFile, nodeName, 0, chunkSize, 1);
                Assert.fail("second upload should fail");
            } catch (Exception ignore) {

            }
            try {
                uploadChunks(parentPath, secondFile, nodeName, 200, chunkSize,
                    2);
                Assert.fail("second upload should fail");
            } catch (Exception ignore) {

            }
            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            reqParams.put(":applyToChunks", "true");
            HttpResponse response = uploadMultiPart(
                parentPath + "/" + file.getName(), reqParams, null, null);
            Assert.assertEquals("status should be 200 OK ", 200,
                response.getStatusLine().getStatusCode());

            chunkSize = 200;
            uploadChunks(parentPath, secondFile, nodeName, 0, chunkSize,
                Integer.MAX_VALUE);

            response = httpGet(parentPath + "/" + nodeName);
            InputStream fis = new FileInputStream(secondFile);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();

            // clean uploaded file from repository
            reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            response = uploadMultiPart(parentPath + "/" + nodeName, reqParams,
                null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + nodeName);
            Assert.assertEquals("status should be 404 not found ", 404,
                response.getStatusLine().getStatusCode());
            file.delete();
            secondFile.delete();
        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        }
    }

    /**
     * Test chunk upload from midway
     */
    @Test
    public void testMidwayChunkUpload() {
        File file = null;
        try {
            // create 1700 bytes file
            file = createFile("helloworld", 170);
            int chunkSize = 200;
            try {
                uploadChunks(parentPath, file, file.getName(), 200, chunkSize,
                    1);
                Assert.fail("upload should fail");
            } catch (Exception ignore) {

            }

        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        } finally {
            file.delete();
        }
    }

    /**
     * Test upload on a existing node. Test that binary content doesn't get
     * updated until chunk upload finishes.
     */
    @Test
    public void testChunkUploadOnExistingNode() {
        try {
            // create 1700 bytes file
            File file = createFile("helloworld", 170);
            String nodeName = file.getName();
            InputStream fis = new FileInputStream(file);
            uploadMultiPart(parentPath, null, fis, file.getName());
            fis.close();
            HttpResponse response = httpGet(parentPath + "/" + nodeName);
            fis = new FileInputStream(file);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();
            // create 1000 bytes file
            File secondFile = createFile("helloearth", 100);
            int chunkSize = 200;
            // uplaod 3 chunk of 200 bytes uploaded
            uploadChunks(parentPath, secondFile, nodeName, 0, chunkSize, 3);
            JSONObject json = getChunkJson(parentPath + "/" + nodeName);
            validate(json, 600, 3);

            response = httpGet(parentPath + "/" + nodeName);
            fis = new FileInputStream(file);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();

            uploadChunks(parentPath, secondFile, nodeName, 600, chunkSize,
                Integer.MAX_VALUE);
            response = httpGet(parentPath + "/" + nodeName);
            fis = new FileInputStream(secondFile);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();

            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            response = uploadMultiPart(parentPath + "/" + nodeName, reqParams,
                null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + nodeName);
            Assert.assertEquals("status should be 404 not found ", 404,
                response.getStatusLine().getStatusCode());
            file.delete();
            secondFile.delete();
        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        } finally {

        }
    }

    /**
     * Test use case where file size is not known in advance. File parameter
     * "@Completed" indicates file completion.
     */
    @Test
    public void testChunkUploadOnStreaming() {
        try {
            // create 1700 bytes file
            File file = createFile("helloworld", 170);
            String nodeName = file.getName();
            InputStream fis = new FileInputStream(file);
            int chunkSize = 200;
            uploadPart(parentPath, file, file.getName(), 0, 0, chunkSize, false);
            uploadPart(parentPath, file, file.getName(), 0, 200, chunkSize,
                false);

            uploadPart(parentPath, file, file.getName(), 0, 400, chunkSize,
                true);
            fis.close();

            File secondFile = createFile("helloworld", 60);
            HttpResponse response = httpGet(parentPath + "/" + nodeName);
            fis = new FileInputStream(secondFile);
            Assert.assertEquals("content stream doesn't match", true,
                IOUtils.contentEquals(fis, new ByteArrayInputStream(
                    getRequestExecutor().getContent().getBytes())));
            fis.close();
            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(SlingPostConstants.RP_OPERATION, "delete");
            response = uploadMultiPart(parentPath + "/" + nodeName, reqParams,
                null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + nodeName);
            Assert.assertEquals("status should be 404 not found ", 404,
                response.getStatusLine().getStatusCode());
            file.delete();
            secondFile.delete();
        } catch (Exception e) {
            log.error("error:", e);
            Assert.fail("exception caught: " + e.getMessage());
        } finally {
        }
    }

    /**
     * create temporary file of size
     */
    private File createFile(String baseString, long times) throws Exception {
        OutputStream os = null;
        File file = null;
        try {
            file = File.createTempFile("test", "chunkupload");
            // create 1700 bytes file
            String data = appendString(baseString, times);
            os = new FileOutputStream(file);
            IOUtils.write(data, os);
            os.close();
            if (!file.exists()) {
                throw new Exception(file.getAbsolutePath() + "  not found");
            }
        } finally {
            try {
                os.close();
            } catch (Exception ignore) {
            }
        }
        return file;
    }

    /**
     * To query chunk upload in json
     */
    private JSONObject getChunkJson(String path) throws Exception {
        JSONObject json = null;
        HttpResponse response = httpGet(path + ".3.json");
        String jsonStr = getRequestExecutor().getContent();
        json = new JSONObject(jsonStr);
        if (json.has(JCR_CONTENT)) {
            json = json.getJSONObject(JCR_CONTENT);
        }
        return json;
    }

    private void validate(JSONObject json, int bytesUploaded, int expectedChunks)
            throws Exception {
        Assert.assertEquals("bytesUploaded didn't match", bytesUploaded,
            json.optInt(SlingPostConstants.NT_SLING_CHUNKS_LENGTH, 0));
        int chunkCount = 0;
        Iterator<String> itr = json.keys();
        while (itr != null && itr.hasNext()) {
            String key = itr.next();
            if (key.startsWith(CHUNK_NODE_NAME)) {
                chunkCount++;

            }

        }
        Assert.assertEquals("chunksuploaded didn't match", expectedChunks,
            chunkCount);
    }

    /**
     * upload 'numOfChunks' number of chunks starting from offset with size
     * equals to chunkSize or till end of file is reached.
     */

    private int uploadChunks(String path, File file, String nodeName,
            int offSet, int chunkSize, int numOfChunks) throws Exception {
        int length = new Long(file.length()).intValue();
        int chunkNumber = 0;
        while (offSet < length && chunkNumber < numOfChunks) {
            if (offSet + chunkSize >= length) {
                chunkSize = length - offSet;
            }
            uploadPart(path, file, nodeName, file.length(), offSet, chunkSize,
                null);
            offSet += chunkSize;
            chunkNumber++;
        }
        return chunkNumber;
    }

    /**
     * upload single chunk starting from offset of size chunkSize.
     * 
     * @param typeHint TODO
     */
    private HttpResponse uploadPart(String path, File file, String nodeName,
            long length, long offSet, Integer chunkSize, Boolean isComplete)
            throws Exception {
        byte[] buf = fileCutter.cutFile(file, offSet, chunkSize);
        log.debug(Thread.currentThread().getName() + ": uploading bytes from "
            + offSet + " to " + (offSet + chunkSize - 1));
        ByteArrayInputStream instream = new ByteArrayInputStream(buf);
        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put(nodeName + SlingPostConstants.SUFFIX_OFFSET,
            String.valueOf(offSet));
        if (length > 0) {
            reqParams.put(nodeName + SlingPostConstants.SUFFIX_LENGTH,
                Long.toString(length));
        }
        if (isComplete != null) {
            reqParams.put(nodeName + SlingPostConstants.SUFFIX_COMPLETED,
                isComplete.toString());
        }
        return uploadMultiPart(path, reqParams, instream, nodeName);

    }

    /**
     * send multipart post request to server.
     */

    private HttpResponse uploadMultiPart(String path,
            Map<String, String> reqParams, InputStream ins, String fileName)
            throws Exception {
        Charset utf8 = Charset.availableCharsets().get("UTF-8");
        MultipartEntity reqEntity = new MultipartEntity();
        HttpPost httppost = new HttpPost(getRequestBuilder().buildUrl(path));
        if (reqParams != null) {
            for (Map.Entry<String, String> entry : reqParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                reqEntity.addPart(key, new StringBody(value, utf8));
            }
        }
        if (ins != null) {
            ContentBody contentBody = new InputStreamBody(ins, fileName);
            reqEntity.addPart(fileName, contentBody);
        }
        httppost.setEntity(reqEntity);
        HttpResponse response = getRequestExecutor().execute(
            getRequestBuilder().buildOtherRequest(httppost).withCredentials(
                getServerUsername(), getServerPassword())).getResponse();

        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || status >= 300) {
            log.debug("response status = " + status);
            log.debug("output=" + getRequestExecutor().getContent());
            throw new Exception(response.getStatusLine().getReasonPhrase());
        }

        return response;

    }

    /**
     * Send http get request to server.
     */
    private HttpResponse httpGet(String path) throws Exception {
        return getRequestExecutor().execute(
            getRequestBuilder().buildGetRequest(path).withCredentials(
                getServerUsername(), getServerPassword())).getResponse();

    }

    /**
     * create a string of baseString * times
     */
    private String appendString(String baseString, long times) {
        StringBuffer buf = new StringBuffer(baseString);
        for (long i = 1; i < times; i++) {
            buf.append(baseString);
        }
        return buf.toString();
    }

    /**
     * File cutter utility class
     */
    private class FileCutter {

        /**
         * Cut file slice of length size or less starting from offSet. Less in
         * case where offset + size < file.length()
         */

        public byte[] cutFile(File file, long offSet, int size)
                throws IOException {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                fis.skip(offSet);
                byte[] tmp = new byte[size];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int l = fis.read(tmp);
                baos.write(tmp, 0, l);
                return baos.toByteArray();
            } finally {
                fis.close();
            }
        }
    }
}
