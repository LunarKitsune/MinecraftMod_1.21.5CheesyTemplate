package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementCommands {
    private static final DynamicCommandExceptionType ERROR_NO_ACTION_PERFORMED = new DynamicCommandExceptionType(p_308608_ -> (Component)p_308608_);
    private static final Dynamic2CommandExceptionType ERROR_CRITERION_NOT_FOUND = new Dynamic2CommandExceptionType(
        (p_341132_, p_341133_) -> Component.translatableEscape("commands.advancement.criterionNotFound", p_341132_, p_341133_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
        pDispatcher.register(
            Commands.literal("advancement")
                .requires(p_136318_ -> p_136318_.hasPermission(2))
                .then(
                    Commands.literal("grant")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358563_ -> perform(
                                                        p_358563_.getSource(),
                                                        EntityArgument.getPlayers(p_358563_, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            p_358563_, ResourceKeyArgument.getAdvancement(p_358563_, "advancement"), AdvancementCommands.Mode.ONLY
                                                        )
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (p_358572_, p_358573_) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(p_358572_, "advancement").value().criteria().keySet(),
                                                                p_358573_
                                                            )
                                                        )
                                                        .executes(
                                                            p_358574_ -> performCriterion(
                                                                p_358574_.getSource(),
                                                                EntityArgument.getPlayers(p_358574_, "targets"),
                                                                AdvancementCommands.Action.GRANT,
                                                                ResourceKeyArgument.getAdvancement(p_358574_, "advancement"),
                                                                StringArgumentType.getString(p_358574_, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358555_ -> perform(
                                                        p_358555_.getSource(),
                                                        EntityArgument.getPlayers(p_358555_, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            p_358555_, ResourceKeyArgument.getAdvancement(p_358555_, "advancement"), AdvancementCommands.Mode.FROM
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358564_ -> perform(
                                                        p_358564_.getSource(),
                                                        EntityArgument.getPlayers(p_358564_, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            p_358564_, ResourceKeyArgument.getAdvancement(p_358564_, "advancement"), AdvancementCommands.Mode.UNTIL
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358565_ -> perform(
                                                        p_358565_.getSource(),
                                                        EntityArgument.getPlayers(p_358565_, "targets"),
                                                        AdvancementCommands.Action.GRANT,
                                                        getAdvancements(
                                                            p_358565_,
                                                            ResourceKeyArgument.getAdvancement(p_358565_, "advancement"),
                                                            AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            p_389952_ -> perform(
                                                p_389952_.getSource(),
                                                EntityArgument.getPlayers(p_389952_, "targets"),
                                                AdvancementCommands.Action.GRANT,
                                                p_389952_.getSource().getServer().getAdvancements().getAllAdvancements(),
                                                false
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("revoke")
                        .then(
                            Commands.argument("targets", EntityArgument.players())
                                .then(
                                    Commands.literal("only")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358556_ -> perform(
                                                        p_358556_.getSource(),
                                                        EntityArgument.getPlayers(p_358556_, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            p_358556_, ResourceKeyArgument.getAdvancement(p_358556_, "advancement"), AdvancementCommands.Mode.ONLY
                                                        )
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("criterion", StringArgumentType.greedyString())
                                                        .suggests(
                                                            (p_358560_, p_358561_) -> SharedSuggestionProvider.suggest(
                                                                ResourceKeyArgument.getAdvancement(p_358560_, "advancement").value().criteria().keySet(),
                                                                p_358561_
                                                            )
                                                        )
                                                        .executes(
                                                            p_358562_ -> performCriterion(
                                                                p_358562_.getSource(),
                                                                EntityArgument.getPlayers(p_358562_, "targets"),
                                                                AdvancementCommands.Action.REVOKE,
                                                                ResourceKeyArgument.getAdvancement(p_358562_, "advancement"),
                                                                StringArgumentType.getString(p_358562_, "criterion")
                                                            )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("from")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358569_ -> perform(
                                                        p_358569_.getSource(),
                                                        EntityArgument.getPlayers(p_358569_, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            p_358569_, ResourceKeyArgument.getAdvancement(p_358569_, "advancement"), AdvancementCommands.Mode.FROM
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("until")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358571_ -> perform(
                                                        p_358571_.getSource(),
                                                        EntityArgument.getPlayers(p_358571_, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            p_358571_, ResourceKeyArgument.getAdvancement(p_358571_, "advancement"), AdvancementCommands.Mode.UNTIL
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("through")
                                        .then(
                                            Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                                .executes(
                                                    p_358570_ -> perform(
                                                        p_358570_.getSource(),
                                                        EntityArgument.getPlayers(p_358570_, "targets"),
                                                        AdvancementCommands.Action.REVOKE,
                                                        getAdvancements(
                                                            p_358570_,
                                                            ResourceKeyArgument.getAdvancement(p_358570_, "advancement"),
                                                            AdvancementCommands.Mode.THROUGH
                                                        )
                                                    )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("everything")
                                        .executes(
                                            p_136313_ -> perform(
                                                p_136313_.getSource(),
                                                EntityArgument.getPlayers(p_136313_, "targets"),
                                                AdvancementCommands.Action.REVOKE,
                                                p_136313_.getSource().getServer().getAdvancements().getAllAdvancements()
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int perform(
        CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, Collection<AdvancementHolder> pAdvancements
    ) throws CommandSyntaxException {
        return perform(pSource, pTargets, pAction, pAdvancements, true);
    }

    private static int perform(
        CommandSourceStack pSource,
        Collection<ServerPlayer> pTargets,
        AdvancementCommands.Action pAction,
        Collection<AdvancementHolder> pAdvancements,
        boolean pGrantEverything
    ) throws CommandSyntaxException {
        int i = 0;

        for (ServerPlayer serverplayer : pTargets) {
            i += pAction.perform(serverplayer, pAdvancements, pGrantEverything);
        }

        if (i == 0) {
            if (pAdvancements.size() == 1) {
                if (pTargets.size() == 1) {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(
                            pAction.getKey() + ".one.to.one.failure",
                            Advancement.name(pAdvancements.iterator().next()),
                            pTargets.iterator().next().getDisplayName()
                        )
                    );
                } else {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(
                            pAction.getKey() + ".one.to.many.failure", Advancement.name(pAdvancements.iterator().next()), pTargets.size()
                        )
                    );
                }
            } else if (pTargets.size() == 1) {
                throw ERROR_NO_ACTION_PERFORMED.create(
                    Component.translatable(pAction.getKey() + ".many.to.one.failure", pAdvancements.size(), pTargets.iterator().next().getDisplayName())
                );
            } else {
                throw ERROR_NO_ACTION_PERFORMED.create(Component.translatable(pAction.getKey() + ".many.to.many.failure", pAdvancements.size(), pTargets.size()));
            }
        } else {
            if (pAdvancements.size() == 1) {
                if (pTargets.size() == 1) {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                            pAction.getKey() + ".one.to.one.success",
                            Advancement.name(pAdvancements.iterator().next()),
                            pTargets.iterator().next().getDisplayName()
                        ),
                        true
                    );
                } else {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                            pAction.getKey() + ".one.to.many.success", Advancement.name(pAdvancements.iterator().next()), pTargets.size()
                        ),
                        true
                    );
                }
            } else if (pTargets.size() == 1) {
                pSource.sendSuccess(
                    () -> Component.translatable(pAction.getKey() + ".many.to.one.success", pAdvancements.size(), pTargets.iterator().next().getDisplayName()), true
                );
            } else {
                pSource.sendSuccess(() -> Component.translatable(pAction.getKey() + ".many.to.many.success", pAdvancements.size(), pTargets.size()), true);
            }

            return i;
        }
    }

    private static int performCriterion(
        CommandSourceStack pSource, Collection<ServerPlayer> pTargets, AdvancementCommands.Action pAction, AdvancementHolder pAdvancement, String pCriterionName
    ) throws CommandSyntaxException {
        int i = 0;
        Advancement advancement = pAdvancement.value();
        if (!advancement.criteria().containsKey(pCriterionName)) {
            throw ERROR_CRITERION_NOT_FOUND.create(Advancement.name(pAdvancement), pCriterionName);
        } else {
            for (ServerPlayer serverplayer : pTargets) {
                if (pAction.performCriterion(serverplayer, pAdvancement, pCriterionName)) {
                    i++;
                }
            }

            if (i == 0) {
                if (pTargets.size() == 1) {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(
                            pAction.getKey() + ".criterion.to.one.failure",
                            pCriterionName,
                            Advancement.name(pAdvancement),
                            pTargets.iterator().next().getDisplayName()
                        )
                    );
                } else {
                    throw ERROR_NO_ACTION_PERFORMED.create(
                        Component.translatable(pAction.getKey() + ".criterion.to.many.failure", pCriterionName, Advancement.name(pAdvancement), pTargets.size())
                    );
                }
            } else {
                if (pTargets.size() == 1) {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                            pAction.getKey() + ".criterion.to.one.success",
                            pCriterionName,
                            Advancement.name(pAdvancement),
                            pTargets.iterator().next().getDisplayName()
                        ),
                        true
                    );
                } else {
                    pSource.sendSuccess(
                        () -> Component.translatable(
                            pAction.getKey() + ".criterion.to.many.success", pCriterionName, Advancement.name(pAdvancement), pTargets.size()
                        ),
                        true
                    );
                }

                return i;
            }
        }
    }

    private static List<AdvancementHolder> getAdvancements(
        CommandContext<CommandSourceStack> pContext, AdvancementHolder pAdvancement, AdvancementCommands.Mode pMode
    ) {
        AdvancementTree advancementtree = pContext.getSource().getServer().getAdvancements().tree();
        AdvancementNode advancementnode = advancementtree.get(pAdvancement);
        if (advancementnode == null) {
            return List.of(pAdvancement);
        } else {
            List<AdvancementHolder> list = new ArrayList<>();
            if (pMode.parents) {
                for (AdvancementNode advancementnode1 = advancementnode.parent(); advancementnode1 != null; advancementnode1 = advancementnode1.parent()) {
                    list.add(advancementnode1.holder());
                }
            }

            list.add(pAdvancement);
            if (pMode.children) {
                addChildren(advancementnode, list);
            }

            return list;
        }
    }

    private static void addChildren(AdvancementNode pNode, List<AdvancementHolder> pOutput) {
        for (AdvancementNode advancementnode : pNode.children()) {
            pOutput.add(advancementnode.holder());
            addChildren(advancementnode, pOutput);
        }
    }

    static enum Action {
        GRANT("grant") {
            @Override
            protected boolean perform(ServerPlayer p_136395_, AdvancementHolder p_299481_) {
                AdvancementProgress advancementprogress = p_136395_.getAdvancements().getOrStartProgress(p_299481_);
                if (advancementprogress.isDone()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getRemainingCriteria()) {
                        p_136395_.getAdvancements().award(p_299481_, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer p_136398_, AdvancementHolder p_300422_, String p_136400_) {
                return p_136398_.getAdvancements().award(p_300422_, p_136400_);
            }
        },
        REVOKE("revoke") {
            @Override
            protected boolean perform(ServerPlayer p_136406_, AdvancementHolder p_301329_) {
                AdvancementProgress advancementprogress = p_136406_.getAdvancements().getOrStartProgress(p_301329_);
                if (!advancementprogress.hasProgress()) {
                    return false;
                } else {
                    for (String s : advancementprogress.getCompletedCriteria()) {
                        p_136406_.getAdvancements().revoke(p_301329_, s);
                    }

                    return true;
                }
            }

            @Override
            protected boolean performCriterion(ServerPlayer p_136409_, AdvancementHolder p_299512_, String p_136411_) {
                return p_136409_.getAdvancements().revoke(p_299512_, p_136411_);
            }
        };

        private final String key;

        Action(final String pKey) {
            this.key = "commands.advancement." + pKey;
        }

        public int perform(ServerPlayer pPlayer, Iterable<AdvancementHolder> pAdvancements, boolean pGrantEverything) {
            int i = 0;
            if (!pGrantEverything) {
                pPlayer.getAdvancements().flushDirty(pPlayer, true);
            }

            for (AdvancementHolder advancementholder : pAdvancements) {
                if (this.perform(pPlayer, advancementholder)) {
                    i++;
                }
            }

            if (!pGrantEverything) {
                pPlayer.getAdvancements().flushDirty(pPlayer, false);
            }

            return i;
        }

        protected abstract boolean perform(ServerPlayer pPlayer, AdvancementHolder pAdvancement);

        protected abstract boolean performCriterion(ServerPlayer pPlayer, AdvancementHolder pAdvancement, String pCriterionName);

        protected String getKey() {
            return this.key;
        }
    }

    static enum Mode {
        ONLY(false, false),
        THROUGH(true, true),
        FROM(false, true),
        UNTIL(true, false),
        EVERYTHING(true, true);

        final boolean parents;
        final boolean children;

        private Mode(final boolean pParents, final boolean pChildren) {
            this.parents = pParents;
            this.children = pChildren;
        }
    }
}