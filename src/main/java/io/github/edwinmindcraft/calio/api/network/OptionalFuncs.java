package io.github.edwinmindcraft.calio.api.network;

import com.mojang.datafixers.util.*;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This is the result of a code generator.
 */
public class OptionalFuncs {
	public static <T1, R> Function<T1, Optional<R>> opt(Function<T1, R> func) {
		return (t1) -> Optional.ofNullable(func.apply(t1));
	}

	public static <T1, R> Function<Optional<T1>, R> of(Function<T1, R> func) {
		return (t1) -> func.apply(t1.orElse(null));
	}

	public static <T1, T2, R> BiFunction<Optional<T1>, Optional<T2>, R> of(BiFunction<T1, T2, R> func) {
		return (t1, t2) -> func.apply(t1.orElse(null), t2.orElse(null));
	}

	public static <T1, T2, T3, R> Function3<Optional<T1>, Optional<T2>, Optional<T3>, R> of(Function3<T1, T2, T3, R> func) {
		return (t1, t2, t3) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null));
	}

	public static <T1, T2, T3, T4, R> Function4<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, R> of(Function4<T1, T2, T3, T4, R> func) {
		return (t1, t2, t3, t4) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, R> Function5<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, R> of(Function5<T1, T2, T3, T4, T5, R> func) {
		return (t1, t2, t3, t4, t5) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, R> Function6<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, R> of(Function6<T1, T2, T3, T4, T5, T6, R> func) {
		return (t1, t2, t3, t4, t5, t6) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, R> Function7<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, R> of(Function7<T1, T2, T3, T4, T5, T6, T7, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function8<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, R> of(Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function9<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, R> of(Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> Function10<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, R> of(Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> Function11<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, R> of(Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> Function12<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, Optional<T12>, R> of(Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null), t12.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> Function13<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, Optional<T12>, Optional<T13>, R> of(Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null), t12.orElse(null), t13.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> Function14<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, Optional<T12>, Optional<T13>, Optional<T14>, R> of(Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null), t12.orElse(null), t13.orElse(null), t14.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> Function15<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, Optional<T12>, Optional<T13>, Optional<T14>, Optional<T15>, R> of(Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null), t12.orElse(null), t13.orElse(null), t14.orElse(null), t15.orElse(null));
	}

	public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> Function16<Optional<T1>, Optional<T2>, Optional<T3>, Optional<T4>, Optional<T5>, Optional<T6>, Optional<T7>, Optional<T8>, Optional<T9>, Optional<T10>, Optional<T11>, Optional<T12>, Optional<T13>, Optional<T14>, Optional<T15>, Optional<T16>, R> of(Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, R> func) {
		return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16) -> func.apply(t1.orElse(null), t2.orElse(null), t3.orElse(null), t4.orElse(null), t5.orElse(null), t6.orElse(null), t7.orElse(null), t8.orElse(null), t9.orElse(null), t10.orElse(null), t11.orElse(null), t12.orElse(null), t13.orElse(null), t14.orElse(null), t15.orElse(null), t16.orElse(null));
	}
}
