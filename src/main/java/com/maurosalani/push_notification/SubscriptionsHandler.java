package com.maurosalani.push_notification;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maurosalani.push_notification.dto.PushMessage;
import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionEndpoint;
import com.maurosalani.push_notification.repository.RedisRepository;

public class SubscriptionsHandler {

	private final CryptoService cryptoService;

	private final HttpClient httpClient;

	private final Algorithm jwtAlgorithm;

	private final ServerKeys serverKeys;

	private final ObjectMapper objectMapper;

	private static SubscriptionsHandler subscriptionsHandlerInstance = null;

	private final RedisRepository repository;

	private SubscriptionsHandler(ServerKeys serverKeys) {
		cryptoService = new CryptoService();
		this.httpClient = HttpClient.newHttpClient();
		this.serverKeys = serverKeys;
		this.objectMapper = new ObjectMapper();
		this.jwtAlgorithm = Algorithm.ECDSA256(this.serverKeys.getPublicKey(), this.serverKeys.getPrivateKey());
		this.repository = new RedisRepository();
		Logger.getLogger(SubscriptionsHandler.class.getName()).info("SubscriptionsHandler has correctly been created");
	}

	public static SubscriptionsHandler getInstance(ServerKeys serverKeys) {
		if (subscriptionsHandlerInstance == null) {
			subscriptionsHandlerInstance = new SubscriptionsHandler(serverKeys);
		}
		return subscriptionsHandlerInstance;
	}

	public void subscribeUser(Subscription subscription) {
		repository.registerUser(subscription);
	}

	public void unsubscribeUser(SubscriptionEndpoint subscriptionEndpoint) {
		repository.unregisterUserByEndpoint(subscriptionEndpoint.getEndpoint());
	}

	public void unsubscribeUser(String username) {
		repository.unregisterUserByUsername(username);
	}

	public boolean isSubscribed(SubscriptionEndpoint subscriptionEndpoint) {
		return repository.isSubscribed(subscriptionEndpoint.getEndpoint());
	}

	public void subscribeToTopic(String username, String topic) {
		repository.subscribeUserToTopic(topic, username);
	}

	public void publishMessageForTopic(String message, String topic) {
		// retrieve info of users and associated subscriptions
		Collection<String> usernames = repository.getAllUsernameFromTopic(topic);
		Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
		for (String username : usernames) {
			Subscription sub = repository.getSubscriptionFromUsername(username);
			subscriptions.put(sub.getEndpoint(), sub);
		}
		// sending message to each user
		if (subscriptions.isEmpty()) {
			Logger.getLogger(SubscriptionsHandler.class.getName()).info("No user to whom deliver message");
			return;
		}
		try {
			sendPushMessageToAllSubscribers(subscriptions, new PushMessage(topic, message));
		} catch (IOException e) {
			Logger.getLogger(PushController.class.getName()).info("Error sending message to users");
		}
	}

	private void sendPushMessageToAllSubscribers(Map<String, Subscription> subs, Object message)
			throws JsonProcessingException {

		Set<String> failedSubscriptions = new HashSet<>();

		for (Subscription subscription : subs.values()) {
			try {
				byte[] result = this.cryptoService.encrypt(this.objectMapper.writeValueAsString(message),
						subscription.getKeys().getP256dh(), subscription.getKeys().getAuth(), 0);
				boolean remove = sendPushMessage(subscription, result);
				if (remove) {
					failedSubscriptions.add(subscription.getEndpoint());
				}
			} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException
					| IllegalStateException | InvalidKeySpecException | NoSuchPaddingException
					| IllegalBlockSizeException | BadPaddingException e) {
				Logger.getLogger(PushController.class.getName()).info("send encrypted messages" + e);
			}
		}

		failedSubscriptions.forEach(repository::unregisterUserByEndpoint);
	}

	/**
	 * @return true if the subscription is no longer valid and can be removed, false
	 *         if everything is okay
	 */
	private boolean sendPushMessage(Subscription subscription, byte[] body) {
		String origin = null;
		try {
			URL url = new URL(subscription.getEndpoint());
			origin = url.getProtocol() + "://" + url.getHost();
		} catch (MalformedURLException e) {
			Logger.getLogger(PushController.class.getName()).info("create origin" + e);
			return true;
		}

		Date today = new Date();
		Date expires = new Date(today.getTime() + 12 * 60 * 60 * 1000); // 12 hours

		String token = JWT.create().withAudience(origin).withExpiresAt(expires)
				.withSubject("mailto:example@example.com").sign(this.jwtAlgorithm);

		URI endpointURI = URI.create(subscription.getEndpoint());

		HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
		if (body != null) {
			httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body))
					.header("Content-Type", "application/octet-stream").header("Content-Encoding", "aes128gcm");
		} else {
			httpRequestBuilder.POST(HttpRequest.BodyPublishers.noBody());
		}

		HttpRequest request = httpRequestBuilder.uri(endpointURI).header("TTL", "180")
				.header("Authorization", "vapid t=" + token + ", k=" + this.serverKeys.getPublicKeyBase64()).build();
		try {
			HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());

			switch (response.statusCode()) {
			case 201:
				Logger.getLogger(PushController.class.getName())
						.info("Push message successfully sent: " + subscription.getEndpoint());
				break;
			case 404:
			case 410:
				Logger.getLogger(PushController.class.getName())
						.info("Subscription not found or gone: " + subscription.getEndpoint());
				// remove subscription from our collection of subscriptions
				return true;
			case 429:
				Logger.getLogger(PushController.class.getName()).info("Too many requests: " + request);
				break;
			case 400:
				Logger.getLogger(PushController.class.getName()).info("Invalid request: " + request);
				break;
			case 413:
				Logger.getLogger(PushController.class.getName()).info("Payload size too large: " + request);
				break;
			default:
				Logger.getLogger(PushController.class.getName())
						.info("Unhandled status code: " + response.statusCode() + " -> " + request);
			}
		} catch (IOException | InterruptedException e) {
			Logger.getLogger(PushController.class.getName()).info("Send push message" + e);
		}

		return false;
	}

}
