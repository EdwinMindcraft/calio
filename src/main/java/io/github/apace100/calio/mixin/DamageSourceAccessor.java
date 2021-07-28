package io.github.apace100.calio.mixin;

import net.minecraft.world.damagesource.DamageSource;

/**
 * On forge, use {@link DamageSource}'s methods instead.
 * On fabric, use an access widener.
 */
@Deprecated
//@Mixin(DamageSource.class)
public interface DamageSourceAccessor {

	/**
	 * @deprecated Use {@link DamageSource#DamageSource(String)} instead
	 */
	//@Invoker("<init>")
	@Deprecated
	static DamageSource createDamageSource(String name) {
		return new DamageSource(name);
	}

	/**
	 * @deprecated Use {@link DamageSource#bypassArmor()} instead
	 */
	//@Invoker("bypassArmor")
	@Deprecated
	default DamageSource callSetBypassesArmor() {
		return ((DamageSource) this).bypassArmor();
	}

	/**
	 * @deprecated Use {@link DamageSource#bypassInvul()} instead
	 */
	//@Invoker("bypassInvul")
	@Deprecated
	default DamageSource callSetOutOfWorld() {
		return ((DamageSource) this).bypassInvul();
	}

	/**
	 * @deprecated Use {@link DamageSource#bypassMagic()} instead
	 */
	//@Invoker("bypassMagic")
	@Deprecated
	default DamageSource callSetUnblockable() {
		return ((DamageSource) this).bypassMagic();
	}

	/**
	 * @deprecated Use {@link DamageSource#setIsFire()} instead
	 */
	//@Invoker("setIsFire")
	@Deprecated
	default DamageSource callSetFire() {
		return ((DamageSource) this).setIsFire();
	}

	/**
	 * @deprecated Use {@link DamageSource#setMagic()} instead
	 */
	//@Invoker("setMagic")
	@Deprecated
	default DamageSource callSetUsesMagic() {
		return ((DamageSource) this).setMagic();
	}

	/**
	 * @deprecated Use {@link DamageSource#setProjectile()} instead
	 */
	//@Invoker("setProjectile")
	@Deprecated
	default DamageSource callSetProjectile() {
		return ((DamageSource) this).setProjectile();
	}

	/**
	 * @deprecated Use {@link DamageSource#setExplosion()} instead
	 */
	//@Invoker("setExplosion")
	@Deprecated
	default DamageSource callSetExplosive() {
		return ((DamageSource) this).setExplosion();
	}
}
