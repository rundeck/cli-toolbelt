package org.rundeck.toolbelt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a type as a "subcommand", which will contain command methods but not expose a top-level name for itself
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SubCommand {
    /**
     * Defines a relative path of subcommands that should be the parent of this command
     *
     * @return path of subcommand parents
     */
    String[] path() default {};
    /**
     * Defines descriptions of the parent subcommands, if any, that are not already defined, this should
     * be the same length as path(), empty strings will be skipped.
     *
     * @return descriptions of subcommand parents
     */
    String[] descriptions() default {};
}
