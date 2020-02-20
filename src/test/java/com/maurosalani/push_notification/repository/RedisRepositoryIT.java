package com.maurosalani.push_notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionKeys;

@RunWith(MockitoJUnitRunner.class)
public class RedisRepositoryIT {

	private RedisRepository repository;

	@Before
	public void setup() {
		repository = new RedisRepository();
		repository.topic_username.clear();
		repository.username_subscription.clear();
	}

	@Test
	public void testSubscribeOfOneTopicAndMultipleUsers() {
		String topic = "topic1";
		repository.subscribeUserToTopic(topic, "user1");
		repository.subscribeUserToTopic(topic, "user2");
		assertThat(repository.getAllUsernameFromTopic(topic)).containsExactlyInAnyOrder("user1", "user2");
	}

	@Test
	public void testUnsubscribeUser() {
		String topic = "test1";
		repository.subscribeUserToTopic(topic, "user1");
		repository.unsubscribeUserFromTopic(topic, "user1");
		assertThat(repository.getAllUsernameFromTopic(topic)).isEmpty();
	}

	@Test
	public void testRegisterOfNewUserSubscription() {
		Subscription subscription1 = new Subscription("user1", "endpoint1", 1L, new SubscriptionKeys("a", "a"));
		repository.registerUser(subscription1);
		assertThat(repository.username_subscription.get("user1")).isEqualTo(subscription1);

		Subscription subscription2 = new Subscription("user2", "endpoint1", 1L, new SubscriptionKeys("a", "a"));

		repository.registerUser(subscription2);
		assertThat(repository.username_subscription.get("user2")).isEqualTo(subscription2);
	}

	@Test
	public void testUnregisterUserByEndpoint() {
		Subscription subscription1 = new Subscription("user1", "endpoint1", 1L, new SubscriptionKeys("a", "a"));
		repository.registerUser(subscription1);
		repository.unregisterUserByEndpoint("endpoint1");
		assertThat(repository.getSubscriptionFromUsername("user1")).isNull();
	}
	
	@Test
	public void testUnregisterUserByUsername() {
		Subscription subscription1 = new Subscription("user1", "endpoint1", 1L, new SubscriptionKeys("a", "a"));
		repository.registerUser(subscription1);
		repository.unregisterUserByUsername("user1");
		assertThat(repository.getSubscriptionFromUsername("user1")).isNull();
	}
}
