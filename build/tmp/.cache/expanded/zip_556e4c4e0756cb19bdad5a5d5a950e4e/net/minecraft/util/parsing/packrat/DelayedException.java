package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
    T create(String pMessage, int pCursor);

    static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType pException) {
        return (p_397051_, p_397771_) -> pException.createWithContext(StringReaderTerms.createReader(p_397051_, p_397771_));
    }

    static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType pException, String pArgument) {
        return (p_397984_, p_394109_) -> pException.createWithContext(StringReaderTerms.createReader(p_397984_, p_394109_), pArgument);
    }
}