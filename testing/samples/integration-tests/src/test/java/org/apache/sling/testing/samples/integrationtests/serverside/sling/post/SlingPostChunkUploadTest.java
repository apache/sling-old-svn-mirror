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
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class SlingPostChunkUploadTest extends TestCase {

    /**
     * Jcr property name indicating total continuous bytes stored in repository.
     */
    public static final String BYTES_UPLOADED = "bytesUploaded";

    /**
     * Jcr property name indicating total continuous chunks stored in repository
     */
    public static final String CHUNKS_UPLOADED = "chunksUploaded";

    private String poolTimeOutMillSecStr = System.getProperty("CONN_TIME_OUT_MILLSEC", "60000");

    private String maxTotalConnStr = System.getProperty("MAX_TOTAL_CONN", "400");

    private String maxConnPerRouteStr = System.getProperty("MAX_CONN_PER_ROUTE", "200");

    public String baseUrl = System.getProperty("baseUrl", "http://localhost:4502");

    private HttpClient httpclient;

    private String userName;

    private String password;

    private String hostName;

    private String port = "";

    private String scheme;

    FileCutter fileCutter;

    String parentPath = "/content/dam";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        int poolTimeOutMillSec = Integer.parseInt(poolTimeOutMillSecStr);
        int maxTotalConn = Integer.parseInt(maxTotalConnStr);
        int maxConnPerRoute = Integer.parseInt(maxConnPerRouteStr);
        initialize(baseUrl, poolTimeOutMillSec, maxTotalConn, maxConnPerRoute);
        userName = "admin";
        password = "admin";
        fileCutter = new FileCutter();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        this.destroy();
    }

    /**
     * Test chunk upload without interruption.
     */
    @Test
    public void testChunkUpload() {
        OutputStream os = null;
        InputStream fis = null;
        InputStream respStream = null;
        File file = null;
        try {
            file = File.createTempFile("test", "chunkupload");
            String data = appendString("helloworld", 100);
            os = new FileOutputStream(file);
            IOUtils.write(data, os);
            os.close();
            if (!file.exists()) {
                throw new Exception(file.getAbsolutePath() + "  not found");
            }
            int chunkSize = 400;
            String uploadId = uploadFirstPart(parentPath, file, chunkSize);
            //
            uploadChunks(parentPath, uploadId, file, chunkSize, 2, Integer.MAX_VALUE, chunkSize);

            // retrieve the stream on get and validate its content with uploaded
            // file
            HttpResponse response = httpGet(parentPath + "/" + file.getName());
            respStream = response.getEntity().getContent();
            fis = new FileInputStream(file);
            assertEquals("content stream doesn't match", true, IOUtils.contentEquals(fis, respStream));

            response = httpGet(uploadId);
            // uploaded ended. Get request on upload should return 404
            assertEquals("status should be 404 not found ", 404, response.getStatusLine().getStatusCode());

            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(":operation", "delete");
            response = uploadMultiPart(parentPath + "/" + file.getName(), reqParams, null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + file.getName());
            assertEquals("status should be 404 not found ", 404, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("exception caught");
        } finally {
            try {
                os.close();
            } catch (Exception ignore) {
            }
            try {
                fis.close();
            } catch (Exception ignore) {
            }
            try {
                respStream.close();
            } catch (Exception ignore) {
            }
            file.delete();
        }
    }

    /**
     * Test chunk upload after interruption. test the use of variable chunk size. After interruption, client retrieves chunk upload
     * information and resume upload with variable chunk size.
     */
    @Test
    public void testInterruptedChunkUpload() {
        OutputStream os = null;
        InputStream fis = null;
        InputStream respStream = null;
        File file = null;
        try {
            file = File.createTempFile("test", "chunkupload");
            // create 1700 bytes file
            String data = appendString("helloworld", 170);
            os = new FileOutputStream(file);
            IOUtils.write(data, os);
            os.close();
            if (!file.exists()) {
                throw new Exception(file.getAbsolutePath() + "  not found");
            }
            int chunkSize = 200;
            // uplaod first chunk 200 bytes uploaded
            String uploadId = uploadFirstPart(parentPath, file, chunkSize);
            JSONObject json = getChunkJson(uploadId);
            validate(json, 200, 1);

            chunkSize = 300;
            // upload next two chunks of 200 each.total 600 bytes and 3 chunks
            // uploaded
            int retChunkNumber = uploadChunks(parentPath, uploadId, file, json.getInt(BYTES_UPLOADED), json.getInt(CHUNKS_UPLOADED) + 1,
                json.getInt(CHUNKS_UPLOADED) + 2, chunkSize);
            json = getChunkJson(uploadId);
            validate(json, 800, 3);

            chunkSize = 400;
            // upload two chunk of 400 each. total 1400 bytes and 5 chunks
            // uploaded
            retChunkNumber = uploadChunks(parentPath, uploadId, file, json.getInt(BYTES_UPLOADED), json.getInt(CHUNKS_UPLOADED) + 1,
                json.getInt(CHUNKS_UPLOADED) + 2, chunkSize);
            json = getChunkJson(uploadId);
            validate(json, 1600, 5);

            chunkSize = 500;
            retChunkNumber = uploadChunks(parentPath, uploadId, file, json.getInt(BYTES_UPLOADED), json.getInt(CHUNKS_UPLOADED) + 1,
                Integer.MAX_VALUE, chunkSize);

            HttpResponse response = httpGet(uploadId);
            // uploaded ended. Get on uplaod should return 404
            assertEquals("status should be 404 not found ", 404, response.getStatusLine().getStatusCode());

            response = httpGet(parentPath + "/" + file.getName());
            respStream = response.getEntity().getContent();
            fis = new FileInputStream(file);
            assertEquals("content stream doesn't match", true, IOUtils.contentEquals(fis, respStream));

            // clean uploaded file from repository
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(":operation", "delete");
            response = uploadMultiPart(parentPath + "/" + file.getName(), reqParams, null, null);

            // status should be 404
            response = httpGet(parentPath + "/" + file.getName());
            assertEquals("status should be 404 not found ", 404, response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("exception caught");
        } finally {
            try {
                os.close();
            } catch (Exception ignore) {
            }
            try {
                fis.close();
            } catch (Exception ignore) {
            }
            try {
                respStream.close();
            } catch (Exception ignore) {
            }
            file.delete();
        }
    }

    /**
     * To query chunk upload in json
     *
     */
    private JSONObject getChunkJson(String uploadId) throws Exception {
        InputStream respStream = null;
        JSONObject json = null;
        try {
            HttpResponse response = httpGet(uploadId + ".json");
            respStream = response.getEntity().getContent();
            json = new JSONObject(IOUtils.toString(respStream));
        } finally {
            try {
                respStream.close();
            } catch (Exception ignore) {
            }
        }
        return json;
    }

    private void validate(JSONObject json, int bytesUploaded, int chunksUploaded) throws Exception {
        assertEquals("bytesUploaded didn't match", bytesUploaded, json.getInt(BYTES_UPLOADED));
        assertEquals("chunksuploaded didn't match", chunksUploaded, json.getInt(CHUNKS_UPLOADED));
    }

    /**
     * upload first chunk to server.
     *
     */
    private String uploadFirstPart(String path, File file, int chunkSize) throws Exception {
        HttpResponse resp = uploadPart(path, null, file, 0, chunkSize, 1);
        InputStream respStream = resp.getEntity().getContent();
        String html = IOUtils.toString(respStream);
        respStream.close();
        return getLocationFromHtml(html);

    }

    /**
     * upload chunks starting from offseet with size equals to chunkSize from startChunkNumber to endChunkNumber both inclusive or till end
     * of file is reached.
     */

    private int uploadChunks(String path, String uploadId, File file, int offSet, int startChunkNumber, int endChunkNumber, int chunkSize)
            throws Exception {
        int length = new Long(file.length()).intValue();
        int chunkNumber = startChunkNumber;
        while (offSet < length && chunkNumber <= endChunkNumber) {
            if (offSet + chunkSize >= length) {
                chunkSize = length - offSet;
            }
            uploadPart(path, uploadId, file, offSet, chunkSize, chunkNumber);
            offSet += chunkSize;
            chunkNumber++;
        }
        return chunkNumber;
    }

    /**
     * upload single chunk starting from offset of size chunkSize and chunkNumber.
     */
    private HttpResponse uploadPart(String path, String uploadId, File file, int offSet, int chunkSize, int chunkNumber) throws Exception {
        byte[] buf = fileCutter.cutFile(file, offSet, chunkSize);
        System.out.println(Thread.currentThread().getName() + ": uploading bytes from " + offSet + " to " + (offSet + chunkSize - 1));
        Map<String, String> headers = new HashMap<String, String>();
        ByteArrayInputStream instream = new ByteArrayInputStream(buf);
        Map<String, String> reqParams = new HashMap<String, String>();
        if (uploadId != null) {
            reqParams.put(":chunkUploadId", uploadId);
        }
        reqParams.put(":chunkNumber", String.valueOf(chunkNumber));

        if ((offSet + chunkSize) >= file.length()) {
            reqParams.put(":lastChunk", String.valueOf(true));
        }
        return uploadMultiPart(path, reqParams, instream, file.getName());

    }

    /**
     * send multipart post request to server.
     */

    private HttpResponse uploadMultiPart(String path, Map<String, String> reqParams, InputStream ins, String fileName) throws Exception {
        HttpHost target = new HttpHost(hostName, Integer.parseInt(port), scheme);
        Charset utf8 = Charset.availableCharsets().get("UTF-8");
        MultipartEntity reqEntity = new MultipartEntity();
        HttpPost httppost = new HttpPost(path);
        if (reqParams != null) {
            for (Map.Entry<String, String> entry : reqParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                reqEntity.addPart(key, new StringBody(value, utf8));
            }
        }
        if (ins != null) {
            ContentBody contentBody = new InputStreamBody(ins, fileName);
            reqEntity.addPart("*", contentBody);
        }
        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(userName, password);
        httppost.addHeader(new BasicScheme().authenticate(cred, httppost));
        httppost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(target, httppost);
        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || status >= 300) {
            System.out.println("output=" + IOUtils.toString(response.getEntity().getContent()));
            fail("Not valid response status code:" + status);
        }

        return response;

    }

    /**
     * Send http get request to server.
     */
    private HttpResponse httpGet(String path) throws Exception {
        HttpHost target = new HttpHost(hostName, Integer.parseInt(port), scheme);
        HttpGet httpget = new HttpGet(path);
        UsernamePasswordCredentials cred = new UsernamePasswordCredentials(userName, password);
        httpget.addHeader(new BasicScheme().authenticate(cred, httpget));
        HttpResponse response = httpclient.execute(target, httpget);

        return response;

    }

    /**
     * retrieve location tag from html.
     */
    private String getLocationFromHtml(String html) {

        int startIndex = html.indexOf("<td>Location</td>") + "<td>Location</td>\n<td><a href=\"".length();
        int endIndex = html.indexOf("\" id=\"Location\"", startIndex);
        String pString = html.substring(startIndex, endIndex);
        startIndex = pString.indexOf("<td><a href=\"");
        return pString.substring(startIndex + "<td><a href=\"".length());
    }

    /**
     * create a string of baseString * times
     *
     */
    private String appendString(String baseString, int times) {
        StringBuffer buf = new StringBuffer(baseString);
        for (int i = 1; i < times; i++) {
            buf.append(baseString);
        }
        return buf.toString();
    }

    /**
     * initialize httpclient
     *
     */
    protected void initialize(String baseUrl, int poolTimeOutMillSec, int maxTotalConn, int maxConnPerRoute) {
        try {
            destroy();
            intializeHttpClientInternal(poolTimeOutMillSec, maxTotalConn, maxConnPerRoute);
            configureHttpClient(baseUrl);
        } catch (Exception ignore) {
        }
    }

    /**
     * destroy httpclient. shutsdown connection manager
     */
    protected void destroy() {
        try {
            if (httpclient != null) httpclient.getConnectionManager().shutdown();
        } catch (Exception ignore) {
        }
    }

    protected void finalize() throws Throwable {
        try {
            httpclient.getConnectionManager().shutdown();
        } catch (Exception ignore) {
        }
    }

    private void intializeHttpClientInternal(int poolTimeOutMillSec, int maxTotalConn, int maxConnPerRoute) {
        ThreadSafeClientConnManager connman = new ThreadSafeClientConnManager();
        connman.setMaxTotal(maxTotalConn);
        connman.setDefaultMaxPerRoute(maxConnPerRoute);
        httpclient = new DefaultHttpClient(connman);
        httpclient.getParams().setParameter("http.connection-manager.timeout", poolTimeOutMillSec);
    }

    private void configureHttpClient(String baseUrl) throws Exception {
        Map<String, String> urlMap = new HashMap<String, String>(5);
        String[] tokens = baseUrl.split("/");
        if (tokens.length < 3) {
            throw new Exception("invalid author url:" + baseUrl);
        } else {
            scheme = tokens[0].substring(0, tokens[0].lastIndexOf(":"));
            String hostNameAndPort = tokens[2];
            if (hostNameAndPort.startsWith("[")) {
                // IPv6 address
                int lastIndex = hostNameAndPort.lastIndexOf("]");
                if (lastIndex < 0)
                    throw new Exception("invalid author url:" + baseUrl);
                else {
                    hostName = hostNameAndPort.substring(0, lastIndex + 1);

                    if (lastIndex + 2 <= hostNameAndPort.length()) port = hostNameAndPort.substring(lastIndex + 2);
                    if (port == null || "".equals(port)) {
                        port = "-1";
                    }
                }
            } else {
                String[] addressTokens = hostNameAndPort.split(":");
                hostName = addressTokens[0];
                if (addressTokens.length > 1) port = addressTokens[1];
                if (port == null || "".equals(port)) {
                    port = "-1";
                }
            }
        }
        String contextpath = "";
        if (tokens.length > 3) {
            for (int i = 3; i < tokens.length; i++) {
                contextpath = contextpath + "/" + tokens[i];
            }
        }
    }

    /**
     * File cutter utility class
     */
    private class FileCutter {

        /**
         * Cut file slice of length size or less starting from offSet. Less in case where offset + size < file.length()
         *
         */

        public byte[] cutFile(File file, long offSet, int size) throws IOException {
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
