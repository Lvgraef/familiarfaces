package io.github.itskillerluc.familiarfaces.server.entities;

import com.mojang.serialization.Dynamic;
import io.github.itskillerluc.familiarfaces.server.config.Config;
import io.github.itskillerluc.familiarfaces.server.entities.ai.ArmadilloAi;
import io.github.itskillerluc.familiarfaces.server.init.*;
import io.github.itskillerluc.familiarfaces.server.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.IntFunction;

public class Armadillo extends Animal {
    private static final EntityDataSerializer<ArmadilloState> ARMADILLO_STATE_SERIALIZER = EntityDataSerializer.simpleEnum(ArmadilloState.class);
    private static final EntityDataAccessor<ArmadilloState> ARMADILLO_STATE = SynchedEntityData.defineId(
            Armadillo.class, ARMADILLO_STATE_SERIALIZER
    );

    static {
        EntityDataSerializers.registerSerializer(ARMADILLO_STATE_SERIALIZER);
    }

    public final AnimationState rollOutAnimationState = new AnimationState();
    public final AnimationState rollUpAnimationState = new AnimationState();
    public final AnimationState peekAnimationState = new AnimationState();
    private long inStateTicks = 0L;
    private int scuteTime;
    private int brushCooldown;
    private boolean peekReceivedClient = false;

