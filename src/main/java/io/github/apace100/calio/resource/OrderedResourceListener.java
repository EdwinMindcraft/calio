package io.github.apace100.calio.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.HashSet;
import java.util.Set;

public class OrderedResourceListener {

	public static final String ENTRYPOINT_KEY = "calio:ordered-resource-listener";

	public static class Registration {

		private final OrderedResourceListenerManager.Instance manager;
		final ResourceLocation id;
		final PreparableReloadListener resourceReloadListener;
		final Set<ResourceLocation> dependencies = new HashSet<>();
		final Set<ResourceLocation> dependants = new HashSet<>();
		private boolean isCompleted;

		Registration(OrderedResourceListenerManager.Instance manager, ResourceLocation id, PreparableReloadListener listener) {
			this.id = id;
			this.manager = manager;
			this.resourceReloadListener = listener;
		}

		public Registration after(String identifier) {
			return this.after(new ResourceLocation(identifier));
		}

		public Registration after(ResourceLocation identifier) {
			if (this.isCompleted) {
				throw new IllegalStateException(
						"Can't add a resource reload listener registration dependency after it was completed.");
			}
			this.dependencies.add(identifier);
			return this;
		}

		public Registration before(String identifier) {
			return this.before(new ResourceLocation(identifier));
		}

		public Registration before(ResourceLocation identifier) {
			if (this.isCompleted) {
				throw new IllegalStateException(
						"Can't add a resource reload listener registration dependant after it was completed.");
			}
			this.dependants.add(identifier);
			return this;
		}

		public void complete() {
			this.isCompleted = true;
			this.manager.add(this);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(this.id.toString());
			builder.append("{depends_on=[");
			boolean first = true;
			for (ResourceLocation afterId : this.dependencies) {
				builder.append(afterId);
				if (!first) {
					builder.append(',');
				}
				first = false;
			}
			builder.append("]}");
			return builder.toString();
		}
	}
}
