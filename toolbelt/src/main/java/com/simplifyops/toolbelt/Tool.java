package com.simplifyops.toolbelt;

import java.util.Set;

/**
 * a CLI tool can run main arguments, or subcommand arguments
 */
public interface Tool {
    /**
     * Run main arguments
     *
     * @param args       arguments
     * @param exitSystem true to perform System.exit(2) on failure
     *
     * @return true/false if the result succeeded
     *
     * @throws CommandRunFailure
     */
    boolean runMain(String[] args, final boolean exitSystem) throws CommandRunFailure;

    Tool merge(Tool tool);

    Set<String> listCommands();

    void getHelp();
}
