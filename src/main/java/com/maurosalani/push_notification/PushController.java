package com.maurosalani.push_notification;

import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionEndpoint;

@RestController
public class PushController {

	private final ServerKeys serverKeys;

	private final SubscriptionsHandler subscriptionsHandler = SubscriptionsHandler.getInstance();

	public PushController(ServerKeys serverKeys, CryptoService cryptoService, ObjectMapper objectMapper) {
		this.serverKeys = serverKeys;
	}

	@GetMapping(path = "/publicSigningKey", produces = "application/octet-stream")
	public byte[] publicSigningKey() {
		return this.serverKeys.getPublicKeyUncompressed();
	}

	@GetMapping(path = "/publicSigningKeyBase64")
	public String publicSigningKeyBase64() {
		return this.serverKeys.getPublicKeyBase64();
	}

	@PostMapping("/subscribe")
	@ResponseStatus(HttpStatus.CREATED)
	public void subscribe(@RequestBody Subscription subscription) {
		Logger.getLogger(PushController.class.getName())
				.info("Username: " + subscription.getUsername() + "just subscribed: " + subscription.getEndpoint());
		subscriptionsHandler.subscribeUser(subscription);
	}

	@PostMapping("/unsubscribe")
	public void unsubscribe(@RequestBody SubscriptionEndpoint subscription) {
		Logger.getLogger(PushController.class.getName()).info("Unsubscription: " + subscription.getEndpoint());
		subscriptionsHandler.unsubscribeUser(subscription);
	}

	@PostMapping("/isSubscribed")
	public boolean isSubscribed(@RequestBody SubscriptionEndpoint subscription) {
		return subscriptionsHandler.isSubscribed(subscription);
	}

}
