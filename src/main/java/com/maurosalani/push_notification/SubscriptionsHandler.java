package com.maurosalani.push_notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionEndpoint;

public class SubscriptionsHandler {

	private static SubscriptionsHandler subscriptionsHandlerInstance = null;
	// map from username to subscription
	private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
	// map from subscriptionEndpoint to username
	private final Map<String, String> subscriptionsEndpoint_Username = new ConcurrentHashMap<>();
	// map from topic to username
	private final Map<String, String> topic_username = new ConcurrentHashMap<>();

	private SubscriptionsHandler() {

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
        
       
}
