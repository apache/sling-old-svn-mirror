package org.apache.sling.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportLogger {

    private static boolean reportFolderLogged = false;
    private static final Logger logger = LoggerFactory.getLogger(ReportLogger.class);
    
    public static final String REPORTS_DIR = "performance-reports";

	public enum ReportType {
		TXT
	}

    /**
     * Method the writes the performance report after a test is run
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param methodName
     * @param statistics
     * @param reportType
     * @param reportLevel
     * @throws Exception
     */
    public static void writeReport(String testSuiteName, String testCaseName, String className, String methodName,
            DescriptiveStatistics statistics, ReportType reportType, PerformanceRunner.ReportLevel reportLevel) throws Exception {
		switch (reportType) {
            case TXT:
                writeReportTxt(testSuiteName, testCaseName, className, methodName, statistics, reportLevel);
                break;
            default:
                throw new Exception("The specified reporting format is not yet supported");
		}
	}

	/**
     * Method the writes the performance report after a test is run, in text format
	 * 
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param methodName
	 * @param statistics
     * @param reportLevel
     * @throws Exception
	 */
    public static void writeReportTxt(String testSuiteName, String testCaseName, String className, String methodName,
            DescriptiveStatistics statistics, PerformanceRunner.ReportLevel reportLevel) throws Exception {

        File reportDir = new File("target/" + REPORTS_DIR);
		if (!reportDir.exists() && !reportDir.mkdir()) {
                throw new IOException("Unable to create " + REPORTS_DIR + " directory");
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

		if (reportLevel.equals(PerformanceRunner.ReportLevel.ClassLevel)) {
            String resultFileName = className;
            writeReportClassLevel(resultFileName, testSuiteName, statistics);
        } else if (reportLevel.equals(PerformanceRunner.ReportLevel.MethodLevel)) {
            String resultFileName = className + "." + methodName;
            writeReportMethodLevel(resultFileName, testSuiteName, testCaseName, className, methodName, statistics);
        }
	}
	
	/**
     * Write report for class level tests
     *
     * @param resultFileName the name of the result file (without extension)
     * @param testSuiteName the name of the test suite name
     * @param statistics the statistics object used to compute different medians
     */
    private static void writeReportClassLevel(String resultFileName, String testSuiteName,
            DescriptiveStatistics statistics) throws IOException {
    	
        File report = getReportFile(resultFileName, ".txt");
		boolean needsPrefix = !report.exists();
	    PrintWriter writer = new PrintWriter(
	    		new FileWriterWithEncoding(report, "UTF-8", true));
	    try {
	    	if (needsPrefix) {
	    		writer.format("# %-50.50s     min     10%%     50%%     90%%     max%n", resultFileName);
	    	}
	    	
	    	writer.format(
	    			"%-52.52s  %6.0f  %6.0f  %6.0f  %6.0f  %6.0f%n",
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
     *
     * @param resultFileName the name of the result file (without extension)
     * @param testSuiteName the name of the test suite name
     * @param testCaseName
     * @param className
     * @param methodName
     * @param statistics the statistics object used to compute different medians
     */
    private static void writeReportMethodLevel(String resultFileName, String testSuiteName, String testCaseName, String className,
            String methodName, DescriptiveStatistics statistics) throws IOException {
        File report = getReportFile(resultFileName, ".txt");
	
    	boolean needsPrefix = !report.exists();
    	PrintWriter writer = new PrintWriter(
    			new FileWriterWithEncoding(report, "UTF-8", true));
    	try {
    		if (needsPrefix) {
    			writer.format(
                        "%-40.40s|%-120.120s|%-80.80s|%-40.40s|      DateTime      |  min  |   10%%   |   50%%   |   90%%   |   max%n",
    					"Test Suite",
                        "Test Case",
    					"Test Class",
    					"Test Method");
    		}
    		
    		writer.format(
                    "%-40.40s|%-120.120s|%-80.80s|%-40.40s|%-20.20s|%7.0f|%9.0f|%9.0f|%9.0f|%9.0f%n",
    				testSuiteName,
                    (testCaseName.length() < 120) ? (testCaseName) : (testCaseName.substring(0, 115) + "[...]"),
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
	 */
	private static String getDate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		return dateFormat.format(date);
	}
	
	private static File getReportFile(String resultFileName, String extension) {
	    final String folder = "target/" + REPORTS_DIR;
	    final String filename =  resultFileName + extension;
	    if(!reportFolderLogged) {
	        logger.info("Writing performance test results under {}", folder);
	        reportFolderLogged = true;
	    }
	    return new File(folder, filename);
	}

}