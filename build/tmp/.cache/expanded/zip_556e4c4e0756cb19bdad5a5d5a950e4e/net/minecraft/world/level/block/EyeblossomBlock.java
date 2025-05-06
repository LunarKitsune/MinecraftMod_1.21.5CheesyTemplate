package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class EyeblossomBlock extends FlowerBlock {
    public static final MapCodec<EyeblossomBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_377554_ -> p_377554_.group(Codec.BOOL.fieldOf("open").forGetter(p_378496_ -> p_378496_.type.open), propertiesCodec())
            .apply(p_377554_, EyeblossomBlock::new)
    );
    private static final int EYEBLOSSOM_XZ_RANGE = 3;
    private static final int EYEBLOSSOM_Y_RANGE = 2;
    private final EyeblossomBlock.Type type;

    @Override
    public MapCodec<? extends EyeblossomBlock> codec() {
        return CODEC;
    }

    public EyeblossomBlock(EyeblossomBlock.Type pType, BlockBehaviour.Properties pProperties) {
        super(pType.effect, pType.effectDuration, pProperties);
        this.type = pType;
    }

    public EyeblossomBlock(boolean pOpen, BlockBehaviour.Properties pProperties) {
        super(EyeblossomBlock.Type.fromBoolean(pOpen).effect, EyeblossomBlock.Type.fromBoolean(pOpen).effectDuration, pProperties);
        this.type = EyeblossomBlock.Type.fromBoolean(pOpen);
    }

    @Override
    public void animateTick(BlockState p_378124_, Level p_378091_, BlockPos p_377687_, RandomSource p_377934_) {
        if (this.type.emitSounds() && p_377934_.nextInt(700) == 0) {
            BlockState blockstate = p_378091_.getBlockState(p_377687_.below());
            if (blockstate.is(Blocks.PALE_MOSS_BLOCK)) {
                p_378091_.playLocalSound(
                    p_377687_.getX(), p_377687_.getY(), p_377687_.getZ(), SoundEvents.EYEBLOSSOM_IDLE, SoundSource.AMBIENT, 1.0F, 1.0F, false
                );
            }
        }
    }

    @Override
    protected void randomTick(BlockState p_377061_, ServerLevel p_376852_, BlockPos p_376526_, RandomSource p_377682_) {
        if (this.tryChangingState(p_377061_, p_376852_, p_376526_, p_377682_)) {
            p_376852_.playSound(null, p_376526_, this.type.transform().longSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.randomTick(p_377061_, p_376852_, p_376526_, p_377682_);
    }

    @Override
    protected void tick(BlockState p_378472_, ServerLevel p_377898_, BlockPos p_376262_, RandomSource p_378553_) {
        if (this.tryChangingState(p_378472_, p_377898_, p_376262_, p_378553_)) {
            p_377898_.playSound(null, p_376262_, this.type.transform().shortSwitchSound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        super.tick(p_378472_, p_377898_, p_376262_, p_378553_);
    }

    private boolean tryChangingState(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.dimensionType().natural()) {
            return false;
        } else if (CreakingHeartBlock.isNaturalNight(pLevel) == this.type.open) {
            return false;
        } else {
            EyeblossomBlock.Type eyeblossomblock$type = this.type.transform();
            pLevel.setBlock(pPos, eyeblossomblock$type.state(), 3);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pState));
            eyeblossomblock$type.spawnTransformParticle(pLevel, pPos, pRandom);
            BlockPos.betweenClosed(pPos.offset(-3, -2, -3), pPos.offset(3, 2, 3)).forEach(p_377124_ -> {
                BlockState blockstate = pLevel.getBlockState(p_377124_);
                if (blockstate == pState) {
                    double d0 = Math.sqrt(pPos.distSqr(p_377124_));
                    int i = pRandom.nextIntBetweenInclusive((int)(d0 * 5.0), (int)(d0 * 10.0));
                    pLevel.scheduleTick(p_377124_, pState.getBlock(), i);
                }
            });
            return true;
        }
    }

    @Override
    protected void entityInside(BlockState p_375775_, Level p_376791_, BlockPos p_376904_, Entity p_376719_, InsideBlockEffectApplier p_391726_) {
        if (!p_376791_.isClientSide()
            && p_376791_.getDifficulty() != Difficulty.PEACEFUL
            && p_376719_ instanceof Bee bee
            && Bee.attractsBees(p_375775_)
            && !bee.hasEffect(MobEffects.POISON)) {
            bee.addEffect(this.getBeeInteractionEffect());
        }
    }

    @Override
    public MobEffectInstance getBeeInteractionEffect() {
        return new MobEffectInstance(MobEffects.POISON, 25);
    }

    public static enum Type {
        OPEN(true, MobEffects.BLINDNESS, 11.0F, SoundEvents.EYEBLOSSOM_OPEN_LONG, SoundEvents.EYEBLOSSOM_OPEN, 16545810),
        CLOSED(false, MobEffects.NAUSEA, 7.0F, SoundEvents.EYEBLOSSOM_CLOSE_LONG, SoundEvents.EYEBLOSSOM_CLOSE, 6250335);

        final boolean open;
        final Holder<MobEffect> effect;
        final float effectDuration;
        final SoundEvent longSwitchSound;
        final SoundEvent shortSwitchSound;
        private final int particleColor;

        private Type(
            final boolean pOpen,
            final Holder<MobEffect> pEffect,
            final float pEffectDuration,
            final SoundEvent pLongSwitchSound,
            final SoundEvent pShortSwitchSound,
            final int pParticleColor
        ) {
            this.open = pOpen;
            this.effect = pEffect;
            this.effectDuration = pEffectDuration;
            this.longSwitchSound = pLongSwitchSound;
            this.shortSwitchSound = pShortSwitchSound;
            this.particleColor = pParticleColor;
        }

        public Block block() {
            return this.open ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM;
        }

        public BlockState state() {
            return this.block().defaultBlockState();
        }

        public EyeblossomBlock.Type transform() {
            return fromBoolean(!this.open);
        }

        public boolean emitSounds() {
            return this.open;
        }

        public static EyeblossomBlock.Type fromBoolean(boolean pOpen) {
            return pOpen ? OPEN : CLOSED;
        }

        public void spawnTransformParticle(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
            Vec3 vec3 = pPos.getCenter();
            double d0 = 0.5 + pRandom.nextDouble();
            Vec3 vec31 = new Vec3(pRandom.nextDouble() - 0.5, pRandom.nextDouble() + 1.0, pRandom.nextDouble() - 0.5);
            Vec3 vec32 = vec3.add(vec31.scale(d0));
            TrailParticleOption trailparticleoption = new TrailParticleOption(vec32, this.particleColor, (int)(20.0 * d0));
            pLevel.sendParticles(trailparticleoption, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        public SoundEvent longSwitchSound() {
            return this.longSwitchSound;
        }
    }
}