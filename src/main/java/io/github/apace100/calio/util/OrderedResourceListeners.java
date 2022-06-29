package io.github.apace100.calio.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.github.apace100.calio.resource.OrderedResourceListener;
import io.github.apace100.calio.resource.OrderedResourceListenerManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Allows registering data resource listeners in a specified order, to prevent problems
 * due to mod loading order and inter-mod data dependencies.
 */
@Deprecated(forRemoval = true, since = "1.18.2")
@ApiStatus.ScheduledForRemoval(inVersion = "1.19")
public final class OrderedResourceListeners {

	private static final Map<ResourceLocation, ImmutableRegistration> REGISTRATIONS = new HashMap<>();

	@Deprecated
	public static List<PreparableReloadListener> orderedList() {
		Set<ResourceLocation> handled = new HashSet<>();
		List<ResourceLocation> ordered = new LinkedList<>();
		int prevSize;
		do {
			prevSize = handled.size();
			for (ImmutableRegistration value : REGISTRATIONS.values()) {
				if (handled.contains(value.key()))
					continue;
				if (handled.containsAll(value.dependencies())) {
					handled.add(value.key());
					OptionalInt min = value.children().stream().mapToInt(ordered::indexOf).filter(x -> x >= 0).min();
					if (min.isEmpty())
						ordered.add(value.key());
					else
						ordered.add(min.getAsInt(), value.key());
				}
			}
		} while (prevSize != handled.size());
		if (handled.size() != REGISTRATIONS.size())
			throw new IllegalStateException("Some validators have missing or circular dependencies: [" + String.join(",", REGISTRATIONS.values().stream().filter(x -> !handled.contains(x.key())).map(Record::toString).collect(Collectors.toSet())) + "]");
		return ordered.stream().map(REGISTRATIONS::get).filter(x -> !x.dummy() && x.listener() != null).map(ImmutableRegistration::listener).collect(ImmutableList.toImmutableList());
	}

	@Deprecated
	public static Registration register(PreparableReloadListener resourceReloadListener, ResourceLocation location) {
		return new Registration(resourceReloadListener, location);
	}

	@Deprecated
	public static void addDummy(ResourceLocation location) {
		new Registration(null, location, true).complete();
	}

	@Deprecated
	public static void completeRegistration(Registration registration) {
		ImmutableRegistration value = new ImmutableRegistration(registration.resourceReloadListener, registration.key(), registration.afterSet.build(), registration.beforeSet.build(), registration.dummy);
		REGISTRATIONS.put(registration.key(), value);
		//Shut up, it works.
		OrderedResourceListener.Registration register = OrderedResourceListenerManager.getInstance().register(PackType.SERVER_DATA, registration.key(), registration.resourceReloadListener);
		value.children().forEach(register::before);
		value.dependencies().forEach(register::after);
		register.complete();
	}

	@Deprecated
	private record ImmutableRegistration(PreparableReloadListener listener, ResourceLocation key,
										 ImmutableSet<ResourceLocation> dependencies,
										 ImmutableSet<ResourceLocation> children, boolean dummy) {}

	@Deprecated
	public static class Registration {

		private final PreparableReloadListener resourceReloadListener;
		private final ResourceLocation key;
		private final ImmutableSet.Builder<ResourceLocation> afterSet = new ImmutableSet.Builder<>();
		private final ImmutableSet.Builder<ResourceLocation> beforeSet = new ImmutableSet.Builder<>();
		private final boolean dummy;
		private boolean isCompleted;

		@Deprecated
		private Registration(PreparableReloadListener resourceReloadListener, ResourceLocation key, boolean dummy) {
			this.resourceReloadListener = resourceReloadListener;
			this.key = key;
			this.dummy = dummy;
		}

		@Deprecated
		private Registration(PreparableReloadListener resourceReloadListener, ResourceLocation key) {
			this(resourceReloadListener, key, false);
		}

		@Deprecated
		public ResourceLocation key() {
			return this.key;
		}

		@Deprecated
		public Registration after(ResourceLocation identifier) {
			if (this.isCompleted) {
				throw new IllegalStateException(
						"Can't add a resource reload listener registration dependency after it was completed.");
			}
			this.afterSet.add(identifier);
			return this;
		}

		@Deprecated
		public Registration before(ResourceLocation identifier) {
			if (this.isCompleted) {
				throw new IllegalStateException(
						"Can't add a resource reload listener registration dependency after it was completed.");
			}
			this.beforeSet.add(identifier);
			return this;
		}

		@Deprecated
		public void complete() {
			completeRegistration(this);
			this.isCompleted = true;
		}
	}
}
