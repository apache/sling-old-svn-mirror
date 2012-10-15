package org.apache.sling.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class ReportLogger {

	public enum ReportType {
		TXT
	}

	public static void writeReport(String test, String testSuiteName,
			String name, DescriptiveStatistics statistics, ReportType reportType, PerformanceRunner.ReportLevel reportlevel)
			throws Exception {
		switch (reportType) {
		case TXT:
			writeReportTxt(test, testSuiteName, name, statistics, reportlevel);
			break;
		default:
			throw new Exception(
					"The specified reporting format is not yet supported");
		}
	}

	/**
	 * Method the writes the performance report after a test is run
	 * 
	 * @param test
	 *            the test name
	 * @param name
	 *            the name that will be listed in the report
	 * @param statistics
	 *            the statistics data to be written
	 * @throws IOException
	 */
	public static void writeReportTxt(String test, String testSuiteName, String name, DescriptiveStatistics statistics, PerformanceRunner.ReportLevel reportlevel)
			throws Exception{

		String className = test;
		className = className.substring(className.lastIndexOf(".") + 1);

		File reportDir = new File("target/performance-reports");
		if (!reportDir.exists()) {
			if (!reportDir.mkdir())
				throw new IOException("Unable to create performance-reports directory");	
		}

		// need this in the case a user wants to set the suite name from the
		// command line
		// useful if we run the test cases from the command line for example
		// by using maven
		if (testSuiteName.equals(ParameterizedTestList.TEST_CASE_ONLY)) {
			if (System.getProperty("testsuitename") != null) {
				testSuiteName = System.getProperty("testsuitename");
			}
		}

		String resultFileName = className;
		if (reportlevel.equals(PerformanceRunner.ReportLevel.ClassLevel)){
			writeReportClassLevel(resultFileName, testSuiteName, statistics);
			}else if (reportlevel.equals(PerformanceRunner.ReportLevel.MethodLevel)){
				resultFileName = test + "." + name;
				writeReportMethodLevel(resultFileName, testSuiteName, statistics);
			}
	}
	
	/**
     * Write report for class level tests
     * @param resultFileName the name of the result file (without extension)
     * @param testSuiteName the name of the test suite name
     * @param statistics the statistics object used to compute different medians
     * @throws IOException
     */
    private static void writeReportClassLevel(String resultFileName, String testSuiteName, DescriptiveStatistics statistics)
    		throws IOException{
    	
    	File report = new File("target/performance-reports", resultFileName + ".txt");
		boolean needsPrefix = !report.exists();
	    PrintWriter writer = new PrintWriter(
	    		new FileWriterWithEncoding(report, "UTF-8", true));
	    try {
	    	if (needsPrefix) {
	    		writer.format(
	    				"# %-34.34s     min     10%%     50%%     90%%     max%n",
	    				resultFileName);
	    	}
	    	
	    	writer.format(
	    			"%-36.36s  %6.0f  %6.0f  %6.0f  %6.0f  %6.0f%n",
	    			testSuiteName,
	    			statistics.getMin(),
	    			statistics.getPercentile(10.0),
	    			statistics.getPercentile(50.0),
	    			statistics.getPercentile(90.0),
	    			statistics.getMax());
	    	} finally {
	    		writer.close();
	   		}
	}
    
    /**
     * Write report for method level tests
     * @param resultFileName the name of the result file (without extension)
     * @param testSuiteName the name of the test suite name
     * @param statistics the statistics object used to compute different medians
     * @throws IOException
     */
    private static void writeReportMethodLevel(String resultFileName, String testSuiteName, DescriptiveStatistics statistics)
    	    throws IOException{
    	File report = new File("target/performance-reports", resultFileName + ".txt");
	
    	String className = resultFileName.substring(0, resultFileName.lastIndexOf(".")); 
    	String methodName = resultFileName.substring(resultFileName.lastIndexOf(".") + 1); 
    	
    	boolean needsPrefix = !report.exists();
    	PrintWriter writer = new PrintWriter(
    			new FileWriterWithEncoding(report, "UTF-8", true));
    	try {
    		if (needsPrefix) {
    			writer.format(
    					"%-40.40s|%-80.80s|%-40.40s|      DateTime      |  min  |   10%%   |   50%%   |   90%%   |   max%n",
    					"Test Suite",
    					"Test Class",
    					"Test Method");
    		}
    		
    		writer.format(
    				"%-40.40s|%-80.80s|%-40.40s|%-20.20s|%7.0f|%9.0f|%9.0f|%9.0f|%9.0f%n",
    				testSuiteName,
    				className,
    				methodName,
    				getDate(),
    				statistics.getMin(),
    				statistics.getPercentile(10.0),
    				statistics.getPercentile(50.0),
    				statistics.getPercentile(90.0),
    				statistics.getMax());
    		} finally {
    			writer.close();
    		}
    }
	    
	
	

	/**
	 * Get the date that will be written into the result file
	 * 
	 * @return
	 */
	private static String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		return dateFormat.format(date);
	}

}
