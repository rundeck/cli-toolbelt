/**
 * Example using groovy DSL for command definition
 */
@GrabResolver(name='jitpack', root='https://jitpack.io')
@Grab('com.github.simplifyops.cli-toolbelt:toolbelt-groovy:-SNAPSHOT')
import static org.rundeck.toolbelt.groovy.Toolbelt.toolbelt


toolbelt('test', args) {

    /**
     * Defines a command 'blah' with input argument '--peace'
     */
    blah(peace: String).run {
        println "peace is $peace"
        println "peace is also $it"
    }

    /**
     * Define command with a description
     */
    dance "Subsist on berries while undulating randomly"
    dance(howlong: Integer, where: String).run {
        println "Dance for $howlong days, in $where"
    }

    hello {
        description 'Milk duds'
        arguments(
                filch: String,
                gromber: Integer
        )
        run {
            println("what is going on? args are ${it}")
        }
    }

    contortion("Contort yourself into something else").run {
        println 'this is doggos'
    }

    command("foobar", "a weird command") {
        arguments(ziptie: String)
        run {
            println "click alot of $ziptie"
        }
    }

    widget "something about widgets"
    widget(name: String)
    widget().run {
        println("The name is $name")
    }
}
