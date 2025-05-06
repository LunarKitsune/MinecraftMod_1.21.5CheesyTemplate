package net.minecraft.commands.execution;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.ExecutionCommandSource;

public interface CustomCommandExecutor<T> {
    void run(T pSource, ContextChain<T> pContextChain, ChainModifiers pChainModifiers, ExecutionControl<T> pExecutionControl);

    public interface CommandAdapter<T> extends Command<T>, CustomCommandExecutor<T> {
        @Override
        default int run(CommandContext<T> pContext) throws CommandSyntaxException {
            throw new UnsupportedOperationException("This function should not run");
        }
    }

    public abstract static class WithErrorHandling<T extends ExecutionCommandSource<T>> implements CustomCommandExecutor<T> {
        public final void run(T p_310241_, ContextChain<T> p_311766_, ChainModifiers p_310779_, ExecutionControl<T> p_309382_) {
            try {
                this.runGuarded(p_310241_, p_311766_, p_310779_, p_309382_);
            } catch (CommandSyntaxException commandsyntaxexception) {
                this.onError(commandsyntaxexception, p_310241_, p_310779_, p_309382_.tracer());
                p_310241_.callback().onFailure();
            }
        }

        protected void onError(CommandSyntaxException pError, T pSource, ChainModifiers pChainModifiers, @Nullable TraceCallbacks pTraceCallbacks) {
            pSource.handleError(pError, pChainModifiers.isForked(), pTraceCallbacks);
        }

        protected abstract void runGuarded(T pSource, ContextChain<T> pContextChain, ChainModifiers pChainModifiers, ExecutionControl<T> pExecutionControl) throws CommandSyntaxException;
    }
}