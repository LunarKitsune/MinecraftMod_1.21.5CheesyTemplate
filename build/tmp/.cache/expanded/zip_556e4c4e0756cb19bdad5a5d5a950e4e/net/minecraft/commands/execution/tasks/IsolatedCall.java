package net.minecraft.commands.execution.tasks;

import java.util.function.Consumer;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;

public class IsolatedCall<T extends ExecutionCommandSource<T>> implements EntryAction<T> {
    private final Consumer<ExecutionControl<T>> taskProducer;
    private final CommandResultCallback output;

    public IsolatedCall(Consumer<ExecutionControl<T>> pTaskProducer, CommandResultCallback pOutput) {
        this.taskProducer = pTaskProducer;
        this.output = pOutput;
    }

    @Override
    public void execute(ExecutionContext<T> p_312137_, Frame p_311608_) {
        int i = p_311608_.depth() + 1;
        Frame frame = new Frame(i, this.output, p_312137_.frameControlForDepth(i));
        this.taskProducer.accept(ExecutionControl.create(p_312137_, frame));
    }
}