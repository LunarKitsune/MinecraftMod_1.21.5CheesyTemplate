package net.minecraft.world.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;

public class FoodData {
    private static final int DEFAULT_TICK_TIMER = 0;
    private static final float DEFAULT_EXHAUSTION_LEVEL = 0.0F;
    private int foodLevel = 20;
    private float saturationLevel = 5.0F;
    private float exhaustionLevel;
    private int tickTimer;

    private void add(int pFoodLevel, float pSaturationLevel) {
        this.foodLevel = Mth.clamp(pFoodLevel + this.foodLevel, 0, 20);
        this.saturationLevel = Mth.clamp(pSaturationLevel + this.saturationLevel, 0.0F, this.foodLevel);
    }

    public void eat(int pFoodLevelModifier, float pSaturationLevelModifier) {
        this.add(pFoodLevelModifier, FoodConstants.saturationByModifier(pFoodLevelModifier, pSaturationLevelModifier));
    }

    public void eat(FoodProperties pFoodProperties) {
        this.add(pFoodProperties.nutrition(), pFoodProperties.saturation());
    }

    public void tick(ServerPlayer pPlayer) {
        ServerLevel serverlevel = pPlayer.serverLevel();
        Difficulty difficulty = serverlevel.getDifficulty();
        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        boolean flag = serverlevel.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (flag && this.saturationLevel > 0.0F && pPlayer.isHurt() && this.foodLevel >= 20) {
            this.tickTimer++;
            if (this.tickTimer >= 10) {
                float f = Math.min(this.saturationLevel, 6.0F);
                pPlayer.heal(f / 6.0F);
                this.addExhaustion(f);
                this.tickTimer = 0;
            }
        } else if (flag && this.foodLevel >= 18 && pPlayer.isHurt()) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                pPlayer.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                if (pPlayer.getHealth() > 10.0F || difficulty == Difficulty.HARD || pPlayer.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                    pPlayer.hurtServer(serverlevel, pPlayer.damageSources().starve(), 1.0F);
                }

                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }

    public void readAdditionalSaveData(CompoundTag pCompoundTag) {
        this.foodLevel = pCompoundTag.getIntOr("foodLevel", 20);
        this.tickTimer = pCompoundTag.getIntOr("foodTickTimer", 0);
        this.saturationLevel = pCompoundTag.getFloatOr("foodSaturationLevel", 5.0F);
        this.exhaustionLevel = pCompoundTag.getFloatOr("foodExhaustionLevel", 0.0F);
    }

    public void addAdditionalSaveData(CompoundTag pCompoundTag) {
        pCompoundTag.putInt("foodLevel", this.foodLevel);
        pCompoundTag.putInt("foodTickTimer", this.tickTimer);
        pCompoundTag.putFloat("foodSaturationLevel", this.saturationLevel);
        pCompoundTag.putFloat("foodExhaustionLevel", this.exhaustionLevel);
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public boolean needsFood() {
        return this.foodLevel < 20;
    }

    public void addExhaustion(float pExhaustion) {
        this.exhaustionLevel = Math.min(this.exhaustionLevel + pExhaustion, 40.0F);
    }

    public float getSaturationLevel() {
        return this.saturationLevel;
    }

    public void setFoodLevel(int pFoodLevel) {
        this.foodLevel = pFoodLevel;
    }

    public void setSaturation(float pSaturationLevel) {
        this.saturationLevel = pSaturationLevel;
    }
}