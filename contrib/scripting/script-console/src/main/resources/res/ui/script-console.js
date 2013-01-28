//Codemirror editor
var inputEditor
var outputEditor
var OUTPUT_SEP = '\n------------------\n'

function sendData(url, data) {
    $.ajax({
        type:"POST",
        url:url,
        data:data,
//        dataType:"json",
        timeout:30000, //In millis
        beforeSend:function () {
            $('#ajaxSpinner').show();
        },
        /* error: function() {
         $('#status').text('Update failed—try again.').slideDown('slow');
         },*/
        complete:function () {
            $('#ajaxSpinner').hide();
        },
        success:function (data) {
            renderData(data)
        }
    });
}

function renderData(data){
    $('#code-output').show();
    var currentData = outputEditor.getValue()
    //If we need to append the data then use below
    //For now we just replace the existing output
    //var outData = data + OUTPUT_SEP+ currentData

    outputEditor.setValue(data)
}

function setUpCodeMirror() {
    CodeMirror.modeURL = pluginRoot + "/res/ui/codemirror/mode/%N/%N.js";
    inputEditor = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers:true,
        extraKeys: {
            'Ctrl-Q':clearOutput,
            'Ctrl-X':executeScript
        }
    });
    outputEditor = CodeMirror.fromTextArea(document.getElementById("result"), {
            lineNumbers:true,
            readOnly:"nocursor"
        });
}

function updateWithOption(opt){
    setLangMode(inputEditor,opt.attr('langMode'))
    $('[name=lang]').val(opt.val())
}

function setLangMode(editor, modeName) {
    if(!modeName){
        modeName = "text/plain"
    }else{
        CodeMirror.autoLoadMode(inputEditor, modeName);
    }
    editor.setOption("mode", modeName);
}

function setUpLangOptions() {
    var codeLang = $('#codeLang')
    var options = codeLang.prop ? codeLang.prop('options') : codeLang.attr('options');
    codeLang.empty()

    for(var i in scriptConfig){
        var config = scriptConfig[i]
        var opt = new Option(config.langName,config.langCode);
        if(config.mode){
            opt.langMode = config.mode;
        }
        options[options.length] = opt
    }
    $('#codeLang').change(function(){
        var opt = $(this).find(":selected");
        updateWithOption(opt)
    });

    $('#codeLang option:eq(0)').attr('selected','selected')
    updateWithOption($(options[0]))
}

function executeScript(){
    inputEditor.save() //Copy the contents to textarea form field
    sendData(pluginRoot, $("#consoleForm").serialize());
}

function clearOutput(){
    outputEditor.setValue('')
}

$(document).ready(function () {
    $("#executeButton").click(executeScript);
    $('#ajaxSpinner').hide();
    $('#code-output').hide();
    setUpCodeMirror();
    setUpLangOptions();
});
