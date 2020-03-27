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

    @SubCommand
    class MyTool4 implements HasSubCommands {

        @Override
        List<Object> getSubCommands() {
            [new MyTool2(), new MyTool1()]
        }
    }

    @Command
    @SubCommand(path = ["test1"])
    class SubCmd1 {
        Set<String> runMethods = []

        @Command(description = "sub 1")
        public void amethod(@Arg("name") String name, @Arg("age") Integer age, @Arg("leaving") Boolean leaving) {
            runMethods << 'amethod'

        }

        @Command(description = "sub 2")
        public void bmethod(@Arg("name") String name, @Arg("age") Integer age, @Arg("leaving") Boolean leaving) {
            runMethods << 'bmethod'
        }
    }

    @Command
    @SubCommand(path = ["mytool1"])
    class SubCmd2 extends SubCmd1{

    }


    @Command
    @SubCommand(path = ["mytool1", "greet"])
    class SubCmd3 extends SubCmd1{

    }

    @Command
    @SubCommand(path = ["mytool1", "asdf", "xyz"], descriptions = ["desc1", "desc2", "desc3"])
    class SubCmd4 extends SubCmd1{

    }

    class MyTool5 implements HasSubCommands {

        @Override
        List<Object> getSubCommands() {
            [new MyTool2(), new MyTool1()]
        }
    }

    class MyTool6 implements HasSubCommands {

        @Override
        List<Object> getSubCommands() {
            []
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
        def result = tool.runMain(['mytool1', 'greet', helpCmd] as String[], false)
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
        output.output.contains 'Use "test mytool3 [command] help" to get help on any command.'
    }

    def "subcommand no methods with hassubcommands"() {
        given:
            def test = new MyTool4()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test)
        when:
            def result = tool.runMain(['-h'] as String[], false)
        then:
            result == false
            output.output.contains "Available commands:\n"
            output.output.contains '   mytool1 - '
            output.output.contains '   mytool2 - '
            output.output.contains 'Use "test [command] help" to get help on any command.'
    }

    def "subcommand with path"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd1()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['-h'] as String[], false)
        then:
            result == false
            output.output.contains "Available commands:\n"
            output.output.contains '   mytool1 - '
            output.output.contains '   test1   - '
            output.output.contains 'Use "test [command] help" to get help on any command.'
        when:
            output.output = []
            def result2 = tool.runMain(['test1', '-h'] as String[], false)
        then:
            result2 == false
            output.output.contains "Available commands:\n"
            output.output.contains '   amethod - sub 1'
            output.output.contains '   bmethod - sub 2'
            output.output.contains 'Use "test test1 [command] help" to get help on any command.'
    }

    def "subcommand with path can run"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd1()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['test1',method] as String[], false)
        then:
            result
            test2.runMethods.contains method

        where:
            method << ['amethod','bmethod']
    }


    def "subcommand with path extending existing path default subcommand"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd2()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['-h'] as String[], false)
        then:
            result == false
            output.output.contains "Available commands:\n"
            output.output.contains '   amethod - sub 1'
            output.output.contains '   bmethod - sub 2'
            output.output.contains '   greet   - '
            output.output.contains 'Use "test mytool1 [command] help" to get help on any command.'
    }

    def "subcommand with path extending existing path"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd2()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            output.output = []
            def result2 = tool.runMain(['mytool1', '-h'] as String[], false)
        then:
            result2 == false
            output.output.contains "Available commands:\n"
            output.output.contains '   amethod - sub 1'
            output.output.contains '   bmethod - sub 2'
            output.output.contains '   greet   - '
            output.output.contains 'Use "test mytool1 [command] help" to get help on any command.'
    }
    def "subcommand with path extending existing path can run"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd2()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['mytool1',method] as String[], false)
        then:
            result
            test2.runMethods.contains method

        where:
            method << ['amethod','bmethod']
    }
    def "subcommand with path and description sets descriptions"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd4()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['help'] as String[], false)
        then:
            !result
            output.output.contains('Available commands:\n')
            output.output.contains('   mytool1 - desc1')
    }
    def "subcommand with path and description sets descriptions2"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd4()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['mytool1','help'] as String[], false)
        then:
            !result
            output.output[0] == '\nmytool1: desc1\n'
            output.output[1] == ('Available commands:\n')
            output.output[2] == ('   asdf  - desc2')
    }
    def "subcommand with path and description sets descriptions3"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd4()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['mytool1','asdf','help'] as String[], false)
        then:
            !result
            output.output.contains('Available commands:\n')
            output.output.contains('   xyz - desc3')
    }

    def "subcommand with path and description sets descriptions4"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd4()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test1, test2)
        when:
            def result = tool.runMain(['mytool1','asdf','xyz','help'] as String[], false)
        then:
            !result
            output.output[0] == ('\nxyz: desc3\n')
            output.output[1] == ('Available commands:\n')
            output.output[2] == ('   amethod - sub 1')
            output.output[3] == ('   bmethod - sub 2')
    }


    def "subcommand with path extending terminal path fails"() {
        given:
            def test1 = new MyTool1()
            def test2 = new SubCmd3()
            def output = new TestOutput()
        when:
            def tool = ToolBelt.with('test', output, test1, test2)
        then:
            RuntimeException e = thrown()
            e.message.contains("Unable to define subcommand object of type")
            e.message.contains("at path: 'mytool1 greet': The subcommand at path: 'greet' cannot be extended")
    }

    def "only hassubcommands"() {
        given:
            def test = new MyTool5()
            def output = new TestOutput()
            def tool = ToolBelt.with('test', output, test)
        when:
            def result = tool.runMain(['-h'] as String[], false)
        then:
            result == false
            output.output.contains "Available commands:\n"
            output.output.contains '   mytool1 - '
            output.output.contains '   mytool2 - '
            output.output.contains 'Use "test [command] help" to get help on any command.'
    }

    def "only hassubcommands with no commands fails"() {
        given:
            def test = new MyTool6()
            def output = new TestOutput()
        when:
            def tool = ToolBelt.with('test', output, test)
        then:
            IllegalArgumentException e = thrown()
            e.message.contains 'Specified object has no methods with @Command annotation or does not provide subcommands via HasSubCommands: '
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

    class MyError extends Exception {
        MyError(final String var1) {
            super(var1)
        }
    }

    class MyToolEh {
        @Command()
        public boolean amethod(@Arg("name") String name) throws MyError {
            if (name == null) {
                throw new MyError("name is null")
            }
            return true;
        }
    }

    def "error handler for throwable type"() {
        given:
            def output = new TestOutput()
            def test = new MyToolEh()
            def myEh = Mock(ToolBelt.ErrorHandler)
            def tool = ToolBelt.belt('test').
                add(test).
                commandOutput(output).
                handles(MyError, myEh).
                commandInput(new SimpleCommandInput()).
                buckle()

        when:
            def result = tool.runMain(['mytooleh', 'amethod', '--namez', 'bob'] as String[], false)
        then:
            !result
            1 * myEh.handleError({ it instanceof MyError }, _) >> true

    }

    static class TestCH implements ToolBelt.CommandInvoker {
        String[] sawArgs
        boolean result

        @Override
        boolean run(final String[] args) {
            this.sawArgs = args
            return result
        }
        String description;
        boolean helped

        void getHelp() {
            helped = true
        }
    }

    def "command handler handles all args"() {
        given:
            def sut = new TestCH()
            sut.result = expect

            def output = new TestOutput()
            def tool = ToolBelt.belt('test').
                add(sut).
                commandOutput(output).
                commandInput(new SimpleCommandInput()).
                buckle()
        when:
            def result = tool.runMain((['testch'] + args) as String[], false)

        then:
            output.output == []
            result == expect
            sut.sawArgs == args

        where:
            expect | args
            true   | []
            true   | ['asdf']
            false  | []
            false  | ['asdf']
    }

    def "command handler description"() {
        given:
            def sut = new TestCH()
            sut.result = true
            sut.description = expect

            def output = new TestOutput()
            def tool = ToolBelt.belt('test').
                add(sut).
                commandOutput(output).
                defaultHelpCommands().
                commandInput(new SimpleCommandInput()).
                buckle()
        when:
            def result = tool.runMain((['help']) as String[], false)

        then:
            output.output.contains('Available commands:\n')
            output.output.contains('   testch - ' + expect)

        where:
            value  | expect
            null   | ''
            'asdf' | 'asdf'
    }

    def "command handler help"() {
        given:
            def sut = new TestCH()
            sut.result = true

            def output = new TestOutput()
            def tool = ToolBelt.belt('test').
                add(sut).
                commandOutput(output).
                defaultHelpCommands().
                commandInput(new SimpleCommandInput()).
                buckle()
        when:
            def result = tool.runMain((['testch', 'help']) as String[], false)

        then:
            sut.helped
    }
}
