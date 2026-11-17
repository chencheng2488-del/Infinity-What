package com.bandybus.infwhat.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long item tooltip lines so they fit on screen.
 *
 * <p>1.6.4's {@code GuiContainer} flips a tooltip to the left of the cursor when it overflows the
 * right edge but never clamps the resulting x, so a single very long line can start off-screen.
 * The mods' Chinese descriptions are long sentences, hence this small wrapper.
 */
public final class TooltipHelper {

    private TooltipHelper() {
    }

    /**
     * Wraps one line into chunks of at most {@code maxChars} characters, preferring to break after
     * Chinese/English punctuation and spaces.
     */
    public static List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<String>();
        if (text == null) {
            return lines;
        }
        String remaining = text.trim();
        while (remaining.length() > maxChars) {
            int cut = maxChars;
            for (int i = maxChars; i > 0; i--) {
                char candidate = remaining.charAt(i - 1);
                if (candidate == '\u3002' || candidate == '\uFF1B' || candidate == ';'
                        || candidate == '\uFF0C' || candidate == ',' || candidate == '\u3001'
                        || candidate == '\uFF01' || candidate == '!' || candidate == '\uFF1F'
                        || candidate == '?' || candidate == ' ') {
                    cut = i;
                    break;
                }
            }
            String line = remaining.substring(0, cut).trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }
}
