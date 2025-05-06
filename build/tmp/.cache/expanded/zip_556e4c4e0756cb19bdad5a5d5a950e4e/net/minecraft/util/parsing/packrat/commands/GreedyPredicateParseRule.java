package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public abstract class GreedyPredicateParseRule implements Rule<StringReader, String> {
    private final int minSize;
    private final int maxSize;
    private final DelayedException<CommandSyntaxException> error;

    public GreedyPredicateParseRule(int pMinSize, DelayedException<CommandSyntaxException> pError) {
        this(pMinSize, Integer.MAX_VALUE, pError);
    }

    public GreedyPredicateParseRule(int pMinSize, int pMaxSize, DelayedException<CommandSyntaxException> pError) {
        this.minSize = pMinSize;
        this.maxSize = pMaxSize;
        this.error = pError;
    }

    @Nullable
    public String parse(ParseState<StringReader> p_397364_) {
        StringReader stringreader = p_397364_.input();
        String s = stringreader.getString();
        int i = stringreader.getCursor();
        int j = i;

        while (j < s.length() && this.isAccepted(s.charAt(j)) && j - i < this.maxSize) {
            j++;
        }

        int k = j - i;
        if (k < this.minSize) {
            p_397364_.errorCollector().store(p_397364_.mark(), this.error);
            return null;
        } else {
            stringreader.setCursor(j);
            return s.substring(i, j);
        }
    }

    protected abstract boolean isAccepted(char pC);
}