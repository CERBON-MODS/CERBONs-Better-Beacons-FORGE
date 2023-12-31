package com.cerbon.better_beacons.effect;

import com.cerbon.better_beacons.effect.custom.LongReachEffect;
import com.cerbon.better_beacons.effect.custom.PatrolNullifierEffect;
import com.cerbon.better_beacons.effect.custom.PhantomBaneEffect;
import com.cerbon.better_beacons.util.BBConstants;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BBEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BBConstants.MOD_ID);

    public static final RegistryObject<MobEffect> PHANTOM_BANE = MOB_EFFECTS.register("phantom_bane",
            () -> new PhantomBaneEffect(MobEffectCategory.BENEFICIAL, 3124687));

    public static final RegistryObject<MobEffect> PATROL_NULLIFIER = MOB_EFFECTS.register("patrol_nullifier",
            () -> new PatrolNullifierEffect(MobEffectCategory.BENEFICIAL, 9120331));

    public static final RegistryObject<MobEffect> LONG_REACH = MOB_EFFECTS.register("long_reach",
            () -> new LongReachEffect(MobEffectCategory.BENEFICIAL, 0xDEF58F)
                    .addAttributeModifier(ForgeMod.BLOCK_REACH.get(), "C7F45B68-A090-4AD7-B75B-376BD2991CFD", 3D, AttributeModifier.Operation.ADDITION));

    public static void register(IEventBus eventBus){
        MOB_EFFECTS.register(eventBus);
    }
}