    public Armadillo(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.getNavigation().setCanFloat(true);
        this.scuteTime = this.pickNextScuteDropTime();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.14);
    }

    public static boolean checkArmadilloSpawnRules(
            EntityType<Armadillo> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
    ) {
        return level.getBlockState(pos.below()).is(Tags.Blocks.ARMADILLO_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return EntityTypeRegistry.ARMADILLO.get().create(level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ARMADILLO_STATE, Armadillo.ArmadilloState.IDLE);
    }

    @Override
    public float getEyeHeight(Pose pPose) {
        return 0.26F;
    }

    public boolean isScared() {
        return this.entityData.get(ARMADILLO_STATE) != Armadillo.ArmadilloState.IDLE;
    }

    public boolean shouldHideInShell() {
        return this.getState().shouldHideInShell(this.inStateTicks);
    }

    public boolean shouldSwitchToScaredState() {
        return this.getState() == Armadillo.ArmadilloState.ROLLING && this.inStateTicks > (long) Armadillo.ArmadilloState.ROLLING.animationDuration();
    }

    public Armadillo.ArmadilloState getState() {
        return this.entityData.get(ARMADILLO_STATE);
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    public void switchToState(Armadillo.ArmadilloState state) {
        this.entityData.set(ARMADILLO_STATE, state);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (ARMADILLO_STATE.equals(key)) {
            this.inStateTicks = 0L;
        }

        super.onSyncedDataUpdated(key);
    }

    @Override
    protected Brain.Provider<Armadillo> brainProvider() {
        return ArmadilloAi.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return ArmadilloAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("armadilloBrain");
        ((Brain<Armadillo>) this.brain).tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("armadilloActivityUpdate");
        ArmadilloAi.updateActivity(this);
        this.level().getProfiler().pop();

        if (this.isAlive() && !this.isBaby() && --this.scuteTime <= 0) {
            this.playSound(SoundEventRegistry.ARMADILLO_SCUTE_DROP.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            this.spawnAtLocation(ItemRegistry.ARMADILLO_SCUTE.get());
            this.gameEvent(GameEvent.ENTITY_PLACE);
            this.scuteTime = this.pickNextScuteDropTime();
        }

        super.customServerAiStep();
    }

    private int pickNextScuteDropTime() {
        return this.random.nextInt(6000) + 6000;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }

        if (this.isScared()) {
            this.clampHeadRotationToBody();
        }

        this.inStateTicks++;

        if (this.brushCooldown > 0) {
            this.brushCooldown--;
        }
    }

    protected void clampHeadRotationToBody() {
        float f = (float) this.getMaxHeadYRot();
        float f1 = this.getYHeadRot();
        float f2 = Mth.wrapDegrees(this.yBodyRot - f1);
        float f3 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - f1), -f, f);
        float f4 = f1 + f2 - f3;
        this.setYHeadRot(f4);
    }

    @Override
    public float getScale() {
        return this.isBaby() ? 0.6F : 1.0F;
    }

    private void setupAnimationStates() {
        switch (this.getState()) {
            case IDLE:
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.stop();
                this.peekAnimationState.stop();
                break;
            case ROLLING:
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.startIfStopped(this.tickCount);
                this.peekAnimationState.stop();
                break;
            case SCARED:
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.stop();
                if (this.peekReceivedClient) {
                    this.peekAnimationState.stop();
                    this.peekReceivedClient = false;
                }

                if (this.inStateTicks == 0L) {
                    this.peekAnimationState.start(this.tickCount);
                    Util.fastForward(peekAnimationState, Armadillo.ArmadilloState.SCARED.animationDuration(), 1.0F);
                } else {
                    this.peekAnimationState.startIfStopped(this.tickCount);
                }
                break;
            case UNROLLING:
                this.rollOutAnimationState.startIfStopped(this.tickCount);
                this.rollUpAnimationState.stop();
                this.peekAnimationState.stop();
        }
    }

    public void handleEntityEvent(byte id) {
        if (id == 64 && this.level().isClientSide) {
            this.peekReceivedClient = true;
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEventRegistry.ARMADILLO_PEEK.get(), this.getSoundSource(), 1.0F, 1.0F, false);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Tags.Items.ARMADILLO_FOOD);
    }

    public boolean isScaredBy(LivingEntity entity) {
        if (!this.getBoundingBox().inflate(7.0, 2.0, 7.0).intersects(entity.getBoundingBox())) {
            return false;
        } else if (entity.getMobType() == MobType.UNDEAD) {
            return true;
        } else if (this.getLastHurtByMob() == entity) {
            return true;
        } else if (entity instanceof Player player) {
            return !player.isSpectator() && (player.isSprinting() || player.isPassenger());
        } else {
            return false;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("state", this.getState().getSerializedName());
        compound.putInt("scute_time", this.scuteTime);
        compound.putInt("brush_cooldown", this.brushCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.switchToState(Armadillo.ArmadilloState.fromName(compound.getString("state")));
        if (compound.contains("scute_time")) {
            this.scuteTime = compound.getInt("scute_time");
        }
        if (compound.contains("brush_cooldown")) {
            this.brushCooldown = compound.getInt("brush_cooldown");
        }
    }

    public void rollUp() {
        if (!this.isScared()) {
            this.stopInPlace();
            this.resetLove();
            this.playSound(SoundEventRegistry.ARMADILLO_ROLL.get());
            this.switchToState(Armadillo.ArmadilloState.ROLLING);
        }
    }

    public void stopInPlace() {
        this.getNavigation().stop();
        this.setXxa(0.0F);
        this.setYya(0.0F);
        this.setSpeed(0.0F);
    }

    public void rollOut() {
        if (this.isScared()) {
            this.playSound(SoundEventRegistry.ARMADILLO_UNROLL_FINISH.get());
            this.switchToState(Armadillo.ArmadilloState.IDLE);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isScared()) {
            amount = (amount - 1.0F) / 2.0F;
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource damageSource, float damageAmount) {
        super.actuallyHurt(damageSource, damageAmount);
        if (!this.isNoAi() && !this.isDeadOrDying()) {
            if (damageSource.getEntity() instanceof LivingEntity) {
                this.getBrain().setMemoryWithExpiry(MemoryModuleTypeRegistry.DANGER_DETECTED_RECENTLY.get(), true, 80L);
                if (this.canStayRolledUp()) {
                    this.rollUp();
                }
            } else if (damageSource.is(Tags.DamageTypes.PANIC_ENVIRONMENTAL_CAUSES)) {
                this.rollOut();
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.getItem() instanceof BrushItem && this.brushOffScute()) {
            itemstack.hurtAndBreak(16, player, e -> {
            });
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return this.isScared() ? InteractionResult.FAIL : super.mobInteract(player, hand);
        }
    }

    @Override
    public void ageUp(int amount, boolean forced) {
        if (this.isBaby() && forced) {
            this.playSound(SoundEventRegistry.ARMADILLO_EAT.get());
        }

        super.ageUp(amount, forced);
    }

    public boolean brushOffScute() {
        if (this.isBaby() || this.brushCooldown > 0) {
            return false;
        } else {
            this.spawnAtLocation(new ItemStack(ItemRegistry.ARMADILLO_SCUTE.get()));
            this.gameEvent(GameEvent.ENTITY_INTERACT);
            this.playSound(SoundEventRegistry.ARMADILLO_BRUSH.get());
            this.brushCooldown = Config.Common.brushingCooldown.get() * 20;
            return true;
        }
    }

    public boolean canStayRolledUp() {
        return !this.isPanicking() && !(isInWaterOrBubble() || isInLava()) && !this.isLeashed() && !this.isPassenger() && !this.isVehicle();
    }
    public boolean isPanicking() {
        if (this.brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)) {
            return this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent();
        } else {
            for (WrappedGoal wrappedgoal : this.goalSelector.getAvailableGoals()) {
                if (wrappedgoal.isRunning() && wrappedgoal.getGoal() instanceof PanicGoal) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public void setInLove(@Nullable Player player) {
        super.setInLove(player);
        this.playSound(SoundEventRegistry.ARMADILLO_EAT.get());
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !this.isScared();
    }


    @Override
    public SoundEvent getEatingSound(ItemStack stack) {
        return SoundEventRegistry.ARMADILLO_EAT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isScared() ? null : SoundEventRegistry.ARMADILLO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEventRegistry.ARMADILLO_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return this.isScared() ? SoundEventRegistry.ARMADILLO_HURT_REDUCED.get() : SoundEventRegistry.ARMADILLO_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(SoundEventRegistry.ARMADILLO_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public int getMaxHeadYRot() {
        return this.isScared() ? 0 : 32;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (!Armadillo.this.isScared()) {
                    super.clientTick();
                }
            }
        };
    }

    public enum ArmadilloState implements StringRepresentable {
        IDLE("idle", false, 0, 0) {
            @Override
            public boolean shouldHideInShell(long p_326483_) {
                return false;
            }
        },
        ROLLING("rolling", true, 10, 1) {
            @Override
            public boolean shouldHideInShell(long p_326211_) {
                return p_326211_ > 5L;
            }
        },
        SCARED("scared", true, 50, 2) {
            @Override
            public boolean shouldHideInShell(long p_326129_) {
                return true;
            }
        },
        UNROLLING("unrolling", true, 30, 3) {
            @Override
            public boolean shouldHideInShell(long p_326371_) {
                return p_326371_ < 26L;
            }
        };

        private static final StringRepresentable.EnumCodec<Armadillo.ArmadilloState> CODEC = StringRepresentable.fromEnum(Armadillo.ArmadilloState::values);
        private static final IntFunction<ArmadilloState> BY_ID = ByIdMap.continuous(
                Armadillo.ArmadilloState::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
        );
        private final String name;
        private final boolean isThreatened;
        private final int animationDuration;
        private final int id;

        ArmadilloState(String name, boolean isThreatened, int animationDuration, int id) {
            this.name = name;
            this.isThreatened = isThreatened;
            this.animationDuration = animationDuration;
            this.id = id;
        }

        public static Armadillo.ArmadilloState fromName(String name) {
            return CODEC.byName(name, IDLE);
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }

        private int id() {
            return this.id;
        }

        public abstract boolean shouldHideInShell(long inStateTicks);

        public boolean isThreatened() {
            return this.isThreatened;
        }

        public int animationDuration() {
            return this.animationDuration;
        }
    }
}