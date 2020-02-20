package com.maurosalani.push_notification.repository;

import java.util.Collection;

import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RMultimap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.maurosalani.push_notification.dto.Subscription;

public class RedisRepository {

	private final String PORT = "6379";
	private final String URL = "redis://127.0.0.1:";
	private final String TOPIC_USERNAME = "topic_username";
	private final String USERNAME_SUBSCRIPTION = "username_subscription";
	private final String SUB_ENDPOINT_USERNAME = "sub_endpoint_username";
	final RMultimap<String, String> topic_username; // package scope for IT test
	final RMap<String, Subscription> username_subscription; // package scope for IT test

	public RedisRepository() {
		Config config = new Config();
		config.useSingleServer().setAddress(URL + PORT);
		RedissonClient redisson = Redisson.create(config);
		topic_username = redisson.getSetMultimap(TOPIC_USERNAME);
		username_subscription = redisson.getMap(USERNAME_SUBSCRIPTION);
	}

	public void subscribeUserToTopic(String topic, String username) {
		topic_username.put(topic, username);
	}

	public void unsubscribeUserFromTopic(String topic, String username) {
		topic_username.remove(topic, username);
	}

	public Collection<String> getAllUsernameFromTopic(String topic) {
		return topic_username.getAll(topic);
	}

	public void unsubscribeUsernameFromAllTopics(String username) {
		topic_username.keySet().stream().forEach(topic -> unsubscribeUserFromTopic(topic, username));
	}

	public void registerUser(Subscription subscription) {
		username_subscription.put(subscription.getUsername(), subscription);
	}

	public void unregisterUserByEndpoint(String endpoint) {
                Subscription subscription = username_subscription.values().stream()
                                    .filter(sub -> sub.getEndpoint().equals(endpoint)).findFirst().get();
              	if (subscription != null) {
			String username = subscription.getUsername();
			removeUsernameFromTopics(username);
			username_subscription.remove(username);
		}
	}

	public void unregisterUserByUsername(String username) {
		if (username_subscription.containsKey(username)) {
			String endpoint = username_subscription.get(username).getEndpoint();
			removeUsernameFromTopics(username);
			username_subscription.remove(username);
		}
	}

	private void removeUsernameFromTopics(String username) {
		for (String topic : topic_username.keySet()) {
			if (topic_username.get(topic).contains(username)) {
				topic_username.remove(topic, username);
			}
		}
	}

	public boolean isSubscribed(String endpoint) {
            
                boolean isSubscribed = username_subscription.values().stream()
                    .anyMatch(sub -> sub.getEndpoint().equals(endpoint));
		return isSubscribed;
	}

	public String getPORT() {
		return PORT;
	}

	public String getURL() {
		return URL;
	}

	public Subscription getSubscriptionFromUsername(String username) {
		return username_subscription.get(username);
	}
}
