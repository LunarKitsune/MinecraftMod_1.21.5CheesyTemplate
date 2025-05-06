package net.minecraft.world.entity.player;

import net.minecraft.nbt.CompoundTag;

public class Abilities {
    private static final boolean DEFAULT_INVULNERABLE = false;
    private static final boolean DEFAULY_FLYING = false;
    private static final boolean DEFAULT_MAY_FLY = false;
    private static final boolean DEFAULT_INSTABUILD = false;
    private static final boolean DEFAULT_MAY_BUILD = true;
    private static final float DEFAULT_FLYING_SPEED = 0.05F;
    private static final float DEFAULT_WALKING_SPEED = 0.1F;
    public boolean invulnerable;
    public boolean flying;
    public boolean mayfly;
    public boolean instabuild;
    public boolean mayBuild = true;
    private float flyingSpeed = 0.05F;
    private float walkingSpeed = 0.1F;

    public void addSaveData(CompoundTag pCompound) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putBoolean("invulnerable", this.invulnerable);
        compoundtag.putBoolean("flying", this.flying);
        compoundtag.putBoolean("mayfly", this.mayfly);
        compoundtag.putBoolean("instabuild", this.instabuild);
        compoundtag.putBoolean("mayBuild", this.mayBuild);
        compoundtag.putFloat("flySpeed", this.flyingSpeed);
        compoundtag.putFloat("walkSpeed", this.walkingSpeed);
        pCompound.put("abilities", compoundtag);
    }

    public void loadSaveData(CompoundTag pCompound) {
        CompoundTag compoundtag = pCompound.getCompoundOrEmpty("abilities");
        this.invulnerable = compoundtag.getBooleanOr("invulnerable", false);
        this.flying = compoundtag.getBooleanOr("flying", false);
        this.mayfly = compoundtag.getBooleanOr("mayfly", false);
        this.instabuild = compoundtag.getBooleanOr("instabuild", false);
        this.flyingSpeed = compoundtag.getFloatOr("flySpeed", 0.05F);
        this.walkingSpeed = compoundtag.getFloatOr("walkSpeed", 0.1F);
        this.mayBuild = compoundtag.getBooleanOr("mayBuild", true);
    }

    public float getFlyingSpeed() {
        return this.flyingSpeed;
    }

    public void setFlyingSpeed(float pFlyingSpeed) {
        this.flyingSpeed = pFlyingSpeed;
    }

    public float getWalkingSpeed() {
        return this.walkingSpeed;
    }

    public void setWalkingSpeed(float pWalkingSpeed) {
        this.walkingSpeed = pWalkingSpeed;
    }
}