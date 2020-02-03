package com.maurosalani.push_notification;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maurosalani.push_notification.dto.Notification;
import com.maurosalani.push_notification.dto.PushMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionEndpoint;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.springframework.scheduling.annotation.Scheduled;

public class SubscriptionsHandler {
    
        private final CryptoService cryptoService;
        
        private final HttpClient httpClient;
        
        private final Algorithm jwtAlgorithm;
        
        private final ServerKeys serverKeys;
        
        private final ObjectMapper objectMapper;

	private static SubscriptionsHandler subscriptionsHandlerInstance = null;
	// map from username to subscription
	private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
	// map from subscriptionEndpoint to username
	private final Map<String, String> subscriptionsEndpoint_Username = new ConcurrentHashMap<>();
	// map from topic to username
	private final Map<String, List<String>> topic_username = new ConcurrentHashMap<>();

	private SubscriptionsHandler() {
                cryptoService = new CryptoService();
                this.httpClient = HttpClient.newHttpClient();
                this.serverKeys = new ServerKeys(new AppProperties(), cryptoService);
                this.objectMapper = new ObjectMapper();
                this.jwtAlgorithm = Algorithm.ECDSA256(this.serverKeys.getPublicKey(), this.serverKeys.getPrivateKey());
	}

	public static SubscriptionsHandler getInstance() {
		if (subscriptionsHandlerInstance == null) {
			subscriptionsHandlerInstance = new SubscriptionsHandler();
		}
		return subscriptionsHandlerInstance;
	}

	public void subscribeUser(Subscription subscription) {
		this.subscriptions.put(subscription.getUsername(), subscription);
		this.subscriptionsEndpoint_Username.put(subscription.getEndpoint(), subscription.getUsername());
	}

	public void unsubscribeUser(SubscriptionEndpoint subscriptionEndpoint) {
		String username = subscriptionsEndpoint_Username.get(subscriptionEndpoint.getEndpoint());
		this.subscriptions.remove(username);
	}

	public boolean isSubscribed(SubscriptionEndpoint subscriptionEndpoint) {
		return this.subscriptionsEndpoint_Username.containsKey(subscriptionEndpoint.getEndpoint());
	}

	public void subscribeToTopic(String username, String topic) {
		this.topic_username.get(topic).add(username);
	}

	public void publishMessageForTopic(String message, String topic) {
		// ottieni le subscription di tutti gli username iscritti al topic e invia loro un messaggio
                
                //ottengo la lista degli username associati ad un topic
		List<String> usernames = topic_username.get(topic);
                //ottendo le subscription dagli username
                List<Subscription> subscription = usernames.stream()
                                                       .map(username -> subscriptions.get(username))
                                                       .collect(Collectors.toList());
                //invio messaggio
                
	}
        
        public void chuckNorrisJoke() {
        if (this.subscriptions.isEmpty()) {
            return;
        }

        try {
            // >>>>>>>>>>>> EXAMPLE: retrieve joke from website >>>>>>>>>>>>
            HttpResponse<String> response = this.httpClient.send(
                    HttpRequest.newBuilder(URI.create("https://api.icndb.com/jokes/random")).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jokeJson = this.objectMapper.readValue(response.body(), Map.class);

                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) jokeJson.get("value");
                int id = (int) value.get("id");
                String joke = (String) value.get("joke");
                // <<<<<<<<<<<< EXAMPLE: retrieve joke from website <<<<<<<<<<<

                sendPushMessageToAllSubscribers(this.subscriptions, new PushMessage("Chuck Norris Joke: " + id, joke));

                Notification notification = new Notification("Chuck Norris Joke: " + id);
                notification.setBody(joke);
                notification.setIcon("assets/chuck.png");

                sendPushMessageToAllSubscribers(this.subscriptions, Map.of("notification", notification));
            }
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(PushController.class.getName()).info("fetch chuck norris" + e);
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

        failedSubscriptions.forEach(subs::remove);
    }

    /**
     * @return true if the subscription is no longer valid and can be removed,
     * false if everything is okay
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
            httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body)).header("Content-Type", "application/octet-stream")
                    .header("Content-Encoding", "aes128gcm");
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
            Logger.getLogger(PushController.class.getName()).info("send push message" + e);
        }

        return false;
    }       
       
}
