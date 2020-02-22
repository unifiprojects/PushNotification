package com.maurosalani.push_notification;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maurosalani.push_notification.dto.Subscription;
import com.maurosalani.push_notification.dto.SubscriptionEndpoint;

public class PushController {

	private final ServerKeys serverKeys;

	private final SubscriptionsHandler subscriptionsHandler;

	public PushController(ServerKeys serverKeys) {
		this.serverKeys = serverKeys;
		subscriptionsHandler = SubscriptionsHandler.getInstance(serverKeys);
		Logger.getLogger(PushController.class.getName()).info("PushController has correctly been created");
	}

    @GET
	@Path("publicSigningKey")
        @Produces("application/octet-stream")
	public byte[] publicSigningKey() {
		return this.serverKeys.getPublicKeyUncompressed();
	}

    @GET
	@Path("publicSigningKeyBase64")
	public String publicSigningKeyBase64() {
		return this.serverKeys.getPublicKeyBase64();
	}

    @POST
    @Path("subscribe")
	public void subscribe(Subscription subscription) {
		boolean isSubscribed = subscriptionsHandler.isSubscribed(new SubscriptionEndpoint(subscription.getEndpoint()));
		if (!isSubscribed) {
			Logger.getLogger(PushController.class.getName())
					.info("Username: " + subscription.getUsername() + " subscribed: " + subscription.getEndpoint());
			subscriptionsHandler.subscribeUser(subscription);
		} else {
			Logger.getLogger(PushController.class.getName())
					.info(isSubscribed + " = IsSubscribed: " + subscription.getEndpoint());
		}
	}

    @POST
    @Path("unsubscribe")
	public void unsubscribe(SubscriptionEndpoint subscription) {
		Logger.getLogger(PushController.class.getName()).info("Unsubscription: " + subscription.getEndpoint());
		subscriptionsHandler.unsubscribeUser(subscription);
	}

    @POST
    @Path("isSubscribed")
	public boolean isSubscribed(SubscriptionEndpoint subscription) {
		boolean isSubscribed = subscriptionsHandler.isSubscribed(subscription);
		return isSubscribed;
	}

}
