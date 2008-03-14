<?php

// THIS IS AN EXAMPLE
// you will obviously need to do more server side work than I am doing here to check and move your upload.
// API is up for discussion, jump on http://dojotoolkit.org/forums

// JSON.php is available in dojo svn checkout
require("../../../dojo/tests/resources/JSON.php");
$json = new Services_JSON();

// fake delay
sleep(3);
$name = empty($_REQUEST['name'])? "default" : $_REQUEST['name'];
if(is_array($_FILES)){
	$ar = array(
		'status' => "success",
		'details' => $_FILES[$name]
	);
}else{
	$ar = array(
		'status' => "failed",
		'details' => ""
	);
}

// yeah, seems you have to wrap iframeIO stuff in textareas?
$foo = $json->encode($ar);
?>
<textarea><?php print $foo; ?></textarea>
