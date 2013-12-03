/**
 * Removes the editor (toggles all displayfields/editables).
 */
function removeEditor(row) {
    $(row).find(".loggers").toggle();
	$(row).find(".logLevels").toggle();
	$(row).find(".logFile").toggle();
	$(row).find(".configureLink").toggle();
	$(row).find(".editElement").remove();
	$(row).removeClass("currentEditor");
}

/**
 * Turns the loglevel element into an selectfield (current loglevel is selected).
 */
function addLogLevelSelect(row) {
    var logLevelElement = $(row).find(".logLevels");
    // get the current loglevel
	var currentLogLevel = logLevelElement.attr("data-currentloglevel");
	if(!currentLogLevel) {
	    // convenience default for new loggers
	    currentLogLevel = "INFO";
	}
	// get all available loglevels (present in the "newlogger" element)
	var allLogLevels = $("#allLogLevels").attr("data-loglevels").split(",");
	var select = $('<select class="editElement" name="loglevel"></select>');
	$.each(allLogLevels, function(index, logLevel) {
		select.append('<option'+(logLevel == currentLogLevel ? ' selected="selected"' : '')+'>'+logLevel+'</option>');
    });
	logLevelElement.after(select);
	logLevelElement.toggle();
}

/**
 * Adds a new editable logger for the given loggerelement (with controls for adding/removing).
 * @param loggersElement logger element
 * @param loggerName name of the logger
 */
function addLogger(loggersElement, loggerName) {
    var addButton = $('<input type="submit" name="add" class="ui-state-default ui-corner-all" value="+" style="width:5%;" />');
    addButton.bind("click", function() {
    	addLogger($(this).parent(), "");
    	return false;
    });
	var removeButton = $('<input type="submit" class="ui-state-default ui-corner-all" name="remove" value="-" style="width:5%;" />');
	removeButton.bind("click", function() {
		$(this).parent().remove();
		return false;
	});
	var loggerField = $('<input type="text" name="logger" class="loggerField ui-state-default ui-corner-all inputText" value="'+loggerName+'" autocomplete="off" style="width:89%;" />');
	// add the autocomplete with the array of all loggers
	loggerField.autocomplete({
        data: loggers
    });
	var logger = $('<div class="editElement"></div>').append(loggerField, addButton, removeButton);
	loggersElement.after(logger);
}

/**
 * Turns the logger elements into inputfields (with controls).
 */
function addLoggers(row) {
    var loggersElement = $(row).find(".loggers");
	var loggers = loggersElement.find(".logger");
	if(loggers.length == 0) {
	    addLogger(loggersElement, "");	
	}
	$.each(loggers, function(index, logger) {
	      addLogger(loggersElement, $(logger).html());		  
	});
	loggersElement.toggle();
}

/**
 * Turns the logfile element into an inputfield.
 */
function addLogFile(row) {
    var logFileElement = $(row).find(".logFile");
    var logFile = "";
    if(logFileElement.length > 0) {
        logFile = $(logFileElement).html();
    }
    if (logFile.length == 0) {
    	// no logfile -> new logger -> take default
    	logFile = $("#defaultLogfile").attr("data-defaultlogfile");
    }
    logFileElement.after('<input style="width:100%" class="editElement ui-state-default ui-corner-all inputText" type="text" name="logfile" value="'+logFile+'" />');
    logFileElement.toggle();
}

/**
 * Activates the logger configurator (called by clicking the configure link).
 * Turns all display fields in the logger row containing the configure link into edit fields.
 * @param button configure link
 */
function configureLogger(button) {
	var configureLink = $(button.currentTarget);
	var row = configureLink.parent().parent();
	var rowId = $(row).attr("id");
	// remove the current editor, since we have only one form only one editor can be active the same time
	removeEditor($(".currentEditor"));
	// add class as marker (id is already used for pid)
	row.addClass("currentEditor");
	// add the editables
    addLogLevelSelect(row);
	addLoggers(row);
    addLogFile(row);
    // add controls
    var hiddenField = $('<input class="editElement" type="hidden" name="pid" value="'+(rowId != 'newlogger' ? rowId : '')+'" />');
    var saveButton = $('<input class="editElement" type="submit" name="save" value="Save" />');
    var cancelButton=$('<input class="editElement" type="submit" value="Cancel" />');
	cancelButton.bind("click", function() {
	    var row = $(this).parent().parent();
	    removeEditor(row);
	    return false;
	});
    var deleteButton = $('<input class="editElement" type="submit" name="delete" value="Remove Logger" />');
    configureLink.after(saveButton, cancelButton, hiddenField);
    if (rowId !== "newlogger") {
    	// add a delete buttons for existing loggers
    	cancelButton.after(deleteButton);
    }
    configureLink.toggle();
	// prevent click on link
    return false;
}

/**
 * Initializes the log panel.
 */
function initializeSlingLogPanel() {
	$("#loggerConfig").find(".configureLink").bind("click", configureLogger);
}
