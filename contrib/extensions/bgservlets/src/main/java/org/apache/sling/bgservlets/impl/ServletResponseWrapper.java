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
package org.apache.sling.bgservlets.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/** Wraps an HttpServletResponse for background processing */
class ServletResponseWrapper extends HttpServletResponseWrapper {

	private final String outputPath;
	private final ServletOutputStream stream;
	private final PrintWriter writer;
	
	static class CustomOutputStream extends ServletOutputStream {

		private final OutputStream os;
		
		CustomOutputStream(OutputStream os) {
			this.os = os;
		}
		
		@Override
		public void write(int b) throws IOException {
			os.write(b);
		}

		@Override
		public void close() throws IOException {
			os.close();
		}

		@Override
		public void flush() throws IOException {
			os.flush();
		}
		
	}
	
	ServletResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);
		// TODO write output to the Sling repository. For now: just a temp file
		final File output = File.createTempFile(getClass().getSimpleName(), ".data");
		output.deleteOnExit();
		outputPath = output.getAbsolutePath();
		stream = new CustomOutputStream(new FileOutputStream(output));
		writer = new PrintWriter(new OutputStreamWriter(stream));
	}
	
	public String toString() {
		return getClass().getName() + ":" + outputPath;
	}
	
	String getOutputPath() {
		return outputPath;
	}
	
	void cleanup() throws IOException {
		stream.flush();
		stream.close();
	}
	
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return stream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return writer;
	}

}