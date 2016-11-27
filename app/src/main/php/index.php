<?php
	include_once 'config.php';
	include_once 'firebase.php';

	$latchContentString = file_get_contents('php://input');
	$latchContent = json_decode($latchContentString,true);
	file_put_contents('./request',print_r($latchContent,true));
	foreach($latchContent['accounts'] as $accid=>$account) {
		foreach($account as $operation) {
			if ($operation['new_status'] == 'off') {
				sendPushNotification($token,$operation['id'],$serverKey);
			}
		}
	}

	if (!empty($_GET['challenge'])) {
		echo $_GET['challenge'];die;
	}
?>
