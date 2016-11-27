<?php

	function sendPushNotification($token, $message, $serverKey) {
	        $path_to_firebase_cm = 'https://fcm.googleapis.com/fcm/send';

	        $fields = array(
	            'to' => $token,
	            'data' => array('message' => $message)
	        );

	        $headers = array(
	            "Authorization:key=$serverKey",
	            'Content-Type:application/json',
		    'Content-length: ' . strlen(json_encode($fields))
	        );
		file_put_contents('./push',print_r($headers,true) . print_r($fields,true));
                $ch = curl_init();

	        curl_setopt($ch, CURLOPT_URL, $path_to_firebase_cm);
	        curl_setopt($ch, CURLOPT_POST, true);
	        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
	        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
	        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
	        curl_setopt($ch, CURLOPT_IPRESOLVE, CURL_IPRESOLVE_V4 );
	        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
	        $result = curl_exec($ch);
	        curl_close($ch);
		file_put_contents('./push_result',$result);
	}

?>
