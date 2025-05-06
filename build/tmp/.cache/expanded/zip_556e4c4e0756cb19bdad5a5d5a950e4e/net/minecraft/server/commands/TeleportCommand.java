package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.teleport.invalidPosition"));

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(
            Commands.literal("teleport")
                .requires(p_139039_ -> p_139039_.hasPermission(2))
                .then(
                    Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                            p_358635_ -> teleportToPos(
                                p_358635_.getSource(),
                                Collections.singleton(p_358635_.getSource().getEntityOrException()),
                                p_358635_.getSource().getLevel(),
                                Vec3Argument.getCoordinates(p_358635_, "location"),
                                null,
                                null
                            )
                        )
                )
                .then(
                    Commands.argument("destination", EntityArgument.entity())
                        .executes(
                            p_139049_ -> teleportToEntity(
                                p_139049_.getSource(),
                                Collections.singleton(p_139049_.getSource().getEntityOrException()),
                                EntityArgument.getEntity(p_139049_, "destination")
                            )
                        )
                )
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.argument("location", Vec3Argument.vec3())
                                .executes(
                                    p_358636_ -> teleportToPos(
                                        p_358636_.getSource(),
                                        EntityArgument.getEntities(p_358636_, "targets"),
                                        p_358636_.getSource().getLevel(),
                                        Vec3Argument.getCoordinates(p_358636_, "location"),
                                        null,
                                        null
                                    )
                                )
                                .then(
                                    Commands.argument("rotation", RotationArgument.rotation())
                                        .executes(
                                            p_358639_ -> teleportToPos(
                                                p_358639_.getSource(),
                                                EntityArgument.getEntities(p_358639_, "targets"),
                                                p_358639_.getSource().getLevel(),
                                                Vec3Argument.getCoordinates(p_358639_, "location"),
                                                RotationArgument.getRotation(p_358639_, "rotation"),
                                                null
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("facing")
                                        .then(
                                            Commands.literal("entity")
                                                .then(
                                                    Commands.argument("facingEntity", EntityArgument.entity())
                                                        .executes(
                                                            p_358640_ -> teleportToPos(
                                                                p_358640_.getSource(),
                                                                EntityArgument.getEntities(p_358640_, "targets"),
                                                                p_358640_.getSource().getLevel(),
                                                                Vec3Argument.getCoordinates(p_358640_, "location"),
                                                                null,
                                                                new LookAt.LookAtEntity(
                                                                    EntityArgument.getEntity(p_358640_, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                                )
                                                            )
                                                        )
                                                        .then(
                                                            Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                                .executes(
                                                                    p_358638_ -> teleportToPos(
                                                                        p_358638_.getSource(),
                                                                        EntityArgument.getEntities(p_358638_, "targets"),
                                                                        p_358638_.getSource().getLevel(),
                                                                        Vec3Argument.getCoordinates(p_358638_, "location"),
                                                                        null,
                                                                        new LookAt.LookAtEntity(
                                                                            EntityArgument.getEntity(p_358638_, "facingEntity"),
                                                                            EntityAnchorArgument.getAnchor(p_358638_, "facingAnchor")
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.argument("facingLocation", Vec3Argument.vec3())
                                                .executes(
                                                    p_358637_ -> teleportToPos(
                                                        p_358637_.getSource(),
                                                        EntityArgument.getEntities(p_358637_, "targets"),
                                                        p_358637_.getSource().getLevel(),
                                                        Vec3Argument.getCoordinates(p_358637_, "location"),
                                                        null,
                                                        new LookAt.LookAtPosition(Vec3Argument.getVec3(p_358637_, "facingLocation"))
                                                    )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.argument("destination", EntityArgument.entity())
                                .executes(
                                    p_139011_ -> teleportToEntity(
                                        p_139011_.getSource(), EntityArgument.getEntities(p_139011_, "targets"), EntityArgument.getEntity(p_139011_, "destination")
                                    )
                                )
                        )
                )
        );
        pDispatcher.register(Commands.literal("tp").requires(p_139013_ -> p_139013_.hasPermission(2)).redirect(literalcommandnode));
    }

    private static int teleportToEntity(CommandSourceStack pSource, Collection<? extends Entity> pTargets, Entity pDestination) throws CommandSyntaxException {
        for (Entity entity : pTargets) {
            performTeleport(
                pSource,
                entity,
                (ServerLevel)pDestination.level(),
                pDestination.getX(),
                pDestination.getY(),
                pDestination.getZ(),
                EnumSet.noneOf(Relative.class),
                pDestination.getYRot(),
                pDestination.getXRot(),
                null
            );
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable("commands.teleport.success.entity.single", pTargets.iterator().next().getDisplayName(), pDestination.getDisplayName()), true
            );
        } else {
            pSource.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", pTargets.size(), pDestination.getDisplayName()), true);
        }

        return pTargets.size();
    }

    private static int teleportToPos(
        CommandSourceStack pSource,
        Collection<? extends Entity> pTargets,
        ServerLevel pLevel,
        Coordinates pPosition,
        @Nullable Coordinates pRotation,
        @Nullable LookAt pLookAt
    ) throws CommandSyntaxException {
        Vec3 vec3 = pPosition.getPosition(pSource);
        Vec2 vec2 = pRotation == null ? null : pRotation.getRotation(pSource);

        for (Entity entity : pTargets) {
            Set<Relative> set = getRelatives(pPosition, pRotation, entity.level().dimension() == pLevel.dimension());
            if (vec2 == null) {
                performTeleport(pSource, entity, pLevel, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), pLookAt);
            } else {
                performTeleport(pSource, entity, pLevel, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, pLookAt);
            }
        }

        if (pTargets.size() == 1) {
            pSource.sendSuccess(
                () -> Component.translatable(
                    "commands.teleport.success.location.single",
                    pTargets.iterator().next().getDisplayName(),
                    formatDouble(vec3.x),
                    formatDouble(vec3.y),
                    formatDouble(vec3.z)
                ),
                true
            );
        } else {
            pSource.sendSuccess(
                () -> Component.translatable(
                    "commands.teleport.success.location.multiple",
                    pTargets.size(),
                    formatDouble(vec3.x),
                    formatDouble(vec3.y),
                    formatDouble(vec3.z)
                ),
                true
            );
        }

        return pTargets.size();
    }

    private static Set<Relative> getRelatives(Coordinates pPosition, @Nullable Coordinates pRotation, boolean pAbsolute) {
        Set<Relative> set = EnumSet.noneOf(Relative.class);
        if (pPosition.isXRelative()) {
            set.add(Relative.DELTA_X);
            if (pAbsolute) {
                set.add(Relative.X);
            }
        }

        if (pPosition.isYRelative()) {
            set.add(Relative.DELTA_Y);
            if (pAbsolute) {
                set.add(Relative.Y);
            }
        }

        if (pPosition.isZRelative()) {
            set.add(Relative.DELTA_Z);
            if (pAbsolute) {
                set.add(Relative.Z);
            }
        }

        if (pRotation == null || pRotation.isXRelative()) {
            set.add(Relative.X_ROT);
        }

        if (pRotation == null || pRotation.isYRelative()) {
            set.add(Relative.Y_ROT);
        }

        return set;
    }

    private static String formatDouble(double pValue) {
        return String.format(Locale.ROOT, "%f", pValue);
    }

    private static void performTeleport(
        CommandSourceStack pSource,
        Entity pTarget,
        ServerLevel pLevel,
        double pX,
        double pY,
        double pZ,
        Set<Relative> pRelatives,
        float pYRot,
        float pXRot,
        @Nullable LookAt pLookAt
    ) throws CommandSyntaxException {
        var event = net.minecraftforge.event.ForgeEventFactory.onEntityTeleportCommand(pTarget, pX, pY, pZ);
        if (event.isCanceled()) return;
        pX = event.getTargetX(); pY = event.getTargetY(); pZ = event.getTargetZ();
        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            double d0 = pRelatives.contains(Relative.X) ? pX - pTarget.getX() : pX;
            double d1 = pRelatives.contains(Relative.Y) ? pY - pTarget.getY() : pY;
            double d2 = pRelatives.contains(Relative.Z) ? pZ - pTarget.getZ() : pZ;
            float f = pRelatives.contains(Relative.Y_ROT) ? pYRot - pTarget.getYRot() : pYRot;
            float f1 = pRelatives.contains(Relative.X_ROT) ? pXRot - pTarget.getXRot() : pXRot;
            float f2 = Mth.wrapDegrees(f);
            float f3 = Mth.wrapDegrees(f1);
            if (pTarget.teleportTo(pLevel, d0, d1, d2, pRelatives, f2, f3, true)) {
                if (pLookAt != null) {
                    pLookAt.perform(pSource, pTarget);
                }

                if (!(pTarget instanceof LivingEntity livingentity && livingentity.isFallFlying())) {
                    pTarget.setDeltaMovement(pTarget.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    pTarget.setOnGround(true);
                }

                if (pTarget instanceof PathfinderMob pathfindermob) {
                    pathfindermob.getNavigation().stop();
                }
            }
        }
    }
}
