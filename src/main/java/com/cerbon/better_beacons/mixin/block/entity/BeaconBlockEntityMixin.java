package com.cerbon.better_beacons.mixin.block.entity;

import com.cerbon.better_beacons.menu.custom.NewBeaconMenu;
import com.cerbon.better_beacons.util.BBConstants;
import com.cerbon.better_beacons.util.BBUtils;
import com.cerbon.better_beacons.util.BeaconRedirectionAndTransparency;
import com.cerbon.better_beacons.util.IBeaconBlockEntityMixin;
import com.cerbon.better_beacons.util.json.BeaconPaymentItemsRangeManager;
import com.cerbon.better_beacons.world.inventory.BBContainerData;
import com.illusivesoulworks.beaconsforall.BeaconsForAllMod;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.LockCode;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin extends BlockEntity implements IBeaconBlockEntityMixin {
    @Shadow @Final public static final MobEffect[][] BEACON_EFFECTS = BBUtils.getBeaconEffectsFromConfigFile();
    @Shadow @Final @SuppressWarnings("unused") private static final Set<MobEffect> VALID_EFFECTS = Arrays.stream(BEACON_EFFECTS).flatMap(Arrays::stream).collect(Collectors.toSet());
    @Shadow public List<BeaconBlockEntity.BeaconBeamSection> beamSections;
    @Shadow public int levels;
    @Shadow private LockCode lockKey;
    @Shadow public MobEffect primaryPower;
    @Shadow public MobEffect secondaryPower;
    @Unique private MobEffect better_beacons_tertiaryPower;
    @Unique private String better_beacons_PaymentItem;
    @Shadow @Final private ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BeaconBlockEntityMixin.this.levels;
                case 1 -> MobEffect.getIdFromNullable(BeaconBlockEntityMixin.this.primaryPower);
                case 2 -> MobEffect.getIdFromNullable(BeaconBlockEntityMixin.this.secondaryPower);
                case 3 -> MobEffect.getIdFromNullable(BeaconBlockEntityMixin.this.better_beacons_tertiaryPower);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index){
                case 0:
                    BeaconBlockEntityMixin.this.levels = value;
                    break;
                case 1:
                    if (!Objects.requireNonNull(BeaconBlockEntityMixin.this.level).isClientSide && !BeaconBlockEntityMixin.this.beamSections.isEmpty())
                        BeaconBlockEntity.playSound(BeaconBlockEntityMixin.this.level, BeaconBlockEntityMixin.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);

                    BeaconBlockEntityMixin.this.primaryPower = BeaconBlockEntityMixin.getValidEffectById(value);
                case 2:
                    BeaconBlockEntityMixin.this.secondaryPower = BeaconBlockEntityMixin.getValidEffectById(value);
                case 3:
                    BeaconBlockEntityMixin.this.better_beacons_tertiaryPower = BeaconBlockEntityMixin.getValidEffectById(value);
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    @Unique private BBContainerData better_beacons_dataAccess = (key, value) -> {
        if (key.equals(BBConstants.PAYMENT_ITEM_KEY))
            BeaconBlockEntityMixin.this.better_beacons_PaymentItem = value;
    };

    @Shadow public abstract Component getDisplayName();
    @Shadow static MobEffect getValidEffectById(int effectId) {return null;}

    public BeaconBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void better_beacons_addCustomData(CompoundTag tag, CallbackInfo ci){
        tag.putInt(BBConstants.TERTIARY_POWER_KEY, MobEffect.getIdFromNullable(this.better_beacons_tertiaryPower));
        if (this.better_beacons_PaymentItem != null)
            tag.putString(BBConstants.PAYMENT_ITEM_KEY, this.better_beacons_PaymentItem);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void better_beacons_readCustomData(@NotNull CompoundTag tag, CallbackInfo ci){
        this.better_beacons_tertiaryPower = getValidEffectById(tag.getInt(BBConstants.TERTIARY_POWER_KEY));
        if (tag.contains(BBConstants.PAYMENT_ITEM_KEY))
            this.better_beacons_PaymentItem = tag.getString(BBConstants.PAYMENT_ITEM_KEY);
    }

    // This captures the for loop inside tick that computes the beacon segments
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 0, ordinal = 0))
    private static int better_beacons_tick(int val, Level level, BlockPos pos, BlockState state, BeaconBlockEntity beaconBlockEntity) {
        return BeaconRedirectionAndTransparency.tickBeacon(beaconBlockEntity);
    }

    // This captures the variable d0 in the target method and adds to it value the payment item range
    @ModifyVariable(method = "applyEffects", at = @At(value = "LOAD", ordinal = 0))
    private static double better_beacons_increaseRangeBasedOnPaymentItem(double defaultRange, @NotNull Level level, BlockPos pos, int levels, @Nullable MobEffect primary, @Nullable MobEffect secondary){
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof BeaconBlockEntity beaconBlockEntity){
            String paymentItem = ((IBeaconBlockEntityMixin) beaconBlockEntity).better_beacons_getPaymentItem();

            if (paymentItem != null)
                return BeaconPaymentItemsRangeManager.getItemRangeMap().getOrDefault(paymentItem, 0) + defaultRange;
        }
        return defaultRange;
    }

    @Inject(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", shift = At.Shift.BY, by = 2))
    private static void better_beacons_applyTertiaryEffects(Level level, BlockPos pos, int levels, MobEffect primary, MobEffect secondary, CallbackInfo ci, @Local(ordinal = 2) int j, @Local(ordinal = 0) List<Player> players, @Local(ordinal = 0) AABB aabb) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof BeaconBlockEntity beaconBlockEntity) {
            MobEffect tertiary = ((IBeaconBlockEntityMixin) beaconBlockEntity).better_beacons_getTertiaryPower();

            if (levels >= 5 && primary != tertiary && secondary != tertiary && tertiary != null) {
                for (Player player : players)
                    player.addEffect(new MobEffectInstance(tertiary, j, 0, true, true));

                //Add compatibility with beacons for all mod
                if (BBUtils.isModLoaded(BBConstants.BEACONS_FOR_ALL)){
                    List<LivingEntity> livingEntities = level.getEntitiesOfClass(LivingEntity.class, aabb, BeaconsForAllMod::canApplyEffects);

                    for (LivingEntity livingEntity : livingEntities)
                        livingEntity.addEffect(new MobEffectInstance(tertiary, j, 0, true, true));
                }
            }
        }
    }

    // This captures the first for loop in the target method and changes the condition from j<=4 to j<=5
    @ModifyConstant(method = "updateBase", constant = @Constant(intValue = 4))
    private static int better_beacons_makeBeaconBaseGoesTillLevelFive(int j){
        return j + 1;
    }

    @Inject(method = "createMenu", at = @At("RETURN"), cancellable = true)
    private void better_beacons_addNewBeaconMenu(int containerId, Inventory playerInventory, Player player, @NotNull CallbackInfoReturnable<AbstractContainerMenu> cir){
        cir.setReturnValue(BBUtils.canUnlock(player, this.lockKey, this.getDisplayName()) ? new NewBeaconMenu(containerId, playerInventory, this.dataAccess, this.better_beacons_dataAccess, ContainerLevelAccess.create(Objects.requireNonNull(this.level), this.getBlockPos())) : null);
    }

    @Override
    public String better_beacons_getPaymentItem(){
        return this.better_beacons_PaymentItem;
    }

    @Override
    public MobEffect better_beacons_getTertiaryPower(){
        return this.better_beacons_tertiaryPower;
    }
}
