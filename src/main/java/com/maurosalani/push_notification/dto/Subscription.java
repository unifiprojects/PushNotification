package com.maurosalani.push_notification.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Subscription implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String username;

	private final String endpoint;

	private final Long expirationTime;

	public final SubscriptionKeys keys;

	@JsonCreator
	public Subscription(@JsonProperty("username") String username, @JsonProperty("endpoint") String endpoint,
			@JsonProperty("expirationTime") Long expirationTime, @JsonProperty("keys") SubscriptionKeys keys) {
		this.username = username;
		this.endpoint = endpoint;
		this.expirationTime = expirationTime;
		this.keys = keys;
	}

	public String getUsername() {
		return this.username;
	}

	public String getEndpoint() {
		return this.endpoint;
	}

	public Long getExpirationTime() {
		return this.expirationTime;
	}

	public SubscriptionKeys getKeys() {
		return this.keys;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + ((expirationTime == null) ? 0 : expirationTime.hashCode());
		result = prime * result + ((keys == null) ? 0 : keys.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (endpoint == null) {
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (expirationTime == null) {
			if (other.expirationTime != null)
				return false;
		} else if (!expirationTime.equals(other.expirationTime))
			return false;
		if (keys == null) {
			if (other.keys != null)
				return false;
		} else if (!keys.equals(other.keys))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Subscription [endpoint=" + this.endpoint + ", expirationTime=" + this.expirationTime + ", keys="
				+ this.keys + "]";
	}

}