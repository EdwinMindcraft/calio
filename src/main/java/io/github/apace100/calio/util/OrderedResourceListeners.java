package io.github.apace100.calio.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Allows registering data resource listeners in a specified order, to prevent problems
 * due to mod loading order and inter-mod data dependencies.
 */
public final class OrderedResourceListeners {

	private static final Map<ResourceLocation, ImmutableRegistration> REGISTRATIONS = new HashMap<>();

	public static List<PreparableReloadListener> orderedList() {
		Set<ResourceLocation> handled = new HashSet<>();
		int prevSize;
		do {
			prevSize = handled.size();
			for (ImmutableRegistration value : REGISTRATIONS.values()) {
				if (handled.contains(value.key()))
					continue;
				if (handled.containsAll(value.dependencies()))
					handled.add(value.key());
			}
		} while (prevSize != handled.size());
		if (handled.size() != REGISTRATIONS.size())
			throw new IllegalStateException("Some validators have missing or circular dependencies: [" + String.join(",", REGISTRATIONS.values().stream().filter(x -> !handled.contains(x.key())).map(Record::toString).collect(Collectors.toSet())) + "]");
		return handled.stream().map(REGISTRATIONS::get).filter(x -> !x.dummy() && x.listener() != null).map(ImmutableRegistration::listener).collect(ImmutableList.toImmutableList());
	}

	public static Registration register(PreparableReloadListener resourceReloadListener, ResourceLocation location) {
		return new Registration(resourceReloadListener, location);
	}

	public static void addDummy(ResourceLocation location) {
		new Registration(null, location, true).complete();
	}

	private static void completeRegistration(Registration registration) {
		REGISTRATIONS.put(registration.key(), new ImmutableRegistration(registration.resourceReloadListener, registration.key(), registration.afterSet.build(), registration.dummy));
	}

	private record ImmutableRegistration(PreparableReloadListener listener, ResourceLocation key,
										 ImmutableSet<ResourceLocation> dependencies, boolean dummy) {}

	public static class Registration {

		private final PreparableReloadListener resourceReloadListener;
		private final ResourceLocation key;
		private final ImmutableSet.Builder<ResourceLocation> afterSet = new ImmutableSet.Builder<>();
		private final boolean dummy;
		private boolean isCompleted;

		private Registration(PreparableReloadListener resourceReloadListener, ResourceLocation key, boolean dummy) {
			this.resourceReloadListener = resourceReloadListener;
			this.key = key;
			this.dummy = dummy;
		}

		private Registration(PreparableReloadListener resourceReloadListener, ResourceLocation key) {
			this(resourceReloadListener, key, false);
		}

		public ResourceLocation key() {
			return this.key;
		}

		public Registration after(ResourceLocation identifier) {
			if (this.isCompleted) {
				throw new IllegalStateException(
						"Can't add a resource reload listener registration dependency after it was completed.");
			}
			this.afterSet.add(identifier);
			return this;
		}

		public void complete() {
			completeRegistration(this);
			this.isCompleted = true;
		}
	}
}
