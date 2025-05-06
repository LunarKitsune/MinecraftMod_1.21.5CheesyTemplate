package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompassAngleState extends NeedleDirectionHelper {
    public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_377373_ -> p_377373_.group(
                Codec.BOOL.optionalFieldOf("wobble", true).forGetter(NeedleDirectionHelper::wobble),
                CompassAngleState.CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)
            )
            .apply(p_377373_, CompassAngleState::new)
    );
    private final NeedleDirectionHelper.Wobbler wobbler;
    private final NeedleDirectionHelper.Wobbler noTargetWobbler;
    private final CompassAngleState.CompassTarget compassTarget;
    private final RandomSource random = RandomSource.create();

    public CompassAngleState(boolean pWobble, CompassAngleState.CompassTarget pCompassTarget) {
        super(pWobble);
        this.wobbler = this.newWobbler(0.8F);
        this.noTargetWobbler = this.newWobbler(0.8F);
        this.compassTarget = pCompassTarget;
    }

    @Override
    protected float calculate(ItemStack p_376712_, ClientLevel p_377258_, int p_377034_, Entity p_378312_) {
        GlobalPos globalpos = this.compassTarget.get(p_377258_, p_376712_, p_378312_);
        long i = p_377258_.getGameTime();
        return !isValidCompassTargetPos(p_378312_, globalpos) ? this.getRandomlySpinningRotation(p_377034_, i) : this.getRotationTowardsCompassTarget(p_378312_, i, globalpos.pos());
    }

    private float getRandomlySpinningRotation(int pSeed, long pGameTime) {
        if (this.noTargetWobbler.shouldUpdate(pGameTime)) {
            this.noTargetWobbler.update(pGameTime, this.random.nextFloat());
        }

        float f = this.noTargetWobbler.rotation() + hash(pSeed) / 2.1474836E9F;
        return Mth.positiveModulo(f, 1.0F);
    }

    private float getRotationTowardsCompassTarget(Entity pEntity, long pGameTime, BlockPos pTargetPos) {
        float f = (float)getAngleFromEntityToPos(pEntity, pTargetPos);
        float f1 = getWrappedVisualRotationY(pEntity);
        float f2;
        if (pEntity instanceof Player player && player.isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
            if (this.wobbler.shouldUpdate(pGameTime)) {
                this.wobbler.update(pGameTime, 0.5F - (f1 - 0.25F));
            }

            f2 = f + this.wobbler.rotation();
        } else {
            f2 = 0.5F - (f1 - 0.25F - f);
        }

        return Mth.positiveModulo(f2, 1.0F);
    }

    private static boolean isValidCompassTargetPos(Entity pEntity, @Nullable GlobalPos pPos) {
        return pPos != null
            && pPos.dimension() == pEntity.level().dimension()
            && !(pPos.pos().distToCenterSqr(pEntity.position()) < 1.0E-5F);
    }

    private static double getAngleFromEntityToPos(Entity pEntity, BlockPos pPos) {
        Vec3 vec3 = Vec3.atCenterOf(pPos);
        return Math.atan2(vec3.z() - pEntity.getZ(), vec3.x() - pEntity.getX()) / (float) (Math.PI * 2);
    }

    private static float getWrappedVisualRotationY(Entity pEntity) {
        return Mth.positiveModulo(pEntity.getVisualRotationYInDegrees() / 360.0F, 1.0F);
    }

    private static int hash(int pSeed) {
        return pSeed * 1327217883;
    }

    protected CompassAngleState.CompassTarget target() {
        return this.compassTarget;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum CompassTarget implements StringRepresentable {
        NONE("none") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_375914_, ItemStack p_376028_, Entity p_377752_) {
                return null;
            }
        },
        LODESTONE("lodestone") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_376563_, ItemStack p_377548_, Entity p_378126_) {
                LodestoneTracker lodestonetracker = p_377548_.get(DataComponents.LODESTONE_TRACKER);
                return lodestonetracker != null ? lodestonetracker.target().orElse(null) : null;
            }
        },
        SPAWN("spawn") {
            @Override
            public GlobalPos get(ClientLevel p_378146_, ItemStack p_377238_, Entity p_377273_) {
                return GlobalPos.of(p_378146_.dimension(), p_378146_.getSharedSpawnPos());
            }
        },
        RECOVERY("recovery") {
            @Nullable
            @Override
            public GlobalPos get(ClientLevel p_375618_, ItemStack p_378046_, Entity p_376523_) {
                return p_376523_ instanceof Player player ? player.getLastDeathLocation().orElse(null) : null;
            }
        };

        public static final Codec<CompassAngleState.CompassTarget> CODEC = StringRepresentable.fromEnum(CompassAngleState.CompassTarget::values);
        private final String name;

        CompassTarget(final String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        @Nullable
        abstract GlobalPos get(ClientLevel pLevel, ItemStack pStack, Entity pEntity);
    }
}