package org.apache.sling.performance;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportLogger {

    private static boolean reportFolderLogged = false;
    private static final Logger logger = LoggerFactory.getLogger(ReportLogger.class);

    public static final String REPORTS_DIR = "performance-reports";

    /** Multi map of all ReportLogger instances created by getOrCreate(..) */
    private static final MultiKeyMap reportLoggers = new MultiKeyMap();

    public enum ReportType {
        TXT
    }

    /** Name of test suite */
    private final String testSuiteName;

    /** Name of test case */
    private final String testCaseName;

    /** Class name */
    private final String className;

    /** Name of test method to which all other tests will be compared */
    private final String referenceMethod;

    /** Recorded stats for ran tests */
    private final Map<String, PerformanceRecord> records = new LinkedHashMap<String, PerformanceRecord>();

    /**
     * Do not allow instances to be created directly, use the getOrCreate(..) static method
     */
    private ReportLogger() {
        this.testSuiteName = null;
        this.testCaseName = null;
        this.className = null;
        this.referenceMethod = null;
    }

    /**
     * Create a new ReportLogger, will be called by getOrCreate(..)
     *
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param referenceMethod
     */
    private ReportLogger(final String testSuiteName, final String testCaseName, final String className,
                         final String referenceMethod) {
        this.testSuiteName = testSuiteName;
        this.testCaseName = testCaseName;
        this.className = className;
        this.referenceMethod = referenceMethod;
    }

    /**
     * Factory method for ReportRecorder. Will return an existing ReportLogger for given parameters or create a new
     * instance and register it internally.
     *
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param referenceMethod
     * @return
     */
    public static ReportLogger getOrCreate(final String testSuiteName, final String testCaseName,
                                           final String className, final String referenceMethod) {
        Object reportLogger = reportLoggers.get(testSuiteName, testCaseName, className, referenceMethod);
        if (reportLogger == null) {
            reportLogger = new ReportLogger(testSuiteName, testCaseName, className, referenceMethod);
            reportLoggers.put(testSuiteName, testCaseName, className, referenceMethod, reportLogger);
        }
        return (ReportLogger)reportLogger;
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
        writeReportTxt(testSuiteName,
                testCaseName,
                className,
                methodName,
                statistics.getMin(),
                statistics.getPercentile(10),
                statistics.getPercentile(50),
                statistics.getPercentile(90),
                statistics.getMax(),
                reportLevel);
    }

    /**
     * Method that writes the performance report
     *
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param methodName
     * @param min
     * @param percentile10
     * @param percentile50
     * @param percentile90
     * @param max
     * @param reportLevel
     * @throws Exception
     */
    public static void writeReportTxt(String testSuiteName, String testCaseName, String className, String methodName,
                                      double min, double percentile10, double percentile50, double percentile90, double max,
                                      PerformanceRunner.ReportLevel reportLevel) throws Exception {
        writeReportTxt(testSuiteName, testCaseName, className, methodName,
                min, percentile10, percentile50, percentile90, max,
                reportLevel, false);
    }

    /**
     * Method that writes the performance report
     *
     * @param testSuiteName
     * @param testCaseName
     * @param className
     * @param methodName
     * @param min
     * @param percentile10
     * @param percentile50
     * @param percentile90
     * @param max
     * @param reportLevel
     * @param showDecimals
     * @throws Exception
     */
    public static void writeReportTxt(String testSuiteName, String testCaseName, String className, String methodName,
                                      double min, double percentile10, double percentile50, double percentile90, double max,
                                      PerformanceRunner.ReportLevel reportLevel, boolean showDecimals) throws Exception {
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
            writeReportClassLevel(resultFileName, testSuiteName, min, percentile10, percentile50, percentile90, max);
        } else if (reportLevel.equals(PerformanceRunner.ReportLevel.MethodLevel)) {
            String resultFileName = className + "." + methodName;
            writeReportMethodLevel(resultFileName, testSuiteName, testCaseName, className, methodName,
                    min, percentile10, percentile50, percentile90, max, showDecimals);
        }
    }

    /**
     * Write report for class level tests
     *
     * @param resultFileName the name of the result file (without extension)
     * @param testSuiteName the name of the test suite name
     * @param min
     * @param percentile10
     * @param percentile50
     * @param percentile90
     * @param max

     */
    private static void writeReportClassLevel(String resultFileName, String testSuiteName,
                                              double min, double percentile10, double percentile50, double percentile90, double max) throws IOException {

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
                    min,
                    percentile10,
                    percentile50,
                    percentile90,
                    max);
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
     * @param min
     * @param percentile10
     * @param percentile50
     * @param percentile90
     * @param max

     */
    private static void writeReportMethodLevel(String resultFileName, String testSuiteName,
                                               String testCaseName, String className, String methodName,
                                               double min, double percentile10, double percentile50, double percentile90, double max,
                                               boolean showDecimals) throws IOException {
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
                    showDecimals ?
                            "%-40.40s|%-120.120s|%-80.80s|%-40.40s|%-20.20s|%7.2f|%9.2f|%9.2f|%9.2f|%9.2f%n":
                            "%-40.40s|%-120.120s|%-80.80s|%-40.40s|%-20.20s|%7.0f|%9.0f|%9.0f|%9.0f|%9.0f%n",
                    testSuiteName,
                    (testCaseName.length() < 120) ? (testCaseName) : (testCaseName.substring(0, 115) + "[...]"),
                    className,
                    methodName,
                    getDate(),
                    min,
                    percentile10,
                    percentile50,
                    percentile90,
                    max);
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

    /**
     * Write results from all registered loggers
     *
     * @throws Exception
     */
    public static void writeAllResults() throws Exception {
        for (Object reportLogger : reportLoggers.values()) {
            ((ReportLogger)reportLogger).writeResults();
        }
    }

    /**
     * Check all thresholds for all records in all registered loggers
     *
     * @return
     */
    public static List<Failure> checkAllThresholds() throws ClassNotFoundException {
        List<Failure> failures = new ArrayList<Failure>();
        for (Object reportLogger : reportLoggers.values()) {
            failures.addAll(((ReportLogger) reportLogger).checkThresholds());
        }
        return failures;
    }

    /**
     * Record statistics for given method
     *
     * @param methodName
     * @param statistics
     */
    public void recordStatistics(final String methodName, final DescriptiveStatistics statistics, final double threshold) {
        records.put(methodName, new PerformanceRecord(statistics, threshold));
    }

    /**
     * Write all records to file in TXT format
     *
     * @throws Exception
     */
    public void writeResults() throws Exception {
        PerformanceRecord referenceRecord = records.get(referenceMethod);
        for (String methodName : records.keySet()) {
            DescriptiveStatistics statistics = records.get(methodName).getStatistics();
            double min = statistics.getMin();
            double percentile10 = statistics.getPercentile(10);
            double percentile50 = statistics.getPercentile(50);
            double percentile90 = statistics.getPercentile(90);
            double max = statistics.getMax();
            boolean showDecimals = false;
            if (referenceRecord != null && !referenceMethod.equals(methodName)) {
                DescriptiveStatistics referenceStatistics = referenceRecord.getStatistics();
                double ref = referenceStatistics.getMin();
                min = ref == 0 ? Double.POSITIVE_INFINITY : min/ref;

                ref = referenceStatistics.getPercentile(10);
                percentile10 = ref == 0 ? Double.POSITIVE_INFINITY : percentile10/ref;

                ref = referenceStatistics.getPercentile(50);
                percentile50 = ref == 0 ? Double.POSITIVE_INFINITY : percentile50/ref;

                ref = referenceStatistics.getPercentile(90);
                percentile90 = ref == 0 ? Double.POSITIVE_INFINITY : percentile90/ref;

                ref = referenceStatistics.getMax();
                max = ref == 0 ? Double.POSITIVE_INFINITY : max /referenceStatistics.getMax();

                showDecimals = true;
            }
            ReportLogger.writeReportTxt(testSuiteName,
                    testCaseName,
                    Class.forName(className).getSimpleName(),
                    methodName,
                    min,
                    percentile10,
                    percentile50,
                    percentile90,
                    max,
                    PerformanceRunner.ReportLevel.MethodLevel,
                    showDecimals);
        }
    }

    /**
     * Test if any of the <link>PerformanceRecord</link> exceeds their threshold against the reference
     *
     * @return
     */
    public List<Failure> checkThresholds() throws ClassNotFoundException {
        PerformanceRecord referenceRecord = records.get(referenceMethod);
        if (referenceRecord == null) {
            return Collections.EMPTY_LIST;
        }
        DescriptiveStatistics referenceStatistics = referenceRecord.getStatistics();
        List<Failure> failures = new ArrayList<Failure>();
        for (String methodName : records.keySet()) {
            PerformanceRecord performanceRecord = records.get(methodName);
            String result = performanceRecord.checkThreshold(referenceStatistics);
            if (result != null) {
                failures.add(new Failure(Description.createTestDescription(Class.forName(className), methodName),
                        new Exception(result)));
            }
        }
        return failures;
    }
}