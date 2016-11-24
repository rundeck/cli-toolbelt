package com.simplifyops.toolbelt.groovy

import com.simplifyops.toolbelt.ANSIColorOutput
import com.simplifyops.toolbelt.CommandInput
import com.simplifyops.toolbelt.CommandOutput
import com.simplifyops.toolbelt.CommandRunFailure
import com.simplifyops.toolbelt.InputError
import com.simplifyops.toolbelt.SimpleCommandInput
import com.simplifyops.toolbelt.Tool
import com.simplifyops.toolbelt.ToolBelt
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Groovy style builder for toolbelt
 */
class Toolbelt {
    public static final ArrayList<String> realProperties = ['commandInput', 'exit','builder', 'builtContext']
    @Delegate
    final ToolBelt builder
    private String[] args
    SimpleCommandInput commandInput
    private Map<String, CommandBuilder> cmdBuilders = [:]
    private boolean autoInvoke = true
    boolean exit = true
    CommandContext builtContext

    Toolbelt(final String name, String[] args, CommandContext context) {
        this.builder = ToolBelt.belt(name).defaultHelpCommands().ansiColorOutput(System.getenv("ANSI_COLOR") != "0")
        this.commandInput = new SimpleCommandInput()
        this.args = args
        this.builtContext = context ?: new CommandContext();
    }

    String invoked() {
        if (args.length > 0) {
            return args[0]
        }
        null
    }

    String[] subargs() {
        if (args != null && args.length > 1) {
            return args[1..-1]
        }
        return new String[0]
    }

    static def toolbelt(
            String name = "",
            String[] args = null,
            @DelegatesTo(ToolBelt) Closure closure,
            autoInvoke = true

    )
    {
        internal_toolbelt(name, args, closure, autoInvoke, null)
    }
    private static def internal_toolbelt(
            String name = "",
            String[] args = null,
            @DelegatesTo(ToolBelt) Closure closure,
            autoInvoke = true,
            CommandContext context
    )
    {
        def tb = new Toolbelt(name, args, context)
        tb.autoInvoke = autoInvoke
        closure.delegate = tb
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.call()
        //finish builders
        tb.finish()
        if (context) {
            tb.builder.finalOutput(context.commandOutput)
        }
        def tool = tb.builder.buckle()
        tb.builtContext.commandOutput = tb.builder.finalOutput()
        if (null != args && autoInvoke) {
            return tool.runMain(args, true)
        } else {
            return tool
        }
    }

    private finish() {
        def invokeContext=[
                commandContext:this.builtContext,
                solo: false
        ]
        builder.add(
            cmdBuilders.collect { k, cmd ->
                new ClosureInvoker(invokeContext + cmd.asMap())
            } as Object[]
        )
    }
    static class CommandContext{
        CommandInput commandInput
        CommandOutput commandOutput
    }

    static class ClosureInvoker implements ToolBelt.CommandInvoker {
        Closure closure
        String name
        String description
        boolean solo
        Set<String> synonyms = []
        Map<String, Class> arguments = [:]
        CommandContext commandContext

        @Override
        boolean isHidden() {
            return false
        }

        @Override
        boolean isDefault() {
            return false
        }

        @Override
        boolean run(final String[] args) throws CommandRunFailure, InputError {
            Map<String, Object> params = [:]
            for (String key : arguments.keySet()) {
                params[key] = commandContext.commandInput.parseArgs(name, args, arguments.get(key), key)
            }
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = params
            def arg = params
            if (params.size() == 1) {
                arg = params.values().first()
            }

            def val = newclos.call(arg)
            return val != null ? val : true
        }

        @Override
        void getHelp() {
            commandContext.commandOutput.output(
                    ANSIColorOutput.colorize(
                            "Command: ",
                            ANSIColorOutput.Color.ORANGE,
                            name + "\n"
                    )
            )
            if (description) {
                commandContext.commandOutput.output(ANSIColorOutput.colorize(
                        ANSIColorOutput.Color.WHITE,
                        description + "\n"
                ))
            }
            for (String syn : synonyms) {
                commandContext.commandOutput.output("Alias: $syn")
            }
            for (String arg : arguments.keySet()) {
                commandContext.commandOutput.output("--$arg : ${arguments[arg].simpleName}")
            }

        }
    }

    @Override
    Object getProperty(final String property) {
        if (!(property in realProperties)) {
            return invokeMethod(property, new Object[0])
        }

        return metaClass.getProperty(this, property)
    }

    @Override
    Object invokeMethod(final String name, final Object args) {
        List list = InvokerHelper.asList(args);
//        println("invoke $name: $list")

        CommandBuilder cmd = cmdBuilders[name]
        boolean found = cmd != null
        if (!cmd) {
            def details = [
                    name       : name,
                    description: null,
                    synonyms   : [] as Set,
                    arguments  : [:],
                    tbuilder    : this
            ]
            cmd = new CommandBuilder(details).run {
                println("Error: No implementation for $name")
                return false
            }
            cmdBuilders.put name, cmd
        }
        if (list.size() > 0) {
            if (list[0] instanceof Map) {
                cmd.arguments.putAll(list[0])
            } else if (list[0] instanceof Class) {
                cmd.arguments.put('_default', list[0])
            } else if (list[0] instanceof String) {
                if (name == 'command') {
                    cmd.name = list[0]
                    if (list.size() > 1 && list[1] instanceof String) {
                        cmd.description = list[1]
                    }
                } else {
                    cmd.description = list[0]
                }
            }
            if (list[-1] instanceof Closure) {
                Closure call = ((Closure) list[-1]).clone()
                call.delegate = cmd
                call.resolveStrategy = Closure.DELEGATE_ONLY
                call.call()
            }
        }

//            builder.add(new ClosureInvoker(details))
        return cmd
//        throw new MissingMethodException(name, com.simplifyops.toolbelt.groovy.Toolbelt, args, false)
    }

    static class CommandBuilder {
        private Toolbelt tbuilder
        String name
        String description
        Closure run = {}
        Map<String, Object> arguments = [:]
        Set<String> synonyms = []

        Map asMap() {
            [
                    name       : name,
                    description: description,
                    closure    : run,
                    arguments  : arguments,
                    synonyms   : synonyms,
                    solo       : false
            ]
        }

        def name(String name) {
            this.name = name
            this
        }

        def description(String description) {
            this.description = description
            this
        }

        def arguments(Map<String, Object> arguments) {
            this.arguments.putAll(arguments)
            this
        }

        def synonyms(String... synonyms) {
            this.synonyms.addAll(synonyms)
            this
        }

        def run(Closure clos) {
            this.run = clos
            this
        }

        //drop into a new subcommand set
        def subcommands(Closure clos) {
            def subargs = tbuilder.subargs()
            Tool tb = Toolbelt.internal_toolbelt(name, subargs, clos, false,tbuilder.builtContext)
            boolean doExit = tbuilder.exit
            run = {
                tb.runMain(subargs, doExit)
            }
            this
        }
    }
}
