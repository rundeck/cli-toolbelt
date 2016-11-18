package com.simplifyops.toolbelt;

import java.util.regex.Pattern;

import static com.simplifyops.toolbelt.NiceFormatter.NL;

/**
 * Created by greg on 11/18/16.
 */
public class PrefixFormatter implements OutputFormatter {
    String prefix;
    OutputFormatter base;

    public PrefixFormatter(final String prefix) {
        this.prefix = prefix;
        this.base = new ToStringFormatter();
    }

    public PrefixFormatter(final String prefix, final OutputFormatter base) {
        this.prefix = prefix;
        this.base = base;
    }

    @Override
    public String format(final Object o) {
        return addPrefix(prefix, null != base ? base.format(o) : o.toString());
    }

    private String addPrefix(final String prefix, final String text) {
        StringBuilder sb = new StringBuilder();
        indent(text, sb, true, prefix);
        return sb.toString();
    }

    private void indent(final String text, final StringBuilder sb, final boolean firstLine, final String prefix) {
        if (text.contains(NL)) {
            for (String s : text.split(Pattern.quote(NL))) {
                sb.append(this.prefix).append(s).append(NL);
            }
        } else {
            sb.append(this.prefix).append(text);
        }
    }

    @Override
    public OutputFormatter withBase(final OutputFormatter base) {
        return new PrefixFormatter(prefix, base);
    }
}
