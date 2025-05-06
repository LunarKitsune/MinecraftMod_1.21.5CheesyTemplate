package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    public ReportGameListener() {
    }

    @Override
    public void testStructureLoaded(GameTestInfo p_177718_) {
        this.attempts++;
    }

    private void handleRetry(GameTestInfo pTestInfo, GameTestRunner pRunner, boolean pPassed) {
        RetryOptions retryoptions = pTestInfo.retryOptions();
        String s = String.format("[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryoptions.unlimitedTries()) {
            s = s + String.format(", Left: %4d", retryoptions.numberOfTries() - this.attempts);
        }

        s = s + "]";
        String s1 = pTestInfo.id() + " " + (pPassed ? "passed" : "failed") + "! " + pTestInfo.getRunTime() + "ms";
        String s2 = String.format("%-53s%s", s, s1);
        if (pPassed) {
            reportPassed(pTestInfo, s2);
        } else {
            say(pTestInfo.getLevel(), ChatFormatting.RED, s2);
        }

        if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
            pRunner.rerunTest(pTestInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo p_177729_, GameTestRunner p_331098_) {
        this.successes++;
        if (p_177729_.retryOptions().hasRetries()) {
            this.handleRetry(p_177729_, p_331098_, true);
        } else if (!p_177729_.isFlaky()) {
            reportPassed(p_177729_, p_177729_.id() + " passed! (" + p_177729_.getRunTime() + "ms)");
        } else {
            if (this.successes >= p_177729_.requiredSuccesses()) {
                reportPassed(p_177729_, p_177729_ + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    p_177729_.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + p_177729_ + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                p_331098_.rerunTest(p_177729_);
            }
        }
    }

    @Override
    public void testFailed(GameTestInfo p_177737_, GameTestRunner p_330024_) {
        if (!p_177737_.isFlaky()) {
            reportFailure(p_177737_, p_177737_.getError());
            if (p_177737_.retryOptions().hasRetries()) {
                this.handleRetry(p_177737_, p_330024_, false);
            }
        } else {
            GameTestInstance gametestinstance = p_177737_.getTest();
            String s = "Flaky test " + p_177737_ + " failed, attempt: " + this.attempts + "/" + gametestinstance.maxAttempts();
            if (gametestinstance.requiredSuccesses() > 1) {
                s = s + ", successes: " + this.successes + " (" + gametestinstance.requiredSuccesses() + " required)";
            }

            say(p_177737_.getLevel(), ChatFormatting.YELLOW, s);
            if (p_177737_.maxAttempts() - this.attempts + this.successes >= p_177737_.requiredSuccesses()) {
                p_330024_.rerunTest(p_177737_);
            } else {
                reportFailure(p_177737_, new ExhaustedAttemptsException(this.attempts, this.successes, p_177737_));
            }
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo p_330084_, GameTestInfo p_327991_, GameTestRunner p_334385_) {
        p_327991_.addListener(this);
    }

    public static void reportPassed(GameTestInfo pTestInfo, String pMessage) {
        getTestInstanceBlockEntity(pTestInfo).ifPresent(p_389781_ -> p_389781_.setSuccess());
        visualizePassedTest(pTestInfo, pMessage);
    }

    private static void visualizePassedTest(GameTestInfo pTestInfo, String pMessage) {
        say(pTestInfo.getLevel(), ChatFormatting.GREEN, pMessage);
        GlobalTestReporter.onTestSuccess(pTestInfo);
    }

    protected static void reportFailure(GameTestInfo pTestInfo, Throwable pError) {
        Component component;
        if (pError instanceof GameTestAssertException gametestassertexception) {
            component = gametestassertexception.getDescription();
        } else {
            component = Component.literal(Util.describeError(pError));
        }

        getTestInstanceBlockEntity(pTestInfo).ifPresent(p_389783_ -> p_389783_.setErrorMessage(component));
        visualizeFailedTest(pTestInfo, pError);
    }

    protected static void visualizeFailedTest(GameTestInfo pTestInfo, Throwable pError) {
        String s = pError.getMessage() + (pError.getCause() == null ? "" : " cause: " + Util.describeError(pError.getCause()));
        String s1 = (pTestInfo.isRequired() ? "" : "(optional) ") + pTestInfo.id() + " failed! " + s;
        say(pTestInfo.getLevel(), pTestInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(pError), pError);
        if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
            showRedBox(pTestInfo.getLevel(), gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(pTestInfo);
    }

    private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestInfo pTestInfo) {
        ServerLevel serverlevel = pTestInfo.getLevel();
        Optional<BlockPos> optional = Optional.ofNullable(pTestInfo.getTestBlockPos());
        return optional.flatMap(p_389780_ -> serverlevel.getBlockEntity(p_389780_, BlockEntityType.TEST_INSTANCE_BLOCK));
    }

    protected static void say(ServerLevel pServerLevel, ChatFormatting pFormatting, String pMessage) {
        pServerLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(pMessage).withStyle(pFormatting)));
    }

    private static void showRedBox(ServerLevel pServerLevel, BlockPos pPos, String pDisplayMessage) {
        DebugPackets.sendGameTestAddMarker(pServerLevel, pPos, pDisplayMessage, -2130771968, Integer.MAX_VALUE);
    }
}