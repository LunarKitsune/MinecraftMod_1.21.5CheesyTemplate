package net.minecraft.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.CharPredicate;

public class ParserUtils {
    public static String readWhile(StringReader pReader, CharPredicate pPredicate) {
        int i = pReader.getCursor();

        while (pReader.canRead() && pPredicate.test(pReader.peek())) {
            pReader.skip();
        }

        return pReader.getString().substring(i, pReader.getCursor());
    }
}