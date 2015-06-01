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

package org.apache.sling.commons.contentdetection.internal;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TikaInputStream.class)
public class ContentAwareMimeTypeServiceImplTest extends EasyMockSupport {

    private Detector mockDetector = EasyMock.createMock(Detector.class);

    private MimeTypeService mockMimeTypeService = EasyMock.createMock(MimeTypeService.class);

    @TestSubject
    private ContentAwareMimeTypeServiceImpl contentAwareMimeTypeService = new ContentAwareMimeTypeServiceImpl();

    @Before
    public void setUp() throws IOException {
        contentAwareMimeTypeService.detector = mockDetector;
        contentAwareMimeTypeService.mimeTypeService = mockMimeTypeService;
    }

    @Test
    public void testGetMimeType() throws IOException {

        //Initializations
        String filename = "test.txt";
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, filename);
        byte[] byteArray = new byte[]{};
        InputStream content = new ByteArrayInputStream(byteArray);
        TikaInputStream stream = TikaInputStream.get(content);
        PowerMock.mockStatic(TikaInputStream.class);

        //Record Expectations
        EasyMock.expect(TikaInputStream.get(content)).andReturn(stream).once();
        EasyMock.expect(mockDetector.detect(stream, metadata)).andReturn(MediaType.TEXT_PLAIN).once();

        //Getting ready for run
        PowerMock.replay(TikaInputStream.class);
        EasyMock.replay(mockDetector);

        //The test run
        String mimeType = contentAwareMimeTypeService.getMimeType(filename, content);

        //Verification of expectations
        Assert.assertEquals(MediaType.TEXT_PLAIN.toString(), mimeType);
        EasyMock.verify(mockDetector);
        PowerMock.verify(TikaInputStream.class);
    }

    @Test
    public void testGetMimeTypeByString(){
        String mimeTypeName = "testName";
        String mimetype = "mimeType";

        EasyMock.expect(mockMimeTypeService.getMimeType(mimeTypeName)).andReturn(mimetype);
        EasyMock.replay(mockMimeTypeService);

        String another = contentAwareMimeTypeService.getMimeType(mimeTypeName);
        Assert.assertEquals(mimetype, another);
        EasyMock.verify(mockMimeTypeService);
    }

    @Test
    public void testGetExtension(){
        String mimeTypeName = "testName";
        String extension = "java";

        EasyMock.expect(mockMimeTypeService.getExtension(mimeTypeName)).andReturn(extension);
        EasyMock.replay(mockMimeTypeService);

        String another = contentAwareMimeTypeService.getExtension(mimeTypeName);
        Assert.assertEquals(extension, another);
        EasyMock.verify(mockMimeTypeService);
    }

    @Test
    public void testGetMimeTypeWithNullContent() throws IOException {
        //Initializations
        String filename = "test.txt";
        InputStream content = null;

        //Record Expectations
        EasyMock.expect(mockMimeTypeService.getMimeType(filename)).andReturn(MediaType.TEXT_PLAIN.getType()).once();

        //Getting ready for run
        EasyMock.replay(mockMimeTypeService);

        //The test run
        String mimeType = contentAwareMimeTypeService.getMimeType(filename, content);

        //Verification of expectations
        Assert.assertEquals(MediaType.TEXT_PLAIN.getType(), mimeType);
        EasyMock.verify(mockMimeTypeService);
    }

    @Test
    public void testRegisterNewMymeType() {
        final String mimeTypeName = "test";
        final String[] mimeTypeExtensions = new String[]{"pict", "apt", "z"};

        /* The only thing ContentAwareMimeTypeServiceImpl#registerMimeType(String name, String... ext)
         * method does is calls MimeTypeService registerMimeType(String name, String[] ext) method.
         * So we Mock it and expect that it will be called.
         */
        mockMimeTypeService.registerMimeType(mimeTypeName, mimeTypeExtensions);
        EasyMock.expectLastCall();
        EasyMock.replay(mockMimeTypeService);

        contentAwareMimeTypeService.registerMimeType(mimeTypeName, mimeTypeExtensions);

        EasyMock.verify(mockMimeTypeService);
    }

    @Test
    public void testRegisterMimeType() throws IOException {
        byte[] byteArray = new byte[]{};
        InputStream content = new ByteArrayInputStream(byteArray);

        /* The only thing ContentAwareMimeTypeServiceImpl#registerMimeType(InputStream i)
         * method does is calls MimeTypeService#registerMimeType(InputStream i) method.
         * So we Mock it and expect that it will be called.
         */
        mockMimeTypeService.registerMimeType(content);
        EasyMock.expectLastCall();
        EasyMock.replay(mockMimeTypeService);

        contentAwareMimeTypeService.registerMimeType(content);

        EasyMock.verify(mockMimeTypeService);
    }

    @After
    public void tearDown() {
    }
}