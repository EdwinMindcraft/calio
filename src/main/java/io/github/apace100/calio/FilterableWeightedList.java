package io.github.apace100.calio;

import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FilterableWeightedList<U> extends ShufflingList<U> {

	private Predicate<U> filter;

	public int size() {
		return this.entries.size();
	}

	public void addFilter(Predicate<U> filter) {
		if (this.hasFilter()) {
			this.filter = this.filter.and(filter);
		} else {
			this.setFilter(filter);
		}
	}

	public void setFilter(Predicate<U> filter) {
		this.filter = filter;
	}

	public void removeFilter() {
		this.filter = null;
	}

	public boolean hasFilter() {
		return this.filter != null;
	}

	@Override
	@NotNull
	public Stream<U> stream() {
		if (this.filter != null)
			return this.entries.stream().map(WeightedEntry::getData).filter(this.filter);
		return super.stream();
	}

	public Stream<WeightedEntry<U>> entryStream() {
		return this.entries.stream().filter(entry -> this.filter == null || this.filter.test(entry.getData()));
	}

	public void addAll(FilterableWeightedList<U> other) {
		other.entryStream().forEach(entry -> this.add(entry.getData(), entry.getWeight()));
	}

	public U pickRandom(Random random) {
		return this.pickRandom();
	}

	public U pickRandom() {
		return this.shuffle().stream().findFirst().orElseThrow(RuntimeException::new);
	}

	public FilterableWeightedList<U> copy() {
		FilterableWeightedList<U> copied = new FilterableWeightedList<>();
		copied.addAll(this);
		return copied;
	}
}
