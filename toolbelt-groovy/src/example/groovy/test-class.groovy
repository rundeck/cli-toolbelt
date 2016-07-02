/**
 * Example using a class annotated with toolbelt Command/Arg
 *
 */
@Grab(group = 'com.simplifyops.toolbelt', module = 'toolbelt-groovy', version = '0.1.1-SNAPSHOT')
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