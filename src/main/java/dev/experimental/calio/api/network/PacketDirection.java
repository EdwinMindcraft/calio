package dev.experimental.calio.api.network;

public enum PacketDirection {
	/**
	 * Login pipeline, from client to server.
	 */
	LOGIN_SERVERBOUND,
	/**
	 * Login pipeline, from server to client.
	 */
	LOGIN_CLIENTBOUND,
	/**
	 * Play pipeline, from client to server.
	 */
	PLAY_SERVERBOUND,
	/**
	 * Play pipeline, from server to client.
	 */
	PLAY_CLIENTBOUND
}
