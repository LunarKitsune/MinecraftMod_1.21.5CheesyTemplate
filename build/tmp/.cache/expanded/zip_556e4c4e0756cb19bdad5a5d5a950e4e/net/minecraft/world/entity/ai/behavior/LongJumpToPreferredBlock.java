package net.minecraft.world.entity.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;

public class LongJumpToPreferredBlock<E extends Mob> extends LongJumpToRandomPos<E> {
    private final TagKey<Block> preferredBlockTag;
    private final float preferredBlocksChance;
    private final List<LongJumpToRandomPos.PossibleJump> notPrefferedJumpCandidates = new ArrayList<>();
    private boolean currentlyWantingPreferredOnes;

    public LongJumpToPreferredBlock(
        UniformInt pTimeBetweenLongJumps,
        int pMaxLongJumpHeight,
        int pMaxLongJumpWidth,
        float pMaxJumpVelocity,
        Function<E, SoundEvent> pGetJumpSound,
        TagKey<Block> pPreferredBlockTag,
        float pPreferredBlocksChance,
        BiPredicate<E, BlockPos> pAcceptableLandingSpot
    ) {
        super(pTimeBetweenLongJumps, pMaxLongJumpHeight, pMaxLongJumpWidth, pMaxJumpVelocity, pGetJumpSound, pAcceptableLandingSpot);
        this.preferredBlockTag = pPreferredBlockTag;
        this.preferredBlocksChance = pPreferredBlocksChance;
    }

    @Override
    protected void start(ServerLevel p_217279_, E p_217280_, long p_217281_) {
        super.start(p_217279_, p_217280_, p_217281_);
        this.notPrefferedJumpCandidates.clear();
        this.currentlyWantingPreferredOnes = p_217280_.getRandom().nextFloat() < this.preferredBlocksChance;
    }

    @Override
    protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel p_217273_) {
        if (!this.currentlyWantingPreferredOnes) {
            return super.getJumpCandidate(p_217273_);
        } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            while (!this.jumpCandidates.isEmpty()) {
                Optional<LongJumpToRandomPos.PossibleJump> optional = super.getJumpCandidate(p_217273_);
                if (optional.isPresent()) {
                    LongJumpToRandomPos.PossibleJump longjumptorandompos$possiblejump = optional.get();
                    if (p_217273_.getBlockState(blockpos$mutableblockpos.setWithOffset(longjumptorandompos$possiblejump.targetPos(), Direction.DOWN))
                        .is(this.preferredBlockTag)) {
                        return optional;
                    }

                    this.notPrefferedJumpCandidates.add(longjumptorandompos$possiblejump);
                }
            }

            return !this.notPrefferedJumpCandidates.isEmpty() ? Optional.of(this.notPrefferedJumpCandidates.remove(0)) : Optional.empty();
        }
    }
}