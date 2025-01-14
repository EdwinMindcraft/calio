package io.github.apace100.calio.data;

import java.util.Locale;

public class DataException extends RuntimeException {

	private final Phase phase;
	private String path;
	private final Exception exception;

	public DataException(Phase phase, String path, Exception exception) {
		super("Error " + phase.name().toLowerCase(Locale.ROOT) + " data field", exception);
		this.phase = phase;
		this.path = path;
		this.exception = exception;
	}

	public DataException prepend(String path) {
		this.path = path + "." + this.path;
		return this;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " at " + this.path + ": " + this.exception.getMessage();
	}

	public enum Phase {
		READING,
		RECEIVING,
		WRITING
	}
}
