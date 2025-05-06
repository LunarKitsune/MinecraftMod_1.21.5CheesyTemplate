package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.HashedStack;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSampleSubscriptionPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FutureChain;
import net.minecraft.util.Mth;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.StringUtil;
import net.minecraft.util.TickThrottler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl
    extends ServerCommonPacketListenerImpl
    implements GameProtocols.Context,
    ServerGamePacketListener,
    ServerPlayerConnection,
    TickablePacketListener {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_BLOCK_UPDATES_TO_ACK = -1;
    private static final int TRACKED_MESSAGE_DISCONNECT_THRESHOLD = 4096;
    private static final int MAXIMUM_FLYING_TICKS = 80;
    private static final Component CHAT_VALIDATION_FAILED = Component.translatable("multiplayer.disconnect.chat_validation_failed");
    private static final Component INVALID_COMMAND_SIGNATURE = Component.translatable("chat.disabled.invalid_command_signature").withStyle(ChatFormatting.RED);
    private static final int MAX_COMMAND_SUGGESTIONS = 1000;
    public ServerPlayer player;
    public final PlayerChunkSender chunkSender;
    private int tickCount;
    private int ackBlockChangesUpTo = -1;
    private final TickThrottler chatSpamThrottler = new TickThrottler(20, 200);
    private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1480);
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    @Nullable
    private Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    @Nullable
    private Vec3 awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;
    private boolean receivedMovementThisTick;
    @Nullable
    private RemoteChatSession chatSession;
    private SignedMessageChain.Decoder signedMessageDecoder;
    private final LastSeenMessagesValidator lastSeenMessages = new LastSeenMessagesValidator(20);
    private int nextChatIndex;
    private final MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    private final FutureChain chatMessageChain;
    private boolean waitingForSwitchToConfig;

    public ServerGamePacketListenerImpl(MinecraftServer pServer, Connection pConnection, ServerPlayer pPlayer, CommonListenerCookie pCookie) {
        super(pServer, pConnection, pCookie);
        this.chunkSender = new PlayerChunkSender(pConnection.isMemoryConnection());
        this.player = pPlayer;
        pPlayer.connection = this;
        pPlayer.getTextFilter().join();
        this.signedMessageDecoder = SignedMessageChain.Decoder.unsigned(pPlayer.getUUID(), pServer::enforceSecureProfile);
        this.chatMessageChain = new FutureChain(pServer);
    }

    @Override
    public void tick() {
        if (this.ackBlockChangesUpTo > -1) {
            this.send(new ClientboundBlockChangedAckPacket(this.ackBlockChangesUpTo));
            this.ackBlockChangesUpTo = -1;
        }

        this.resetPosition();
        this.player.xo = this.player.getX();
        this.player.yo = this.player.getY();
        this.player.zo = this.player.getZ();
        this.player.doTick();
        this.player.absSnapTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        this.tickCount++;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger() && !this.player.isDeadOrDying()) {
            if (++this.aboveGroundTickCount > this.getMaximumFlyingTicks(this.player)) {
                LOGGER.warn("{} was kicked for floating too long!", this.player.getName().getString());
                this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
                return;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }

        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle != this.player && this.lastVehicle.getControllingPassenger() == this.player) {
            this.vehicleFirstGoodX = this.lastVehicle.getX();
            this.vehicleFirstGoodY = this.lastVehicle.getY();
            this.vehicleFirstGoodZ = this.lastVehicle.getZ();
            this.vehicleLastGoodX = this.lastVehicle.getX();
            this.vehicleLastGoodY = this.lastVehicle.getY();
            this.vehicleLastGoodZ = this.lastVehicle.getZ();
            if (this.clientVehicleIsFloating && this.lastVehicle.getControllingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > this.getMaximumFlyingTicks(this.lastVehicle)) {
                    LOGGER.warn("{} was kicked for floating a vehicle too long!", this.player.getName().getString());
                    this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
                    return;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        } else {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        }

        this.keepConnectionAlive();
        this.chatSpamThrottler.tick();
        this.dropSpamThrottler.tick();
        if (this.player.getLastActionTime() > 0L
            && this.server.getPlayerIdleTimeout() > 0
            && Util.getMillis() - this.player.getLastActionTime() > this.server.getPlayerIdleTimeout() * 1000L * 60L) {
            this.disconnect(Component.translatable("multiplayer.disconnect.idling"));
        }
    }

    private int getMaximumFlyingTicks(Entity pEntity) {
        double d0 = pEntity.getGravity();
        if (d0 < 1.0E-5F) {
            return Integer.MAX_VALUE;
        } else {
            double d1 = 0.08 / d0;
            return Mth.ceil(80.0 * Math.max(d1, 1.0));
        }
    }

    public void resetPosition() {
        this.firstGoodX = this.player.getX();
        this.firstGoodY = this.player.getY();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodY = this.player.getY();
        this.lastGoodZ = this.player.getZ();
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.waitingForSwitchToConfig;
    }

    @Override
    public boolean shouldHandleMessage(Packet<?> p_298646_) {
        return super.shouldHandleMessage(p_298646_)
            ? true
            : this.waitingForSwitchToConfig && this.connection.isConnected() && p_298646_ instanceof ServerboundConfigurationAcknowledgedPacket;
    }

    @Override
    protected GameProfile playerProfile() {
        return this.player.getGameProfile();
    }

    private <T, R> CompletableFuture<R> filterTextPacket(T pMessage, BiFunction<TextFilter, T, CompletableFuture<R>> pProcessor) {
        return pProcessor.apply(this.player.getTextFilter(), pMessage).thenApply(p_264862_ -> {
            if (!this.isAcceptingMessages()) {
                LOGGER.debug("Ignoring packet due to disconnection");
                throw new CancellationException("disconnected");
            } else {
                return (R)p_264862_;
            }
        });
    }

    private CompletableFuture<FilteredText> filterTextPacket(String pText) {
        return this.filterTextPacket(pText, TextFilter::processStreamMessage);
    }

    private CompletableFuture<List<FilteredText>> filterTextPacket(List<String> pTexts) {
        return this.filterTextPacket(pTexts, TextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.setLastClientInput(pPacket.input());
    }

    private static boolean containsInvalidValues(double pX, double pY, double pZ, float pYRot, float pXRot) {
        return Double.isNaN(pX) || Double.isNaN(pY) || Double.isNaN(pZ) || !Floats.isFinite(pXRot) || !Floats.isFinite(pYRot);
    }

    private static double clampHorizontal(double pValue) {
        return Mth.clamp(pValue, -3.0E7, 3.0E7);
    }

    private static double clampVertical(double pValue) {
        return Mth.clamp(pValue, -2.0E7, 2.0E7);
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (containsInvalidValues(pPacket.position().x(), pPacket.position().y(), pPacket.position().z(), pPacket.yRot(), pPacket.xRot())) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
        } else if (!this.updateAwaitingTeleport() && this.player.hasClientLoaded()) {
            Entity entity = this.player.getRootVehicle();
            if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
                ServerLevel serverlevel = this.player.serverLevel();
                double d0 = entity.getX();
                double d1 = entity.getY();
                double d2 = entity.getZ();
                double d3 = clampHorizontal(pPacket.position().x());
                double d4 = clampVertical(pPacket.position().y());
                double d5 = clampHorizontal(pPacket.position().z());
                float f = Mth.wrapDegrees(pPacket.yRot());
                float f1 = Mth.wrapDegrees(pPacket.xRot());
                double d6 = d3 - this.vehicleFirstGoodX;
                double d7 = d4 - this.vehicleFirstGoodY;
                double d8 = d5 - this.vehicleFirstGoodZ;
                double d9 = entity.getDeltaMovement().lengthSqr();
                double d10 = d6 * d6 + d7 * d7 + d8 * d8;
                if (d10 - d9 > 100.0 && !this.isSingleplayerOwner()) {
                    LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", entity.getName().getString(), this.player.getName().getString(), d6, d7, d8);
                    this.send(ClientboundMoveVehiclePacket.fromEntity(entity));
                    return;
                }

                boolean flag = serverlevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                d6 = d3 - this.vehicleLastGoodX;
                d7 = d4 - this.vehicleLastGoodY;
                d8 = d5 - this.vehicleLastGoodZ;
                boolean flag1 = entity.verticalCollisionBelow;
                if (entity instanceof LivingEntity livingentity && livingentity.onClimbable()) {
                    livingentity.resetFallDistance();
                }

                entity.move(MoverType.PLAYER, new Vec3(d6, d7, d8));
                d6 = d3 - entity.getX();
                d7 = d4 - entity.getY();
                if (d7 > -0.5 || d7 < 0.5) {
                    d7 = 0.0;
                }

                d8 = d5 - entity.getZ();
                d10 = d6 * d6 + d7 * d7 + d8 * d8;
                boolean flag2 = false;
                if (d10 > 0.0625) {
                    flag2 = true;
                    LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", entity.getName().getString(), this.player.getName().getString(), Math.sqrt(d10));
                }

                entity.absSnapTo(d3, d4, d5, f, f1);
                boolean flag3 = serverlevel.noCollision(entity, entity.getBoundingBox().deflate(0.0625));
                if (flag && (flag2 || !flag3)) {
                    entity.absSnapTo(d0, d1, d2, f, f1);
                    this.send(ClientboundMoveVehiclePacket.fromEntity(entity));
                    entity.removeLatestMovementRecordingBatch();
                    return;
                }

                this.player.serverLevel().getChunkSource().move(this.player);
                Vec3 vec3 = new Vec3(entity.getX() - d0, entity.getY() - d1, entity.getZ() - d2);
                this.handlePlayerKnownMovement(vec3);
                entity.setOnGroundWithMovement(pPacket.onGround(), vec3);
                entity.doCheckFallDamage(vec3.x, vec3.y, vec3.z, pPacket.onGround());
                this.player.checkMovementStatistics(vec3.x, vec3.y, vec3.z);
                this.clientVehicleIsFloating = d7 >= -0.03125 && !flag1 && !this.server.isFlightAllowed() && !entity.isNoGravity() && this.noBlocksAround(entity);
                this.vehicleLastGoodX = entity.getX();
                this.vehicleLastGoodY = entity.getY();
                this.vehicleLastGoodZ = entity.getZ();
            }
        }
    }

    private boolean noBlocksAround(Entity pEntity) {
        return pEntity.level().getBlockStates(pEntity.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0)).allMatch(BlockBehaviour.BlockStateBase::isAir);
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (pPacket.getId() == this.awaitingTeleport) {
            if (this.awaitingPositionFromClient == null) {
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
                return;
            }

            this.player.absSnapTo(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            this.player.hasChangedDimension();
            this.awaitingPositionFromClient = null;
        }
    }

    @Override
    public void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket p_376469_) {
        PacketUtils.ensureRunningOnSameThread(p_376469_, this, this.player.serverLevel());
        this.player.setClientLoaded(true);
    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        RecipeManager.ServerDisplayInfo recipemanager$serverdisplayinfo = this.server.getRecipeManager().getRecipeFromDisplay(pPacket.recipe());
        if (recipemanager$serverdisplayinfo != null) {
            this.player.getRecipeBook().removeHighlight(recipemanager$serverdisplayinfo.parent().id());
        }
    }

    @Override
    public void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket p_365497_) {
        PacketUtils.ensureRunningOnSameThread(p_365497_, this, this.player.serverLevel());
        this.player.containerMenu.setSelectedBundleItemIndex(p_365497_.slotId(), p_365497_.selectedItemIndex());
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket p_9895_) {
        PacketUtils.ensureRunningOnSameThread(p_9895_, this, this.player.serverLevel());
        this.player.getRecipeBook().setBookSetting(p_9895_.getBookType(), p_9895_.isOpen(), p_9895_.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (pPacket.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            ResourceLocation resourcelocation = Objects.requireNonNull(pPacket.getTab());
            AdvancementHolder advancementholder = this.server.getAdvancements().get(resourcelocation);
            if (advancementholder != null) {
                this.player.getAdvancements().setSelectedTab(advancementholder);
            }
        }
    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        StringReader stringreader = new StringReader(pPacket.getCommand());
        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }

        ParseResults<CommandSourceStack> parseresults = this.server.getCommands().getDispatcher().parse(stringreader, this.player.createCommandSourceStack());
        this.server
            .getCommands()
            .getDispatcher()
            .getCompletionSuggestions(parseresults)
            .thenAccept(
                p_326457_ -> {
                    Suggestions suggestions = p_326457_.getList().size() <= 1000
                        ? p_326457_
                        : new Suggestions(p_326457_.getRange(), p_326457_.getList().subList(0, 1000));
                    this.send(new ClientboundCommandSuggestionsPacket(pPacket.getId(), suggestions));
                }
            );
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notEnabled"));
        } else if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
        } else {
            BaseCommandBlock basecommandblock = null;
            CommandBlockEntity commandblockentity = null;
            BlockPos blockpos = pPacket.getPos();
            BlockEntity blockentity = this.player.level().getBlockEntity(blockpos);
            if (blockentity instanceof CommandBlockEntity) {
                commandblockentity = (CommandBlockEntity)blockentity;
                basecommandblock = commandblockentity.getCommandBlock();
            }

            String s = pPacket.getCommand();
            boolean flag = pPacket.isTrackOutput();
            if (basecommandblock != null) {
                CommandBlockEntity.Mode commandblockentity$mode = commandblockentity.getMode();
                BlockState blockstate = this.player.level().getBlockState(blockpos);
                Direction direction = blockstate.getValue(CommandBlock.FACING);

                BlockState blockstate1 = switch (pPacket.getMode()) {
                    case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
                    case AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
                    default -> Blocks.COMMAND_BLOCK.defaultBlockState();
                };
                BlockState blockstate2 = blockstate1.setValue(CommandBlock.FACING, direction).setValue(CommandBlock.CONDITIONAL, pPacket.isConditional());
                if (blockstate2 != blockstate) {
                    this.player.level().setBlock(blockpos, blockstate2, 2);
                    blockentity.setBlockState(blockstate2);
                    this.player.level().getChunkAt(blockpos).setBlockEntity(blockentity);
                }

                basecommandblock.setCommand(s);
                basecommandblock.setTrackOutput(flag);
                if (!flag) {
                    basecommandblock.setLastOutput(null);
                }

                commandblockentity.setAutomatic(pPacket.isAutomatic());
                if (commandblockentity$mode != pPacket.getMode()) {
                    commandblockentity.onModeSwitch();
                }

                basecommandblock.onUpdated();
                if (!StringUtil.isNullOrEmpty(s)) {
                    this.player.sendSystemMessage(Component.translatable("advMode.setCommand.success", s));
                }
            }
        }
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (!this.server.isCommandBlockEnabled()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notEnabled"));
        } else if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
        } else {
            BaseCommandBlock basecommandblock = pPacket.getCommandBlock(this.player.level());
            if (basecommandblock != null) {
                basecommandblock.setCommand(pPacket.getCommand());
                basecommandblock.setTrackOutput(pPacket.isTrackOutput());
                if (!pPacket.isTrackOutput()) {
                    basecommandblock.setLastOutput(null);
                }

                basecommandblock.onUpdated();
                this.player.sendSystemMessage(Component.translatable("advMode.setCommand.success", pPacket.getCommand()));
            }
        }
    }

    @Override
    public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket p_376361_) {
        ServerLevel serverlevel = this.player.serverLevel();
        PacketUtils.ensureRunningOnSameThread(p_376361_, this, serverlevel);
        BlockPos blockpos = p_376361_.pos();
        if (this.player.canInteractWithBlock(blockpos, 1.0)) {
            if (serverlevel.isLoaded(blockpos)) {
                BlockState blockstate = serverlevel.getBlockState(blockpos);
                boolean flag = this.player.hasInfiniteMaterials() && p_376361_.includeData();
                ItemStack itemstack = blockstate.getCloneItemStack(serverlevel, blockpos, flag);
                if (!itemstack.isEmpty()) {
                    if (flag) {
                        addBlockDataToItem(blockstate, serverlevel, blockpos, itemstack);
                    }

                    this.tryPickItem(itemstack);
                }
            }
        }
    }

    private static void addBlockDataToItem(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack) {
        BlockEntity blockentity = pState.hasBlockEntity() ? pLevel.getBlockEntity(pPos) : null;
        if (blockentity != null) {
            CompoundTag compoundtag = blockentity.saveCustomOnly(pLevel.registryAccess());
            blockentity.removeComponentsFromTag(compoundtag);
            BlockItem.setBlockEntityData(pStack, blockentity.getType(), compoundtag);
            pStack.applyComponents(blockentity.collectComponents());
        }
    }

    @Override
    public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket p_375821_) {
        ServerLevel serverlevel = this.player.serverLevel();
        PacketUtils.ensureRunningOnSameThread(p_375821_, this, serverlevel);
        Entity entity = serverlevel.getEntityOrPart(p_375821_.id());
        if (entity != null && this.player.canInteractWithEntity(entity, 3.0)) {
            ItemStack itemstack = entity.getPickResult();
            if (itemstack != null && !itemstack.isEmpty()) {
                this.tryPickItem(itemstack);
            }
        }
    }

    private void tryPickItem(ItemStack pStack) {
        if (pStack.isItemEnabled(this.player.level().enabledFeatures())) {
            Inventory inventory = this.player.getInventory();
            int i = inventory.findSlotMatchingItem(pStack);
            if (i != -1) {
                if (Inventory.isHotbarSlot(i)) {
                    inventory.setSelectedSlot(i);
                } else {
                    inventory.pickSlot(i);
                }
            } else if (this.player.hasInfiniteMaterials()) {
                inventory.addAndPickItem(pStack);
            }

            this.player.connection.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
            this.player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.containerMenu instanceof AnvilMenu anvilmenu) {
            if (!anvilmenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, anvilmenu);
                return;
            }

            anvilmenu.setItemName(pPacket.getName());
        }
    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.containerMenu instanceof BeaconMenu beaconmenu) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
                return;
            }

            beaconmenu.updateEffects(pPacket.primary(), pPacket.secondary());
        }
    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockpos = pPacket.getPos();
            BlockState blockstate = this.player.level().getBlockState(blockpos);
            if (this.player.level().getBlockEntity(blockpos) instanceof StructureBlockEntity structureblockentity) {
                structureblockentity.setMode(pPacket.getMode());
                structureblockentity.setStructureName(pPacket.getName());
                structureblockentity.setStructurePos(pPacket.getOffset());
                structureblockentity.setStructureSize(pPacket.getSize());
                structureblockentity.setMirror(pPacket.getMirror());
                structureblockentity.setRotation(pPacket.getRotation());
                structureblockentity.setMetaData(pPacket.getData());
                structureblockentity.setIgnoreEntities(pPacket.isIgnoreEntities());
                structureblockentity.setStrict(pPacket.isStrict());
                structureblockentity.setShowAir(pPacket.isShowAir());
                structureblockentity.setShowBoundingBox(pPacket.isShowBoundingBox());
                structureblockentity.setIntegrity(pPacket.getIntegrity());
                structureblockentity.setSeed(pPacket.getSeed());
                if (structureblockentity.hasStructureName()) {
                    String s = structureblockentity.getStructureName();
                    if (pPacket.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                        if (structureblockentity.saveStructure()) {
                            this.player.displayClientMessage(Component.translatable("structure_block.save_success", s), false);
                        } else {
                            this.player.displayClientMessage(Component.translatable("structure_block.save_failure", s), false);
                        }
                    } else if (pPacket.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                        if (!structureblockentity.isStructureLoadable()) {
                            this.player.displayClientMessage(Component.translatable("structure_block.load_not_found", s), false);
                        } else if (structureblockentity.placeStructureIfSameSize(this.player.serverLevel())) {
                            this.player.displayClientMessage(Component.translatable("structure_block.load_success", s), false);
                        } else {
                            this.player.displayClientMessage(Component.translatable("structure_block.load_prepare", s), false);
                        }
                    } else if (pPacket.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                        if (structureblockentity.detectSize()) {
                            this.player.displayClientMessage(Component.translatable("structure_block.size_success", s), false);
                        } else {
                            this.player.displayClientMessage(Component.translatable("structure_block.size_failure"), false);
                        }
                    }
                } else {
                    this.player.displayClientMessage(Component.translatable("structure_block.invalid_structure_name", pPacket.getName()), false);
                }

                structureblockentity.setChanged();
                this.player.level().sendBlockUpdated(blockpos, blockstate, blockstate, 3);
            }
        }
    }

    @Override
    public void handleSetTestBlock(ServerboundSetTestBlockPacket p_397113_) {
        PacketUtils.ensureRunningOnSameThread(p_397113_, this, this.player.serverLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockpos = p_397113_.position();
            BlockState blockstate = this.player.level().getBlockState(blockpos);
            if (this.player.level().getBlockEntity(blockpos) instanceof TestBlockEntity testblockentity) {
                testblockentity.setMode(p_397113_.mode());
                testblockentity.setMessage(p_397113_.message());
                testblockentity.setChanged();
                this.player.level().sendBlockUpdated(blockpos, blockstate, testblockentity.getBlockState(), 3);
            }
        }
    }

    @Override
    public void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket p_393678_) {
        PacketUtils.ensureRunningOnSameThread(p_393678_, this, this.player.serverLevel());
        BlockPos blockpos = p_393678_.pos();
        if (this.player.canUseGameMasterBlocks() && this.player.level().getBlockEntity(blockpos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            if (p_393678_.action() != ServerboundTestInstanceBlockActionPacket.Action.QUERY
                && p_393678_.action() != ServerboundTestInstanceBlockActionPacket.Action.INIT) {
                testinstanceblockentity.set(p_393678_.data());
                if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.RESET) {
                    testinstanceblockentity.resetTest(this.player::sendSystemMessage);
                } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.SAVE) {
                    testinstanceblockentity.saveTest(this.player::sendSystemMessage);
                } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.EXPORT) {
                    testinstanceblockentity.exportTest(this.player::sendSystemMessage);
                } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.RUN) {
                    testinstanceblockentity.runTest(this.player::sendSystemMessage);
                }

                BlockState blockstate = this.player.level().getBlockState(blockpos);
                this.player.level().sendBlockUpdated(blockpos, Blocks.AIR.defaultBlockState(), blockstate, 3);
            } else {
                Registry<GameTestInstance> registry = this.player.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
                Optional<Holder.Reference<GameTestInstance>> optional = p_393678_.data().test().flatMap(registry::get);
                Component component;
                if (optional.isPresent()) {
                    component = optional.get().value().describe();
                } else {
                    component = Component.translatable("test_instance.description.no_test").withStyle(ChatFormatting.RED);
                }

                Optional<Vec3i> optional1;
                if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.QUERY) {
                    optional1 = p_393678_.data()
                        .test()
                        .flatMap(p_390155_ -> TestInstanceBlockEntity.getStructureSize(this.player.serverLevel(), (ResourceKey<GameTestInstance>)p_390155_));
                } else {
                    optional1 = Optional.empty();
                }

                this.connection.send(new ClientboundTestInstanceBlockStatus(component, optional1));
            }
        }
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket p_9917_) {
        PacketUtils.ensureRunningOnSameThread(p_9917_, this, this.player.serverLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockpos = p_9917_.getPos();
            BlockState blockstate = this.player.level().getBlockState(blockpos);
            if (this.player.level().getBlockEntity(blockpos) instanceof JigsawBlockEntity jigsawblockentity) {
                jigsawblockentity.setName(p_9917_.getName());
                jigsawblockentity.setTarget(p_9917_.getTarget());
                jigsawblockentity.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, p_9917_.getPool()));
                jigsawblockentity.setFinalState(p_9917_.getFinalState());
                jigsawblockentity.setJoint(p_9917_.getJoint());
                jigsawblockentity.setPlacementPriority(p_9917_.getPlacementPriority());
                jigsawblockentity.setSelectionPriority(p_9917_.getSelectionPriority());
                jigsawblockentity.setChanged();
                this.player.level().sendBlockUpdated(blockpos, blockstate, blockstate, 3);
            }
        }
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket p_9868_) {
        PacketUtils.ensureRunningOnSameThread(p_9868_, this, this.player.serverLevel());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockpos = p_9868_.getPos();
            if (this.player.level().getBlockEntity(blockpos) instanceof JigsawBlockEntity jigsawblockentity) {
                jigsawblockentity.generate(this.player.serverLevel(), p_9868_.levels(), p_9868_.keepJigsaws());
            }
        }
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        int i = pPacket.getItem();
        if (this.player.containerMenu instanceof MerchantMenu merchantmenu) {
            if (!merchantmenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, merchantmenu);
                return;
            }

            merchantmenu.setSelectionHint(i);
            merchantmenu.tryMoveItems(i);
        }
    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket pPacket) {
        int i = pPacket.slot();
        if (Inventory.isHotbarSlot(i) || i == 40) {
            List<String> list = Lists.newArrayList();
            Optional<String> optional = pPacket.title();
            optional.ifPresent(list::add);
            list.addAll(pPacket.pages());
            Consumer<List<FilteredText>> consumer = optional.isPresent()
                ? p_238198_ -> this.signBook(p_238198_.get(0), p_238198_.subList(1, p_238198_.size()), i)
                : p_143627_ -> this.updateBookContents(p_143627_, i);
            this.filterTextPacket(list).thenAcceptAsync(consumer, this.server);
        }
    }

    private void updateBookContents(List<FilteredText> pPages, int pIndex) {
        ItemStack itemstack = this.player.getInventory().getItem(pIndex);
        if (itemstack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            List<Filterable<String>> list = pPages.stream().map(this::filterableFromOutgoing).toList();
            itemstack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(list));
        }
    }

    private void signBook(FilteredText pTitle, List<FilteredText> pPages, int pIndex) {
        ItemStack itemstack = this.player.getInventory().getItem(pIndex);
        if (itemstack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            ItemStack itemstack1 = itemstack.transmuteCopy(Items.WRITTEN_BOOK);
            itemstack1.remove(DataComponents.WRITABLE_BOOK_CONTENT);
            List<Filterable<Component>> list = pPages.stream().map(p_326455_ -> this.filterableFromOutgoing(p_326455_).<Component>map(Component::literal)).toList();
            itemstack1.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(this.filterableFromOutgoing(pTitle), this.player.getName().getString(), 0, list, true));
            this.player.getInventory().setItem(pIndex, itemstack1);
        }
    }

    private Filterable<String> filterableFromOutgoing(FilteredText pFilteredText) {
        return this.player.isTextFilteringEnabled() ? Filterable.passThrough(pFilteredText.filteredOrEmpty()) : Filterable.from(pFilteredText);
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQueryPacket p_328694_) {
        PacketUtils.ensureRunningOnSameThread(p_328694_, this, this.player.serverLevel());
        if (this.player.hasPermissions(2)) {
            Entity entity = this.player.level().getEntity(p_328694_.getEntityId());
            if (entity != null) {
                CompoundTag compoundtag = entity.saveWithoutId(new CompoundTag());
                this.player.connection.send(new ClientboundTagQueryPacket(p_328694_.getTransactionId(), compoundtag));
            }
        }
    }

    @Override
    public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket p_309524_) {
        PacketUtils.ensureRunningOnSameThread(p_309524_, this, this.player.serverLevel());
        if (!this.player.isSpectator() && p_309524_.containerId() == this.player.containerMenu.containerId) {
            if (this.player.containerMenu instanceof CrafterMenu craftermenu && craftermenu.getContainer() instanceof CrafterBlockEntity crafterblockentity) {
                crafterblockentity.setSlotState(p_309524_.slotId(), p_309524_.newState());
            }
        }
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket p_330554_) {
        PacketUtils.ensureRunningOnSameThread(p_330554_, this, this.player.serverLevel());
        if (this.player.hasPermissions(2)) {
            BlockEntity blockentity = this.player.level().getBlockEntity(p_330554_.getPos());
            CompoundTag compoundtag = blockentity != null ? blockentity.saveWithoutMetadata(this.player.registryAccess()) : null;
            this.player.connection.send(new ClientboundTagQueryPacket(p_330554_.getTransactionId(), compoundtag));
        }
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (containsInvalidValues(pPacket.getX(0.0), pPacket.getY(0.0), pPacket.getZ(0.0), pPacket.getYRot(0.0F), pPacket.getXRot(0.0F))) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
        } else {
            ServerLevel serverlevel = this.player.serverLevel();
            if (!this.player.wonGame) {
                if (this.tickCount == 0) {
                    this.resetPosition();
                }

                if (this.player.hasClientLoaded()) {
                    float f = Mth.wrapDegrees(pPacket.getYRot(this.player.getYRot()));
                    float f1 = Mth.wrapDegrees(pPacket.getXRot(this.player.getXRot()));
                    if (this.updateAwaitingTeleport()) {
                        this.player.absSnapRotationTo(f, f1);
                    } else {
                        double d0 = clampHorizontal(pPacket.getX(this.player.getX()));
                        double d1 = clampVertical(pPacket.getY(this.player.getY()));
                        double d2 = clampHorizontal(pPacket.getZ(this.player.getZ()));
                        if (this.player.isPassenger()) {
                            this.player.absSnapTo(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                            this.player.serverLevel().getChunkSource().move(this.player);
                        } else {
                            double d3 = this.player.getX();
                            double d4 = this.player.getY();
                            double d5 = this.player.getZ();
                            double d6 = d0 - this.firstGoodX;
                            double d7 = d1 - this.firstGoodY;
                            double d8 = d2 - this.firstGoodZ;
                            double d9 = this.player.getDeltaMovement().lengthSqr();
                            double d10 = d6 * d6 + d7 * d7 + d8 * d8;
                            if (this.player.isSleeping()) {
                                if (d10 > 1.0) {
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                                }
                            } else {
                                boolean flag = this.player.isFallFlying();
                                if (serverlevel.tickRateManager().runsNormally()) {
                                    this.receivedMovePacketCount++;
                                    int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                                    if (i > 5) {
                                        LOGGER.debug(
                                            "{} is sending move packets too frequently ({} packets since last tick)", this.player.getName().getString(), i
                                        );
                                        i = 1;
                                    }

                                    if (this.shouldCheckPlayerMovement(flag)) {
                                        float f2 = flag ? 300.0F : 100.0F;
                                        if (d10 - d9 > f2 * i) {
                                            LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getName().getString(), d6, d7, d8);
                                            this.teleport(
                                                this.player.getX(),
                                                this.player.getY(),
                                                this.player.getZ(),
                                                this.player.getYRot(),
                                                this.player.getXRot()
                                            );
                                            return;
                                        }
                                    }
                                }

                                AABB aabb = this.player.getBoundingBox();
                                d6 = d0 - this.lastGoodX;
                                d7 = d1 - this.lastGoodY;
                                d8 = d2 - this.lastGoodZ;
                                boolean flag4 = d7 > 0.0;
                                if (this.player.onGround() && !pPacket.isOnGround() && flag4) {
                                    this.player.jumpFromGround();
                                }

                                boolean flag1 = this.player.verticalCollisionBelow;
                                this.player.move(MoverType.PLAYER, new Vec3(d6, d7, d8));
                                d6 = d0 - this.player.getX();
                                d7 = d1 - this.player.getY();
                                if (d7 > -0.5 || d7 < 0.5) {
                                    d7 = 0.0;
                                }

                                d8 = d2 - this.player.getZ();
                                d10 = d6 * d6 + d7 * d7 + d8 * d8;
                                boolean flag2 = false;
                                if (!this.player.isChangingDimension() && d10 > 0.0625 && !this.player.isSleeping() && !this.player.isCreative() && !this.player.isSpectator()) {
                                    flag2 = true;
                                    LOGGER.warn("{} moved wrongly!", this.player.getName().getString());
                                }

                                if (this.player.noPhysics
                                    || this.player.isSleeping()
                                    || (!flag2 || !serverlevel.noCollision(this.player, aabb)) && !this.isPlayerCollidingWithAnythingNew(serverlevel, aabb, d0, d1, d2)) {
                                    this.player.absSnapTo(d0, d1, d2, f, f1);
                                    boolean flag3 = this.player.isAutoSpinAttack();
                                    this.clientIsFloating = d7 >= -0.03125
                                        && !flag1
                                        && !this.player.isSpectator()
                                        && !this.server.isFlightAllowed()
                                        && !this.player.getAbilities().mayfly
                                        && !this.player.hasEffect(MobEffects.LEVITATION)
                                        && !flag
                                        && !flag3
                                        && this.noBlocksAround(this.player);
                                    this.player.serverLevel().getChunkSource().move(this.player);
                                    Vec3 vec3 = new Vec3(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                                    this.player.setOnGroundWithMovement(pPacket.isOnGround(), pPacket.horizontalCollision(), vec3);
                                    this.player.doCheckFallDamage(vec3.x, vec3.y, vec3.z, pPacket.isOnGround());
                                    this.handlePlayerKnownMovement(vec3);
                                    if (flag4) {
                                        this.player.resetFallDistance();
                                    }

                                    if (pPacket.isOnGround() || this.player.hasLandedInLiquid() || this.player.onClimbable() || this.player.isSpectator() || flag || flag3) {
                                        this.player.tryResetCurrentImpulseContext();
                                    }

                                    this.player.checkMovementStatistics(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                                    this.lastGoodX = this.player.getX();
                                    this.lastGoodY = this.player.getY();
                                    this.lastGoodZ = this.player.getZ();
                                } else {
                                    this.teleport(d3, d4, d5, f, f1);
                                    this.player
                                        .doCheckFallDamage(
                                            this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5, pPacket.isOnGround()
                                        );
                                    this.player.removeLatestMovementRecordingBatch();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldCheckPlayerMovement(boolean pIsElytraMovement) {
        if (this.isSingleplayerOwner()) {
            return false;
        } else if (this.player.isChangingDimension()) {
            return false;
        } else {
            GameRules gamerules = this.player.serverLevel().getGameRules();
            return gamerules.getBoolean(GameRules.RULE_DISABLE_PLAYER_MOVEMENT_CHECK) ? false : !pIsElytraMovement || !gamerules.getBoolean(GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK);
        }
    }

    private boolean updateAwaitingTeleport() {
        if (this.awaitingPositionFromClient != null) {
            if (this.tickCount - this.awaitingTeleportTime > 20) {
                this.awaitingTeleportTime = this.tickCount;
                this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            }

            return true;
        } else {
            this.awaitingTeleportTime = this.tickCount;
            return false;
        }
    }

    private boolean isPlayerCollidingWithAnythingNew(LevelReader pLevel, AABB pBox, double pX, double pY, double pZ) {
        AABB aabb = this.player
            .getBoundingBox()
            .move(pX - this.player.getX(), pY - this.player.getY(), pZ - this.player.getZ());
        Iterable<VoxelShape> iterable = pLevel.getCollisions(this.player, aabb.deflate(1.0E-5F));
        VoxelShape voxelshape = Shapes.create(pBox.deflate(1.0E-5F));

        for (VoxelShape voxelshape1 : iterable) {
            if (!Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.AND)) {
                return true;
            }
        }

        return false;
    }

    public void teleport(double pX, double pY, double pZ, float pYaw, float pPitch) {
        this.teleport(new PositionMoveRotation(new Vec3(pX, pY, pZ), Vec3.ZERO, pYaw, pPitch), Collections.emptySet());
    }

    public void teleport(PositionMoveRotation pPosMoveRotation, Set<Relative> pRelatives) {
        this.awaitingTeleportTime = this.tickCount;
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        this.player.teleportSetPosition(pPosMoveRotation, pRelatives);
        this.awaitingPositionFromClient = this.player.position();
        this.player.connection.send(ClientboundPlayerPositionPacket.of(this.awaitingTeleport, pPosMoveRotation, pRelatives));
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasClientLoaded()) {
            BlockPos blockpos = pPacket.getPos();
            this.player.resetLastActionTime();
            ServerboundPlayerActionPacket.Action serverboundplayeractionpacket$action = pPacket.getAction();
            switch (serverboundplayeractionpacket$action) {
                case SWAP_ITEM_WITH_OFFHAND:
                    if (!this.player.isSpectator()) {
                        var event = net.minecraftforge.event.ForgeEventFactory.onLivingSwapHandItems(this.player);
                        if (event.isCanceled()) return;
                        this.player.setItemInHand(InteractionHand.OFF_HAND, event.getItemSwappedToOffHand());
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, event.getItemSwappedToMainHand());
                        this.player.stopUsingItem();
                    }

                    return;
                case DROP_ITEM:
                    if (!this.player.isSpectator()) {
                        this.player.drop(false);
                    }

                    return;
                case DROP_ALL_ITEMS:
                    if (!this.player.isSpectator()) {
                        this.player.drop(true);
                    }

                    return;
                case RELEASE_USE_ITEM:
                    this.player.releaseUsingItem();
                    return;
                case START_DESTROY_BLOCK:
                case ABORT_DESTROY_BLOCK:
                case STOP_DESTROY_BLOCK:
                    this.player
                        .gameMode
                        .handleBlockBreakAction(blockpos, serverboundplayeractionpacket$action, pPacket.getDirection(), this.player.level().getMaxY(), pPacket.getSequence());
                    this.player.connection.ackBlockChangesUpTo(pPacket.getSequence());
                    return;
                default:
                    throw new IllegalArgumentException("Invalid player action");
            }
        }
    }

    private static boolean wasBlockPlacementAttempt(ServerPlayer pPlayer, ItemStack pStack) {
        if (pStack.isEmpty()) {
            return false;
        } else {
            Item item = pStack.getItem();
            return (item instanceof BlockItem || item instanceof BucketItem) && !pPlayer.getCooldowns().isOnCooldown(pStack);
        }
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasClientLoaded()) {
            this.player.connection.ackBlockChangesUpTo(pPacket.getSequence());
            ServerLevel serverlevel = this.player.serverLevel();
            InteractionHand interactionhand = pPacket.getHand();
            ItemStack itemstack = this.player.getItemInHand(interactionhand);
            if (itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
                BlockHitResult blockhitresult = pPacket.getHitResult();
                Vec3 vec3 = blockhitresult.getLocation();
                BlockPos blockpos = blockhitresult.getBlockPos();
                if (this.player.canInteractWithBlock(blockpos, 1.0)) {
                    Vec3 vec31 = vec3.subtract(Vec3.atCenterOf(blockpos));
                    double d0 = 1.0000001;
                    if (Math.abs(vec31.x()) < 1.0000001 && Math.abs(vec31.y()) < 1.0000001 && Math.abs(vec31.z()) < 1.0000001) {
                        Direction direction = blockhitresult.getDirection();
                        this.player.resetLastActionTime();
                        int i = this.player.level().getMaxY();
                        if (blockpos.getY() <= i) {
                            if (this.awaitingPositionFromClient == null && serverlevel.mayInteract(this.player, blockpos)) {
                                InteractionResult interactionresult = this.player
                                    .gameMode
                                    .useItemOn(this.player, serverlevel, itemstack, interactionhand, blockhitresult);
                                if (interactionresult.consumesAction()) {
                                    CriteriaTriggers.ANY_BLOCK_USE.trigger(this.player, blockhitresult.getBlockPos(), itemstack.copy());
                                }

                                if (direction == Direction.UP && !interactionresult.consumesAction() && blockpos.getY() >= i && wasBlockPlacementAttempt(this.player, itemstack)
                                    )
                                 {
                                    Component component = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
                                    this.player.sendSystemMessage(component, true);
                                } else if (interactionresult instanceof InteractionResult.Success interactionresult$success
                                    && interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                    this.player.swing(interactionhand, true);
                                }
                            }
                        } else {
                            Component component1 = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
                            this.player.sendSystemMessage(component1, true);
                        }

                        this.player.connection.send(new ClientboundBlockUpdatePacket(serverlevel, blockpos));
                        this.player.connection.send(new ClientboundBlockUpdatePacket(serverlevel, blockpos.relative(direction)));
                    } else {
                        LOGGER.warn(
                            "Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.", this.player.getGameProfile().getName(), vec3, blockpos
                        );
                    }
                }
            }
        }
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasClientLoaded()) {
            this.ackBlockChangesUpTo(pPacket.getSequence());
            ServerLevel serverlevel = this.player.serverLevel();
            InteractionHand interactionhand = pPacket.getHand();
            ItemStack itemstack = this.player.getItemInHand(interactionhand);
            this.player.resetLastActionTime();
            if (!itemstack.isEmpty() && itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
                float f = Mth.wrapDegrees(pPacket.getYRot());
                float f1 = Mth.wrapDegrees(pPacket.getXRot());
                if (f1 != this.player.getXRot() || f != this.player.getYRot()) {
                    this.player.absSnapRotationTo(f, f1);
                }

                if (this.player.gameMode.useItem(this.player, serverlevel, itemstack, interactionhand) instanceof InteractionResult.Success interactionresult$success
                    && interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
                    this.player.swing(interactionhand, true);
                }
            }
        }
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.isSpectator()) {
            for (ServerLevel serverlevel : this.server.getAllLevels()) {
                Entity entity = pPacket.getEntity(serverlevel);
                if (entity != null) {
                    this.player
                        .teleportTo(serverlevel, entity.getX(), entity.getY(), entity.getZ(), Set.of(), entity.getYRot(), entity.getXRot(), true);
                    return;
                }
            }
        }
    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.getControlledVehicle() instanceof AbstractBoat abstractboat) {
            abstractboat.setPaddleState(pPacket.getLeft(), pPacket.getRight());
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_342442_) {
        LOGGER.info("{} lost connection: {}", this.player.getName().getString(), p_342442_.reason().getString());
        this.removePlayerFromWorld();
        super.onDisconnect(p_342442_);
    }

    private void removePlayerFromWorld() {
        this.chatMessageChain.close();
        this.server.invalidateStatus();
        this.server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.left", this.player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
        this.player.disconnect();
        this.server.getPlayerList().remove(this.player);
        this.player.getTextFilter().leave();
    }

    public void ackBlockChangesUpTo(int pSequence) {
        if (pSequence < 0) {
            throw new IllegalArgumentException("Expected packet sequence nr >= 0");
        } else {
            this.ackBlockChangesUpTo = Math.max(pSequence, this.ackBlockChangesUpTo);
        }
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (pPacket.getSlot() >= 0 && pPacket.getSlot() < Inventory.getSelectionSize()) {
            if (this.player.getInventory().getSelectedSlot() != pPacket.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                this.player.stopUsingItem();
            }

            this.player.getInventory().setSelectedSlot(pPacket.getSlot());
            this.player.resetLastActionTime();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", this.player.getName().getString());
        }
    }

    @Override
    public void handleChat(ServerboundChatPacket pPacket) {
        Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(pPacket.lastSeenMessages());
        if (!optional.isEmpty()) {
            this.tryHandleChat(pPacket.message(), () -> {
                PlayerChatMessage playerchatmessage;
                try {
                    playerchatmessage = this.getSignedMessage(pPacket, optional.get());
                } catch (SignedMessageChain.DecodeException signedmessagechain$decodeexception) {
                    this.handleMessageDecodeFailure(signedmessagechain$decodeexception);
                    return;
                }

                CompletableFuture<FilteredText> completablefuture = this.filterTextPacket(playerchatmessage.signedContent());
                Component component = net.minecraftforge.common.ForgeHooks.onServerChatSubmittedEvent(this.player, playerchatmessage.decoratedContent());
                this.chatMessageChain.append(completablefuture, p_296589_ -> {
                    if (component == null) return;
                    PlayerChatMessage playerchatmessage1 = playerchatmessage.withUnsignedContent(component).filter(p_296589_.mask());
                    this.broadcastChatMessage(playerchatmessage1);
                });
            });
        }
    }

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket p_215225_) {
        this.tryHandleChat(p_215225_.command(), () -> {
            this.performUnsignedChatCommand(p_215225_.command());
            this.detectRateSpam();
        });
    }

    private void performUnsignedChatCommand(String pCommand) {
        ParseResults<CommandSourceStack> parseresults = this.parseCommand(pCommand);
        if (this.server.enforceSecureProfile() && SignableCommand.hasSignableArguments(parseresults)) {
            LOGGER.error(
                "Received unsigned command packet from {}, but the command requires signable arguments: {}", this.player.getGameProfile().getName(), pCommand
            );
            this.player.sendSystemMessage(INVALID_COMMAND_SIGNATURE);
        } else {
            this.server.getCommands().performCommand(parseresults, pCommand);
        }
    }

    @Override
    public void handleSignedChatCommand(ServerboundChatCommandSignedPacket p_330005_) {
        Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(p_330005_.lastSeenMessages());
        if (!optional.isEmpty()) {
            this.tryHandleChat(p_330005_.command(), () -> {
                this.performSignedChatCommand(p_330005_, optional.get());
                this.detectRateSpam();
            });
        }
    }

    private void performSignedChatCommand(ServerboundChatCommandSignedPacket pPacket, LastSeenMessages pLastSeenMessages) {
        ParseResults<CommandSourceStack> parseresults = this.parseCommand(pPacket.command());

        Map<String, PlayerChatMessage> map;
        try {
            map = this.collectSignedArguments(pPacket, SignableCommand.of(parseresults), pLastSeenMessages);
        } catch (SignedMessageChain.DecodeException signedmessagechain$decodeexception) {
            this.handleMessageDecodeFailure(signedmessagechain$decodeexception);
            return;
        }

        CommandSigningContext commandsigningcontext = new CommandSigningContext.SignedArguments(map);
        parseresults = Commands.mapSource(parseresults, p_296586_ -> p_296586_.withSigningContext(commandsigningcontext, this.chatMessageChain));
        this.server.getCommands().performCommand(parseresults, pPacket.command());
    }

    private void handleMessageDecodeFailure(SignedMessageChain.DecodeException pException) {
        LOGGER.warn("Failed to update secure chat state for {}: '{}'", this.player.getGameProfile().getName(), pException.getComponent().getString());
        this.player.sendSystemMessage(pException.getComponent().copy().withStyle(ChatFormatting.RED));
    }

    private <S> Map<String, PlayerChatMessage> collectSignedArguments(ServerboundChatCommandSignedPacket pPacket, SignableCommand<S> pCommand, LastSeenMessages pLastSeenMessages) throws SignedMessageChain.DecodeException {
        List<ArgumentSignatures.Entry> list = pPacket.argumentSignatures().entries();
        List<SignableCommand.Argument<S>> list1 = pCommand.arguments();
        if (list.isEmpty()) {
            return this.collectUnsignedArguments(list1);
        } else {
            Map<String, PlayerChatMessage> map = new Object2ObjectOpenHashMap<>();

            for (ArgumentSignatures.Entry argumentsignatures$entry : list) {
                SignableCommand.Argument<S> argument = pCommand.getArgument(argumentsignatures$entry.name());
                if (argument == null) {
                    this.signedMessageDecoder.setChainBroken();
                    throw createSignedArgumentMismatchException(pPacket.command(), list, list1);
                }

                SignedMessageBody signedmessagebody = new SignedMessageBody(argument.value(), pPacket.timeStamp(), pPacket.salt(), pLastSeenMessages);
                map.put(argument.name(), this.signedMessageDecoder.unpack(argumentsignatures$entry.signature(), signedmessagebody));
            }

            for (SignableCommand.Argument<S> argument1 : list1) {
                if (!map.containsKey(argument1.name())) {
                    throw createSignedArgumentMismatchException(pPacket.command(), list, list1);
                }
            }

            return map;
        }
    }

    private <S> Map<String, PlayerChatMessage> collectUnsignedArguments(List<SignableCommand.Argument<S>> pArguments) throws SignedMessageChain.DecodeException {
        Map<String, PlayerChatMessage> map = new HashMap<>();

        for (SignableCommand.Argument<S> argument : pArguments) {
            SignedMessageBody signedmessagebody = SignedMessageBody.unsigned(argument.value());
            map.put(argument.name(), this.signedMessageDecoder.unpack(null, signedmessagebody));
        }

        return map;
    }

    private static <S> SignedMessageChain.DecodeException createSignedArgumentMismatchException(
        String pCommand, List<ArgumentSignatures.Entry> pSignedArguments, List<SignableCommand.Argument<S>> pUnsignedArguments
    ) {
        String s = pSignedArguments.stream().map(ArgumentSignatures.Entry::name).collect(Collectors.joining(", "));
        String s1 = pUnsignedArguments.stream().map(SignableCommand.Argument::name).collect(Collectors.joining(", "));
        LOGGER.error("Signed command mismatch between server and client ('{}'): got [{}] from client, but expected [{}]", pCommand, s, s1);
        return new SignedMessageChain.DecodeException(INVALID_COMMAND_SIGNATURE);
    }

    private ParseResults<CommandSourceStack> parseCommand(String pCommand) {
        CommandDispatcher<CommandSourceStack> commanddispatcher = this.server.getCommands().getDispatcher();
        return commanddispatcher.parse(pCommand, this.player.createCommandSourceStack());
    }

    private void tryHandleChat(String pMessage, Runnable pHandler) {
        if (isChatMessageIllegal(pMessage)) {
            this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
        } else if (this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
        } else {
            this.player.resetLastActionTime();
            this.server.execute(pHandler);
        }
    }

    private Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update pUpdate) {
        synchronized (this.lastSeenMessages) {
            Optional optional;
            try {
                LastSeenMessages lastseenmessages = this.lastSeenMessages.applyUpdate(pUpdate);
                optional = Optional.of(lastseenmessages);
            } catch (LastSeenMessagesValidator.ValidationException lastseenmessagesvalidator$validationexception) {
                LOGGER.error(
                    "Failed to validate message acknowledgements from {}: {}",
                    this.player.getName().getString(),
                    lastseenmessagesvalidator$validationexception.getMessage()
                );
                this.disconnect(CHAT_VALIDATION_FAILED);
                return Optional.empty();
            }

            return optional;
        }
    }

    private static boolean isChatMessageIllegal(String pMessage) {
        for (int i = 0; i < pMessage.length(); i++) {
            if (!StringUtil.isAllowedChatCharacter(pMessage.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    private PlayerChatMessage getSignedMessage(ServerboundChatPacket pPacket, LastSeenMessages pLastSeenMessages) throws SignedMessageChain.DecodeException {
        SignedMessageBody signedmessagebody = new SignedMessageBody(pPacket.message(), pPacket.timeStamp(), pPacket.salt(), pLastSeenMessages);
        return this.signedMessageDecoder.unpack(pPacket.signature(), signedmessagebody);
    }

    private void broadcastChatMessage(PlayerChatMessage pMessage) {
        this.server.getPlayerList().broadcastChatMessage(pMessage, this.player, ChatType.bind(ChatType.CHAT, this.player));
        this.detectRateSpam();
    }

    private void detectRateSpam() {
        this.chatSpamThrottler.increment();
        if (!this.chatSpamThrottler.isUnderThreshold() && !this.server.getPlayerList().isOp(this.player.getGameProfile()) && !this.server.isSingleplayerOwner(this.player.getGameProfile())) {
            this.disconnect(Component.translatable("disconnect.spam"));
        }
    }

    @Override
    public void handleChatAck(ServerboundChatAckPacket p_242387_) {
        synchronized (this.lastSeenMessages) {
            try {
                this.lastSeenMessages.applyOffset(p_242387_.offset());
            } catch (LastSeenMessagesValidator.ValidationException lastseenmessagesvalidator$validationexception) {
                LOGGER.error(
                    "Failed to validate message acknowledgement offset from {}: {}",
                    this.player.getName().getString(),
                    lastseenmessagesvalidator$validationexception.getMessage()
                );
                this.disconnect(CHAT_VALIDATION_FAILED);
            }
        }
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.resetLastActionTime();
        this.player.swing(pPacket.getHand());
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasClientLoaded()) {
            this.player.resetLastActionTime();
            switch (pPacket.getAction()) {
                case PRESS_SHIFT_KEY:
                    this.player.setShiftKeyDown(true);
                    break;
                case RELEASE_SHIFT_KEY:
                    this.player.setShiftKeyDown(false);
                    break;
                case START_SPRINTING:
                    this.player.setSprinting(true);
                    break;
                case STOP_SPRINTING:
                    this.player.setSprinting(false);
                    break;
                case STOP_SLEEPING:
                    if (this.player.isSleeping()) {
                        this.player.stopSleepInBed(false, true);
                        this.awaitingPositionFromClient = this.player.position();
                    }
                    break;
                case START_RIDING_JUMP:
                    if (this.player.getControlledVehicle() instanceof PlayerRideableJumping playerrideablejumping1) {
                        int i = pPacket.getData();
                        if (playerrideablejumping1.canJump() && i > 0) {
                            playerrideablejumping1.handleStartJump(i);
                        }
                    }
                    break;
                case STOP_RIDING_JUMP:
                    if (this.player.getControlledVehicle() instanceof PlayerRideableJumping playerrideablejumping) {
                        playerrideablejumping.handleStopJump();
                    }
                    break;
                case OPEN_INVENTORY:
                    if (this.player.getVehicle() instanceof HasCustomInventoryScreen hascustominventoryscreen) {
                        hascustominventoryscreen.openCustomInventoryScreen(this.player);
                    }
                    break;
                case START_FALL_FLYING:
                    if (!this.player.tryToStartFallFlying()) {
                        this.player.stopFallFlying();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid client command!");
            }
        }
    }

    public void sendPlayerChatMessage(PlayerChatMessage pChatMessage, ChatType.Bound pBoundType) {
        this.send(
            new ClientboundPlayerChatPacket(
                this.nextChatIndex++,
                pChatMessage.link().sender(),
                pChatMessage.link().index(),
                pChatMessage.signature(),
                pChatMessage.signedBody().pack(this.messageSignatureCache),
                pChatMessage.unsignedContent(),
                pChatMessage.filterMask(),
                pBoundType
            )
        );
        MessageSignature messagesignature = pChatMessage.signature();
        if (messagesignature != null) {
            this.messageSignatureCache.push(pChatMessage.signedBody(), pChatMessage.signature());
            int i;
            synchronized (this.lastSeenMessages) {
                this.lastSeenMessages.addPending(messagesignature);
                i = this.lastSeenMessages.trackedMessagesCount();
            }

            if (i > 4096) {
                this.disconnect(Component.translatable("multiplayer.disconnect.too_many_pending_chats"));
            }
        }
    }

    public void sendDisguisedChatMessage(Component pMessage, ChatType.Bound pBoundType) {
        this.send(new ClientboundDisguisedChatPacket(pMessage, pBoundType));
    }

    public SocketAddress getRemoteAddress() {
        return this.connection.getRemoteAddress();
    }

    public void switchToConfig() {
        this.waitingForSwitchToConfig = true;
        this.removePlayerFromWorld();
        this.send(ClientboundStartConfigurationPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
    }

    @Override
    public void handlePingRequest(ServerboundPingRequestPacket p_332170_) {
        this.connection.send(new ClientboundPongResponsePacket(p_332170_.getTime()));
    }

    @Override
    public void handleInteract(ServerboundInteractPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasClientLoaded()) {
            final ServerLevel serverlevel = this.player.serverLevel();
            final Entity entity = pPacket.getTarget(serverlevel);
            this.player.resetLastActionTime();
            this.player.setShiftKeyDown(pPacket.isUsingSecondaryAction());
            if (entity != null) {
                if (!serverlevel.getWorldBorder().isWithinBounds(entity.blockPosition())) {
                    return;
                }

                AABB aabb = entity.getBoundingBox();
                if (this.player.canInteractWithEntity(aabb, 3.0)) {
                    pPacket.dispatch(
                        new ServerboundInteractPacket.Handler() {
                            private void performInteraction(InteractionHand p_143679_, ServerGamePacketListenerImpl.EntityInteraction p_143680_) {
                                ItemStack itemstack = ServerGamePacketListenerImpl.this.player.getItemInHand(p_143679_);
                                if (itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
                                    ItemStack itemstack1 = itemstack.copy();
                                    if (p_143680_.run(ServerGamePacketListenerImpl.this.player, entity, p_143679_) instanceof InteractionResult.Success interactionresult$success
                                        )
                                     {
                                        ItemStack itemstack2 = interactionresult$success.wasItemInteraction() ? itemstack1 : ItemStack.EMPTY;
                                        CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(ServerGamePacketListenerImpl.this.player, itemstack2, entity);
                                        if (interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                            ServerGamePacketListenerImpl.this.player.swing(p_143679_, true);
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onInteraction(InteractionHand p_143677_) {
                                this.performInteraction(p_143677_, Player::interactOn);
                            }

                            @Override
                            public void onInteraction(InteractionHand p_143682_, Vec3 p_143683_) {
                                this.performInteraction(p_143682_, (p_143686_, p_143687_, p_143688_) -> net.minecraftforge.common.ForgeHooks.onInteractEntityAt(p_143687_, p_143686_, p_143683_, p_143688_));
                            }

                            @Override
                            public void onAttack() {
                                if (!(entity instanceof ItemEntity)
                                    && !(entity instanceof ExperienceOrb)
                                    && entity != ServerGamePacketListenerImpl.this.player
                                    && !(entity instanceof AbstractArrow abstractarrow && !abstractarrow.isAttackable())) {
                                    ItemStack itemstack = ServerGamePacketListenerImpl.this.player.getItemInHand(InteractionHand.MAIN_HAND);
                                    if (itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
                                        ServerGamePacketListenerImpl.this.player.attack(entity);
                                    }
                                } else {
                                    ServerGamePacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
                                    ServerGamePacketListenerImpl.LOGGER
                                        .warn("Player {} tried to attack an invalid entity", ServerGamePacketListenerImpl.this.player.getName().getString());
                                }
                            }
                        }
                    );
                }
            }
        }
    }

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.resetLastActionTime();
        ServerboundClientCommandPacket.Action serverboundclientcommandpacket$action = pPacket.getAction();
        switch (serverboundclientcommandpacket$action) {
            case PERFORM_RESPAWN:
                if (this.player.wonGame) {
                    this.player.wonGame = false;
                    this.player = this.server.getPlayerList().respawn(this.player, true, Entity.RemovalReason.CHANGED_DIMENSION);
                    this.resetPosition();
                    CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
                } else {
                    if (this.player.getHealth() > 0.0F) {
                        return;
                    }

                    this.player = this.server.getPlayerList().respawn(this.player, false, Entity.RemovalReason.KILLED);
                    this.resetPosition();
                    if (this.server.isHardcore()) {
                        this.player.setGameMode(GameType.SPECTATOR);
                        this.player.serverLevel().getGameRules().getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, this.server);
                    }
                }
                break;
            case REQUEST_STATS:
                this.player.getStats().sendStats(this.player);
        }
    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == pPacket.containerId()) {
            if (this.player.isSpectator()) {
                this.player.containerMenu.sendAllDataToRemote();
            } else if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                int i = pPacket.slotNum();
                if (!this.player.containerMenu.isValidSlotIndex(i)) {
                    LOGGER.debug(
                        "Player {} clicked invalid slot index: {}, available slots: {}", this.player.getName(), i, this.player.containerMenu.slots.size()
                    );
                } else {
                    boolean flag = pPacket.stateId() != this.player.containerMenu.getStateId();
                    this.player.containerMenu.suppressRemoteUpdates();
                    this.player.containerMenu.clicked(i, pPacket.buttonNum(), pPacket.clickType(), this.player);

                    for (Entry<HashedStack> entry : Int2ObjectMaps.fastIterable(pPacket.changedSlots())) {
                        this.player.containerMenu.setRemoteSlotUnsafe(entry.getIntKey(), entry.getValue());
                    }

                    this.player.containerMenu.setRemoteCarried(pPacket.carriedItem());
                    this.player.containerMenu.resumeRemoteUpdates();
                    if (flag) {
                        this.player.containerMenu.broadcastFullState();
                    } else {
                        this.player.containerMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.resetLastActionTime();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == pPacket.containerId()) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                RecipeManager.ServerDisplayInfo recipemanager$serverdisplayinfo = this.server.getRecipeManager().getRecipeFromDisplay(pPacket.recipe());
                if (recipemanager$serverdisplayinfo != null) {
                    RecipeHolder<?> recipeholder = recipemanager$serverdisplayinfo.parent();
                    if (this.player.getRecipeBook().contains(recipeholder.id())) {
                        if (this.player.containerMenu instanceof RecipeBookMenu recipebookmenu) {
                            if (recipeholder.value().placementInfo().isImpossibleToPlace()) {
                                LOGGER.debug("Player {} tried to place impossible recipe {}", this.player, recipeholder.id().location());
                                return;
                            }

                            RecipeBookMenu.PostPlaceAction recipebookmenu$postplaceaction = recipebookmenu.handlePlacement(
                                pPacket.useMaxItems(), this.player.isCreative(), recipeholder, this.player.serverLevel(), this.player.getInventory()
                            );
                            if (recipebookmenu$postplaceaction == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
                                this.player
                                    .connection
                                    .send(
                                        new ClientboundPlaceGhostRecipePacket(
                                            this.player.containerMenu.containerId, recipemanager$serverdisplayinfo.display().display()
                                        )
                                    );
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == pPacket.containerId() && !this.player.isSpectator()) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                boolean flag = this.player.containerMenu.clickMenuButton(this.player, pPacket.buttonId());
                if (flag) {
                    this.player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        if (this.player.hasInfiniteMaterials()) {
            boolean flag = pPacket.slotNum() < 0;
            ItemStack itemstack = pPacket.itemStack();
            if (!itemstack.isItemEnabled(this.player.level().enabledFeatures())) {
                return;
            }

            boolean flag1 = pPacket.slotNum() >= 1 && pPacket.slotNum() <= 45;
            boolean flag2 = itemstack.isEmpty() || itemstack.getCount() <= itemstack.getMaxStackSize();
            if (flag1 && flag2) {
                this.player.inventoryMenu.getSlot(pPacket.slotNum()).setByPlayer(itemstack);
                this.player.inventoryMenu.setRemoteSlot(pPacket.slotNum(), itemstack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (flag && flag2) {
                if (this.dropSpamThrottler.isUnderThreshold()) {
                    this.dropSpamThrottler.increment();
                    this.player.drop(itemstack, true);
                } else {
                    LOGGER.warn("Player {} was dropping items too fast in creative mode, ignoring.", this.player.getName().getString());
                }
            }
        }
    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket pPacket) {
        List<String> list = Stream.of(pPacket.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
        this.filterTextPacket(list).thenAcceptAsync(p_215245_ -> this.updateSignText(pPacket, (List<FilteredText>)p_215245_), this.server);
    }

    private void updateSignText(ServerboundSignUpdatePacket pPacket, List<FilteredText> pFilteredText) {
        this.player.resetLastActionTime();
        ServerLevel serverlevel = this.player.serverLevel();
        BlockPos blockpos = pPacket.getPos();
        if (serverlevel.hasChunkAt(blockpos)) {
            if (!(serverlevel.getBlockEntity(blockpos) instanceof SignBlockEntity signblockentity)) {
                return;
            }

            signblockentity.updateSignText(this.player, pPacket.isFrontText(), pFilteredText);
        }
    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.player.serverLevel());
        this.player.getAbilities().flying = pPacket.isFlying() && this.player.getAbilities().mayfly;
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket p_298714_) {
        PacketUtils.ensureRunningOnSameThread(p_298714_, this, this.player.serverLevel());
        boolean flag = this.player.isModelPartShown(PlayerModelPart.HAT);
        this.player.updateOptions(p_298714_.information());
        if (this.player.isModelPartShown(PlayerModelPart.HAT) != flag) {
            this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT, this.player));
        }
    }

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket p_9839_) {
        PacketUtils.ensureRunningOnSameThread(p_9839_, this, this.player.serverLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            this.server.setDifficulty(p_9839_.getDifficulty(), false);
        }
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket p_9872_) {
        PacketUtils.ensureRunningOnSameThread(p_9872_, this, this.player.serverLevel());
        if (this.player.hasPermissions(2) || this.isSingleplayerOwner()) {
            this.server.setDifficultyLocked(p_9872_.isLocked());
        }
    }

    @Override
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket p_253950_) {
        PacketUtils.ensureRunningOnSameThread(p_253950_, this, this.player.serverLevel());
        RemoteChatSession.Data remotechatsession$data = p_253950_.chatSession();
        ProfilePublicKey.Data profilepublickey$data = this.chatSession != null ? this.chatSession.profilePublicKey().data() : null;
        ProfilePublicKey.Data profilepublickey$data1 = remotechatsession$data.profilePublicKey();
        if (!Objects.equals(profilepublickey$data, profilepublickey$data1)) {
            if (profilepublickey$data != null && profilepublickey$data1.expiresAt().isBefore(profilepublickey$data.expiresAt())) {
                this.disconnect(ProfilePublicKey.EXPIRED_PROFILE_PUBLIC_KEY);
            } else {
                try {
                    SignatureValidator signaturevalidator = this.server.getProfileKeySignatureValidator();
                    if (signaturevalidator == null) {
                        LOGGER.warn("Ignoring chat session from {} due to missing Services public key", this.player.getGameProfile().getName());
                        return;
                    }

                    this.resetPlayerChatState(remotechatsession$data.validate(this.player.getGameProfile(), signaturevalidator));
                } catch (ProfilePublicKey.ValidationException profilepublickey$validationexception) {
                    LOGGER.error("Failed to validate profile key: {}", profilepublickey$validationexception.getMessage());
                    this.disconnect(profilepublickey$validationexception.getComponent());
                }
            }
        }
    }

    @Override
    public void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket p_299199_) {
        if (!this.waitingForSwitchToConfig) {
            throw new IllegalStateException("Client acknowledged config, but none was requested");
        } else {
            this.connection
                .setupInboundProtocol(
                    ConfigurationProtocols.SERVERBOUND,
                    new ServerConfigurationPacketListenerImpl(this.server, this.connection, this.createCookie(this.player.clientInformation()))
                );
        }
    }

    @Override
    public void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket p_298310_) {
        PacketUtils.ensureRunningOnSameThread(p_298310_, this, this.player.serverLevel());
        this.chunkSender.onChunkBatchReceivedByClient(p_298310_.desiredChunksPerTick());
    }

    @Override
    public void handleDebugSampleSubscription(ServerboundDebugSampleSubscriptionPacket p_329570_) {
        PacketUtils.ensureRunningOnSameThread(p_329570_, this, this.player.serverLevel());
        this.server.subscribeToDebugSample(this.player, p_329570_.sampleType());
    }

    private void resetPlayerChatState(RemoteChatSession pChatSession) {
        this.chatSession = pChatSession;
        this.signedMessageDecoder = pChatSession.createMessageDecoder(this.player.getUUID());
        this.chatMessageChain
            .append(
                () -> {
                    this.player.setChatSession(pChatSession);
                    this.server
                        .getPlayerList()
                        .broadcastAll(
                            new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT), List.of(this.player))
                        );
                }
            );
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket p_329963_) {
        net.minecraftforge.common.ForgeHooks.onCustomPayload(p_329963_.payload(), this.connection);
    }

    @Override
    public void handleClientTickEnd(ServerboundClientTickEndPacket p_363521_) {
        PacketUtils.ensureRunningOnSameThread(p_363521_, this, this.player.serverLevel());
        if (!this.receivedMovementThisTick) {
            this.player.setKnownMovement(Vec3.ZERO);
        }

        this.receivedMovementThisTick = false;
    }

    private void handlePlayerKnownMovement(Vec3 pMovement) {
        if (pMovement.lengthSqr() > 1.0E-5F) {
            this.player.resetLastActionTime();
        }

        this.player.setKnownMovement(pMovement);
        this.receivedMovementThisTick = true;
    }

    @Override
    public boolean hasInfiniteMaterials() {
        return this.player.hasInfiniteMaterials();
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    @FunctionalInterface
    interface EntityInteraction {
        InteractionResult run(ServerPlayer pPlayer, Entity pEntity, InteractionHand pHand);
    }
}
