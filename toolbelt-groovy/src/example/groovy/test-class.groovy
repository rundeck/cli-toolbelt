/**
 * Example using a class annotated with toolbelt Command/Arg
 *
 */
@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.simplifyops.cli-toolbelt:toolbelt-groovy:-SNAPSHOT')
import static com.simplifyops.toolbelt.groovy.Toolbelt.toolbelt

import com.simplifyops.toolbelt.Command
import com.simplifyops.toolbelt.Arg

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