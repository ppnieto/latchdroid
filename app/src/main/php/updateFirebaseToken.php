<?php

	file_put_contents('./token',$_POST['token']);
	echo json_encode(['result'=>'ok']);
?>
