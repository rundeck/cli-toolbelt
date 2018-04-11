package org.rundeck.toolbelt

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 6/1/16.
 */
@Unroll
class ToolBeltSpec extends Specification {
    class MyTool1 {
        String name
        int age
        boolean leaving
        boolean greetResult

        @Command
        public boolean greet(@Arg("name") String name, @Arg("age") int age, @Arg("leaving") boolean leaving) {
            this.name = name
            this.age = age
            this.leaving = leaving
            greetResult
        }
    }

    class MyTool2 {
        String name
        int age
        boolean leaving
        boolean greetResult

        @Command(isSolo = true)
        public boolean greet(@Arg("name") String name, @Arg("age") int age, @Arg("leaving") boolean leaving) {
            this.name = name
            this.age = age
            this.leaving = leaving
            greetResult
        }
    }

    class MyTool3 implements HasSubCommands {
        String name
        int age
        boolean leaving
        boolean greetResult

        @Override
        List<Object> getSubCommands() {
            [new MyTool2()]
        }

        @Command()
        public boolean amethod(@Arg("name") String name, @Arg("age") int age, @Arg("leaving") boolean leaving) {
            this.name = name
            this.age = age
            this.leaving = leaving
            greetResult
        }
    }

    class TestOutput implements CommandOutput {
        List<Object> output = []
        List<Object> error = []
        List<Object> warning = []
        List<Object> info = []

        @Override
        void info(final Object output) {
            this.info << output
        }

        @Override
        void output(final Object output) {
            this.output << output
        }

        @Override
        void error(final Object error) {
            this.error << error
        }

        @Override
        void warning(final Object error) {
            this.warning << error
        }
    }

    def "bootstrap with param names"() {
        given:
        def test = new MyTool1()
        test.greetResult = true
        def tool = ToolBelt.with('test',test)
        when:
        def result = tool.runMain(['mytool1', 'greet', '--name', 'bob', '--age', '54', '--leaving'] as String[], false)
        then:
        result
        test.name == 'bob'
        test.age == 54
        test.leaving == true
    }

    def "bootstrap help with #helpCmd"() {
        given:
        def test = new MyTool1()
        test.greetResult = true
        def output = new TestOutput()
        def tool = ToolBelt.with('test',output, test)
        when:
        def result = tool.runMain(['mytool1', helpCmd] as String[], false)
        then:
        !result
        test.name == null
        test.age == 0
        test.leaving == false
        output.output == ['--name <String>', '--age <int>', '--leaving']

        where:
        helpCmd  | _
        '--help' | _
        'help' | _
        '-h' | _
        '?' | _
    }

    def "bootstrap tool fails"() {
        given:
        def test = new MyTool1()
        test.greetResult = false
        def tool = ToolBelt.with('test',test)
        when:
        def result = tool.runMain(['mytool1', 'greet', '--name', 'bob', '--age', '54', '--leaving'] as String[], false)
        then:
        !result
        test.name == 'bob'
        test.age == 54
        test.leaving == true
    }

    def " solo command"() {
        given:
        def test = new MyTool2()
        def tool = ToolBelt.with('test',test)
        when:
        def result = tool.runMain(['mytool2', '--name', 'bob', '--age', '54', '--leaving'] as String[], false)
        then:
        test.name == 'bob'
        test.age == 54
        test.leaving == true
        result == false
    }

    def "single command method with hassubcommands"() {
        given:
        def test = new MyTool3()
        def output = new TestOutput()
        def tool = ToolBelt.with('test', output, test)
        when:
        def result = tool.runMain(['mytool3'] as String[], false)
        then:
        result == false
        output.output.contains "Available commands:\n"
        output.output.contains '   amethod - '
        output.output.contains '   mytool2 - '
        output.output.contains 'Use "mytool3 [command] help" to get help on any command.'
    }

    class ColorTool {

        @Command(isSolo = true)
        public void greet(CommandOutput out) {
            out.output(ANSIColorOutput.colorize(ANSIColorOutput.Color.BLUE, "test"))
        }
    }

    def "ansi color enabled"() {
        given:
        def test = new ColorTool()
        def output = new TestOutput()
        def tool = ToolBelt.belt('test').add(test).ansiColorOutput(isenabled).commandOutput(output).buckle()

        when:
        def result = tool.runMain(['colortool'] as String[], false)
        then:
        output.output == [expect]

        where:
        isenabled | expect
        true      | '\u001B[34mtest\u001B[0m'
        false     | "test"
    }
}
