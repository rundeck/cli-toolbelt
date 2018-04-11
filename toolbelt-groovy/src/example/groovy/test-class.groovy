/**
 * Example using a class annotated with toolbelt Command/Arg
 *
 */
@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.simplifyops.cli-toolbelt:toolbelt-groovy:-SNAPSHOT')
import static org.rundeck.toolbelt.groovy.Toolbelt.toolbelt

import org.rundeck.toolbelt.Command
import org.rundeck.toolbelt.Arg

class MyCommand {
    @Command
    blah(@Arg("peace") String peace) {
        println "${peace}, you say?"
    }
}
//ToolBelt.with('test',new MyCommand()).runMain(args,true)
toolbelt('test', args) {
    add new MyCommand()
}
