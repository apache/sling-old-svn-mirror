new File(basedir, '.').eachFileRecurse(groovy.io.FileType.FILES) { logFile ->
	if ( logFile.name == "build.log" ) {
		logFile.eachLine { line ->
			if ( line.contains("WARNING") ) {
				throw new RuntimeException("Warning found in line ${line}\nIn file ${logFile}");
			}
		}
	}
}