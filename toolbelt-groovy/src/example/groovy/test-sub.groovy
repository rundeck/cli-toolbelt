/**
 * Example using groovy DSL for subcommand definition
 */
@Grab(group = 'com.simplifyops.toolbelt', module = 'toolbelt-groovy', version = '0.1.1-SNAPSHOT')
import static com.simplifyops.toolbelt.groovy.Toolbelt.toolbelt


toolbelt('test', args) {

    greet {
        description "greetings"
        subcommands {
            hello("echo hello --name <your name>") {
                arguments['name'] = String
                run {
                    println "Well hello, $name"
                }
            }

            goodbye.run {
                println "Adios"
            }
        }
    }
    yell {
        description "yells or something"
        subcommands {
            bogus.run {
                println 'bogus command'
            }
            harken.run {
                println "ye olde time"
            }
        }
    }

}