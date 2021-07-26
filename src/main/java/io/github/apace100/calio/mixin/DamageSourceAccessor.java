package io.github.apace100.calio.mixin;

import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * On forge, use {@link DamageSource}'s methods instead.
 * On fabric, use an access widener.
 */
@Deprecated
@Mixin(DamageSource.class)
public interface DamageSourceAccessor {

	/**
	 * @deprecated Use {@link DamageSource#DamageSource(String)} instead
	 */
    @Invoker("<init>")
	@Deprecated
    static DamageSource createDamageSource(String name) {
        throw new RuntimeException("Evil invoker exception! >:)");
    }

	/**
	 * @deprecated Use {@link DamageSource#bypassArmor()} instead
	 */
	@Invoker("bypassArmor")
	@Deprecated
	DamageSource callSetBypassesArmor();

	/**
	 * @deprecated Use {@link DamageSource#bypassInvul()} instead
	 */
    @Invoker("bypassInvul")
	@Deprecated
    DamageSource callSetOutOfWorld();

	/**
	 * @deprecated Use {@link DamageSource#bypassMagic()} instead
	 */
    @Invoker("bypassMagic")
	@Deprecated
    DamageSource callSetUnblockable();

	/**
	 * @deprecated Use {@link DamageSource#setIsFire()} instead
	 */
    @Invoker("setIsFire")
	@Deprecated
    DamageSource callSetFire();

	/**
	 * @deprecated Use {@link DamageSource#setMagic()} instead
	 */
    @Invoker("setMagic")
	@Deprecated
    DamageSource callSetUsesMagic();

	/**
	 * @deprecated Use {@link DamageSource#setProjectile()} instead
	 */
    @Invoker("setProjectile")
	@Deprecated
    DamageSource callSetProjectile();

	/**
	 * @deprecated Use {@link DamageSource#setExplosion()} instead
	 */
    @Invoker("setExplosion")
	@Deprecated
    DamageSource callSetExplosive();
}
