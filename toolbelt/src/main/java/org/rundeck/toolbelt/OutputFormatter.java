package org.rundeck.toolbelt;

/**
 * Format object output
 */
public interface OutputFormatter {
    String format(Object o);

    OutputFormatter withBase(OutputFormatter base);
}
