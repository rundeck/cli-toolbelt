package org.rundeck.toolbelt;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Construct subcommands
 */
public class ToolBelt {
    private CommandSet commands;
    private CommandInput inputParser;
    private Set<String> helpCommands;
    private CommandOutput commandOutput;
    private ChannelOutput.Builder channels;
    private OutputFormatter baseFormatter;
    private OutputFormatter formatter;
    private boolean ansiColor;
    private ANSIColorOutput.Builder ansiBuilder = ANSIColorOutput.builder().sink(new SystemOutput());
    private Map<Class<? extends Throwable>, ErrorHandler> errorHandlers = new HashMap<>();

    /**
     * Handle a throwable type
     */
    public interface ErrorHandler {
        /**
         * Handle the throwable
         *
         * @param throwable throwable
         * @param context   command context
         * @return true if the throwable was consumed, false if it should be rethrown
         */
        boolean handleError(Throwable throwable, CommandContext context);
    }

    /**
     * Create a simple CLI tool for the object, using {@link SimpleCommandInput} to parse CLI args into  method
     * parameters
     *
     * @param commands
     */
    public static Tool with(String name, Object... commands) {
        return with(name, new SimpleCommandInput(), commands);
    }

    /**
     * Create a simple CLI tool for the object, using the specified input parser to parse
     * CLI args into  method parameters
     *
     * @param commands
     *
     * @return
     */
    public static Tool with(String name, CommandInput input, Object... commands) {
        return belt(name).defaultHelpCommands()
                         .ansiColorOutput(isAnsiColorEnvEnabled())
                         .commandInput(input)
                         .add(commands)
                         .buckle();
    }

    /**
     * Create a simple CLI tool for the object, using the specified input parser to parse
     * CLI args into  method parameters
     *
     * @param commands
     *
     * @return
     */
    public static Tool with(String name, CommandOutput output, Object... commands) {
        return belt(name).defaultHelpCommands()
                         .commandOutput(output)
                         .commandInput(new SimpleCommandInput())
                         .add(commands)
                         .buckle();
    }

    /**
     * @return true if the TERM env var contains 'color'
     */
    public static boolean isAnsiColorEnvEnabled() {
        return System.getenv("TERM") != null && System.getenv("TERM").contains("color");
    }

    /**
     * Create a simple CLI tool for the object, using the specified input parser to parse
     * CLI args into  method parameters
     *
     * @param commands
     *
     * @return
     */
    public static Tool with(String name, CommandInput input, CommandOutput output, Object... commands) {
        return belt(name).defaultHelpCommands()
                         .commandInput(input)
                         .commandOutput(output)
                         .add(commands)
                         .buckle();
    }

    /**
     * @return new ToolBelt
     */
    public static ToolBelt belt(String name) {
        return new ToolBelt(name);
    }

    protected ToolBelt(String name) {
        commands = new CommandSet(name);
        commands.setShowBanner(true);
        helpCommands = new HashSet<>();
        channels = ChannelOutput.builder();
    }

    /**
     * Add objects as commands
     *
     * @param instance objects
     * @return this
     */
    public ToolBelt add(final Object... instance) {
        Arrays.asList(instance).forEach(this::introspect);
        return this;
    }

    public <T extends Throwable> ToolBelt handles(Class<T> clazz, ErrorHandler handler) {
        errorHandlers.put(clazz, handler);
        return this;
    }

    /**
     * Use "-h","help","?" as help commands
     *
     * @return this
     */
    public ToolBelt defaultHelpCommands() {
        return helpCommands("-h", "--help", "help", "?");
    }

    /**
     * Define commands indicating help
     *
     * @param commands list of commands
     *
     * @return this
     */
    public ToolBelt helpCommands(String... commands) {
        helpCommands.addAll(Arrays.asList(commands));
        return this;
    }

    /**
     * Use system out/err for command output
     *
     * @return this
     */
    public ToolBelt systemOutput() {
        return commandOutput(new SystemOutput());
    }

    /**
     * Set whether ANSI colorized output for system output is enabled
     *
     * @param enabled true/false
     *
     * @return this builder
     */
    public ToolBelt ansiColorOutput(boolean enabled) {
        ansiColor = enabled;
        return this;
    }

    public ANSIColorOutput.Builder ansiColor() {
        return ansiBuilder;
    }

    /**
     * enable ANSI colorized output
     *
     * @return this builder
     */
    public ToolBelt ansiColorOutput() {
        return ansiColorOutput(true);
    }

    /**
     * Use system out/err for command output
     *
     * @return this
     */
    public ToolBelt commandOutput(CommandOutput output) {
        commandOutput = output;
        return this;
    }

    /**
     * Configure channels
     *
     * @return
     */
    public ChannelOutput.Builder channels() {
        return channels;
    }

    /**
     * Format output data with this formatter
     *
     * @return this
     */
    public ToolBelt formatter(OutputFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    /**
     * Display banner for top level help
     *
     * @param text banner text
     *
     * @return this
     */
    public ToolBelt banner(String text) {
        this.commands.banner = () -> text;
        return this;
    }

    /**
     * Display banner for top level help
     *
     * @param resource resource path
     *
     * @return this
     */
    public ToolBelt bannerResource(String resource) {
        return bannerResource(resource, null);
    }

    /**
     * Display banner for top level help
     *
     * @param resource     resource path
     * @param replacements a map of Regex->replacement, to replace values in the loaded resource
     *
     * @return this
     */
    public ToolBelt bannerResource(String resource, Map<String, String> replacements) {
        this.commands.banner = () -> {
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resource);
            if (null != resourceAsStream) {
                try {
                    String result;
                    try (BufferedReader is = new BufferedReader(new InputStreamReader(resourceAsStream))) {
                        result = is.lines().collect(Collectors.joining("\n"));
                    }
                    if (replacements != null && !replacements.isEmpty()) {
                        for (String s : replacements.keySet()) {
                            String val = replacements.get(s);
                            result = result.replaceAll(s, Matcher.quoteReplacement(val));
                        }
                    }
                    return result;
                } catch (IOException e) {

                }
            }
            return null;
        };
        return this;
    }

    public interface CommandContext {

        public CommandInput getInputParser();

        public CommandOutput getOutput();

        public boolean isPrintStackTrace();

        public Map<Class<? extends Throwable>, ErrorHandler> getErrorHandlers();

        public String getSubCommand();

        public List<String> getCommands();
        String getCommandsString();

        public boolean handle(Throwable t, String command) throws RuntimeException;
    }

    private static class CommandContextImpl
            implements CommandContext
    {
        private CommandInput inputParser;
        private CommandOutput output;
        private boolean printStackTrace;
        private Map<Class<? extends Throwable>, ErrorHandler> errorHandlers = new HashMap<>();
        Queue<String> commands = new ArrayDeque<>();


        @Override
        public String getSubCommand() {
            return commands.peek();
        }

        @Override
        public List<String> getCommands() {
            return new ArrayList<>(commands);
        }
        @Override
        public String getCommandsString() {
            return String.join(" ", getCommands());
        }

        void pushCommand(String command) {
            commands.add(command);
        }

        String popCommand() {
            return commands.remove();
        }

        public CommandInput getInputParser() {
            return inputParser;
        }

        public CommandOutput getOutput() {
            return output;
        }

        void setInputParser(CommandInput inputParser) {
            this.inputParser = inputParser;
        }

        public void setOutput(CommandOutput output) {
            this.output = output;
        }

        public boolean isPrintStackTrace() {
            return printStackTrace;
        }

        public void setPrintStackTrace(boolean printStackTrace) {
            this.printStackTrace = printStackTrace;
        }

        public Map<Class<? extends Throwable>, ErrorHandler> getErrorHandlers() {
            return errorHandlers;
        }

        public void setErrorHandlers(Map<Class<? extends Throwable>, ErrorHandler> errorHandlers) {
            this.errorHandlers = errorHandlers;
        }

        public boolean handle(Throwable t, String command) throws RuntimeException {
            for (Class<? extends Throwable> aClass : errorHandlers.keySet()) {
                if (aClass.isAssignableFrom(t.getClass())) {
                    ErrorHandler errorHandler = errorHandlers.get(aClass);
                    return errorHandler.handleError(t, this);
                }
            }
            return false;
        }

    }

    private static class CommandSet implements Tool, CommandInvoker {
        Map<String, CommandInvoker> commands;
        Map<String, CommandInvoker> commandSynonyms;
        String defCommand;
        Set<String> helpCommands;
        private String description;
        CommandContextImpl context;
        private String name;
        private Set<String> synonyms;
        Tool other;
        private boolean showBanner;
        Supplier<String> banner;
        Supplier<Boolean> printStackTrace;
        public boolean hidden;

        CommandSet(String name) {
            this.name = name;
            synonyms = new HashSet<>();
            helpCommands = new HashSet<>();
            commands = new HashMap<>();
            commandSynonyms = new HashMap<>();
            context = new CommandContextImpl();
        }

        @Override
        public boolean isHidden() {
            return hidden;
        }

        @Override
        public boolean isSolo() {
            return false;
        }

        @Override
        public boolean isDefault() {
            return false;
        }

        public CommandSet(final CommandSet commandSet) {
            this.name = commandSet.name;
            this.commands = new HashMap<>(commandSet.commands);
            this.commandSynonyms = new HashMap<>(commandSet.commandSynonyms);
            this.defCommand = commandSet.defCommand;
            this.helpCommands = new HashSet<>(commandSet.helpCommands);
            this.description = commandSet.description;
            this.context = commandSet.context;
            this.synonyms = new HashSet<>(commandSet.synonyms);
        }

        @Override
        public Tool merge(final Tool tool) {
            CommandSet commandSet = new CommandSet(this);
            commandSet.other = tool;
            return commandSet;
        }

        @Override
        public boolean runMain(final String[] args, final boolean exitSystem) {
            boolean result = false;
            try {
                result = run(args);
            } catch (CommandWarning commandRunFailure) {
                context.getOutput().warning(commandRunFailure.getMessage());
            } catch (CommandRunFailure commandRunFailure) {
                context.getOutput().error(commandRunFailure.getMessage());
                //verbose
                if (printStackTrace == null || printStackTrace.get()) {
                    StringWriter sb = new StringWriter();
                    commandRunFailure.printStackTrace(new PrintWriter(sb));
                    context.getOutput().error(sb.toString());
                }
            }
            if (!result && exitSystem) {
                System.exit(2);
            }
            return result;
        }

        @Override
        public boolean run(final String[] args)
                throws CommandRunFailure
        {
            String[] cmdArgs = args;
            String cmd = defCommand;
            if (args.length > 0 && !(args[0].startsWith("-") && null != defCommand)) {
                cmd = args[0];
                cmdArgs = tail(args);
            }
            if (null == cmd) {
                context.getOutput().error(
                        String.format("A command was expected%s",
                                      context.commands.size() > 1 ?
                                      (String.format(": %s [command]", context.getCommandsString())) :
                                      "."
                        )
                );
                getHelp();
                return false;
            }
            if (helpCommands.contains(cmd)) {
                getHelp();
                return false;
            }
            return runCommand(cmd, cmdArgs);
        }

        @Override
        public Set<String> listCommands() {
            TreeSet<String> strings = new TreeSet<>(commands.keySet()
                                                            .stream()
                                                            .filter(name -> !commands.get(name).isHidden())
                                                            .collect(Collectors.toList()));
            if (null != other) {
                strings.addAll(other.listCommands());
            }
            return strings;
        }

        String pad(String pad, int max) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < max; i++) {
                stringBuilder.append(pad);
            }
            return stringBuilder.toString();
        }

        private String shortDescription(final String text) {
            if(text==null){
                return "";
            }
            int i = text.indexOf("\n");
            if (i >= 0) {
                return text.substring(0, i);
            }
            i = text.indexOf(".");
            if (i >= 0) {
                return text.substring(0, i);
            }
            return text;
        }

        @Override
        public void getHelp() {
            getHelp(showBanner);
        }

        public void getHelp(boolean banner) {
            if (banner && null != this.banner) {
                context.getOutput().output(ANSIColorOutput.colorizeTemplate(this.banner.get()));
            }
            if (description != null && !"".equals(description)) {
                context.getOutput().output(
                        ANSIColorOutput.colorize(
                                "\n",
                                ANSIColorOutput.Color.WHITE,
                                name+": ",
                                description + "\n"
                        )
                );
            }

            List<String> subcommands = commands.keySet()
                                                  .stream()
                                                  .sorted()
                                                  .filter(name -> !commands.get(name).isHidden())
                                                  .filter(name -> !commands.get(name).isSolo())
                    .collect(Collectors.toList());
            if(subcommands.size()>0) {
                context.getOutput().output("Available commands:\n");
                int max = subcommands.stream().mapToInt(String::length).max().orElse(10);
                subcommands
                        .forEach(name -> {
                                     context.getOutput()
                                            .output(
                                                    ANSIColorOutput.colorize(
                                                            ANSIColorOutput.Color.YELLOW,
                                                            String.format(
                                                                    "   %s",
                                                                    name
                                                            ),
                                                            String.format(
                                                                    "%s - %s",
                                                                    pad(
                                                                            " ",
                                                                            max -
                                                                            name.length()
                                                                    ),
                                                                    shortDescription(
                                                                            commands.get(name)
                                                                                    .getDescription()
                                                                    )

                                                            )
                                                    )
                                            );
                                 }
                        );
            }
            //find solo command
            commands.values()
                    .stream()
                    .filter(CommandInvoker::isSolo)
                    .findFirst().ifPresent(CommandInvoker::getHelp);
            context.getOutput().output("");
            context.getOutput().output(
                    ANSIColorOutput.colorize(
                            ANSIColorOutput.Color.GREEN,
                            String.format(
                                    "Use \"%s [command] help\" to get help on any command.",
                                    context.getCommandsString()
                            )
                    )
            );

        }
        void deepHelp(){
            for (String command : commands.keySet()) {
                CommandInvoker commandInvoker = commands.get(command);
                if(commandInvoker.isHidden()){
                   continue;
                }


                context.getOutput().output("--------------------");
                context.getOutput().output("+ Command: " + command);
                if (commandInvoker.getSynonyms() != null && commandInvoker.getSynonyms().size() > 0) {
                    context.getOutput().output("+ Synonyms: " + commandInvoker.getSynonyms());
                }

                commandInvoker.getHelp();
            }
            if (null != other) {
                other.getHelp();
            }
        }

        boolean runCommand(String cmd, String[] args) throws CommandRunFailure
        {
            CommandInvoker commandInvoke = findcommand(cmd);
            if (null == commandInvoke) {
                throw new CommandWarning(String.format(
                        "No such command: %s. Available commands: %s",
                        cmd,
                        listCommands()
                ));
            }
            context.pushCommand(cmd);
            if (args.length > 0 && helpCommands.contains(args[0])) {
                commandInvoke.getHelp();
                return false;
            }
            return commandInvoke.run(args);
        }

        /**
         * Find invoker for a command or a synonym
         *
         * @param cmd
         *
         * @return
         */
        private CommandInvoker findcommand(final String cmd) {
            CommandInvoker commandInvoker = commands.get(cmd);
            return commandInvoker != null ? commandInvoker : commandSynonyms.get(cmd);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getSynonyms() {
            return synonyms;
        }

        public void setSynonyms(Set<String> synonyms) {
            this.synonyms = synonyms;
        }

        @Override
        public String getDescription() {
            return description;
        }

        public boolean isShowBanner() {
            return showBanner;
        }

        public void setShowBanner(boolean showBanner) {
            this.showBanner = showBanner;
        }
    }

    public static String[] tail(final String[] args) {
        List<String> strings = new ArrayList<>(Arrays.asList(args));
        strings.remove(0);
        return strings.toArray(new String[strings.size()]);
    }
    public static List<String> tail(final List<String> args) {
        List<String> strings = new ArrayList<>(args);
        if(strings.size()>0) {
            strings.remove(0);
        }
        return strings;
    }

    private void introspect(final Object instance) {
        if(instance instanceof CommandInvoker){
            CommandInvoker cmd=(CommandInvoker)instance;
            commands.commands.put(cmd.getName(), cmd);
            if(cmd.getSynonyms()!=null && cmd.getSynonyms().size()>0){
                cmd.getSynonyms().forEach(synonym -> commands.commandSynonyms.put(synonym, cmd));
            }
            return;
        }
        introspect(commands, instance);
    }

    /**
     * determine container/commands from annotations on an object, and add to the command set.
     *
     * @param parent
     * @param instance
     */
    private void introspect(CommandSet parent, final Object instance) {
        Class<?> aClass = instance.getClass();
        SubCommand annotation1 = aClass.getAnnotation(SubCommand.class);
        List<String> path = new ArrayList<>();
        if (null != annotation1 && annotation1.path().length > 0) {
            path.addAll(Arrays.asList(annotation1.path()));
        }
        List<String> descriptions = new ArrayList<>();
        if (null != annotation1 && annotation1.descriptions().length > 0) {
            descriptions.addAll(Arrays.asList(annotation1.descriptions()));
        }
        if (path.size() > 0) {
            try {
                parent = locatePath(parent, path, descriptions);
            } catch (InvalidPath invalidPath) {
                throw new RuntimeException(String.format(
                        "Unable to define subcommand object of type %s at path: '%s': %s",
                        instance.getClass().getName(),
                        String.join(" ", path),
                        invalidPath.getMessage()
                ), invalidPath);
            }
        }
        addCommandForParent(parent, instance);
    }

    /**
     * Given a parent and a path, return a CommandSet matching the path
     *
     * @param parent
     * @param path
     */
    private CommandSet locatePath(final CommandSet parent, final List<String> path, final List<String>  descriptions) throws InvalidPath {
        if (path == null || path.size() < 1) {
            return parent;
        }
        String part = path.get(0);
        CommandSet sub = null;
        CommandInvoker commandInvoker = parent.commands.get(part);
        if (null == commandInvoker) {

            CommandSet commandSet = new CommandSet(part);
            commandSet.hidden = false;
            commandSet.context = commands.context;
            commandSet.helpCommands = helpCommands;

            if(descriptions.size() > 0 && !"".equals(descriptions.get(0))) {
                commandSet.description = descriptions.get(0);
            }

            parent.commands.put(part, commandSet);
            sub = commandSet;
        } else if (commandInvoker instanceof CommandSet) {
            sub = (CommandSet) commandInvoker;
            if (null == sub.description && descriptions.size() > 0 && !"".equals(descriptions.get(0))) {
                sub.description = descriptions.get(0);
            }
        } else {
            //TODO: construct a commandset and add invoker as default command
            throw new InvalidPath(String.format(
                    "The subcommand at path: '%s' cannot be extended",
                    String.join(" ", path)
            ));
        }

        return locatePath(sub, tail(path), tail(descriptions));
    }

    static class InvalidPath
            extends Exception
    {
        public InvalidPath(final String message) {
            super(message);
        }
    }

    private void addCommandForParent(CommandSet parent, final Object instance) {
        HashMap<String, CommandInvoker> subCommands = new HashMap<>();
        HashMap<String, CommandInvoker> subSynonyms = new HashMap<>();
        //look for methods
        Class<?> aClass = instance.getClass();
        Command annotation1 = aClass.getAnnotation(Command.class);
        Set<String> synonyms = new HashSet<>();
        String cmd = null != annotation1 ? annotation1.value() : "";
        if ("".equals(cmd)) {
            cmd = aClass.getSimpleName().toLowerCase();
        }
        if (null != annotation1 && annotation1.synonyms().length > 0) {
            synonyms.addAll(Arrays.asList(annotation1.synonyms()));
        }
        String cmdDescription = null != annotation1 ? annotation1.description() : null;
        boolean isSub = false;

        SubCommand subcmdAnnotation = aClass.getAnnotation(SubCommand.class);
        if (null != subcmdAnnotation) {
            isSub = true;
        }

        boolean isHidden = false;
        Hidden annotation2 = aClass.getAnnotation(Hidden.class);
        if (null != annotation2) {
            isHidden = true;
        }
        Method[] methods = aClass.getMethods();
        String defInvoke = null;
        for (Method method : methods) {
            Command annotation = method.getAnnotation(Command.class);
            if (annotation != null) {
                String name = annotation.value();
                if ("".equals(name)) {
                    name = method.getName().toLowerCase();
                }
                MethodInvoker value = new MethodInvoker(name, method, instance, commands.context);
                value.description = annotation.description();
                value.solo = annotation.isSolo();
                value.hidden = annotation.isHidden();
                Set<String> annotationSynonyms = new HashSet<>();
                if (annotation.synonyms().length > 0) {
                    annotationSynonyms.addAll(Arrays.asList(annotation.synonyms()));
                }
                value.setSynonyms(annotationSynonyms);
                subCommands.put(name, value);
                for (String subsynonym : annotationSynonyms) {
                    subSynonyms.put(subsynonym, value);
                }

                if (annotation.isDefault()) {
                    defInvoke = name;
                }
            }
        }

        CommandSet commandSet = new CommandSet(cmd);
        commandSet.hidden = isHidden;
        commandSet.context = commands.context;
        commandSet.helpCommands = helpCommands;
        commandSet.description = cmdDescription;

        if (instance instanceof HasSubCommands) {
            if (subCommands.size() < 1) {
                isSub = true;
            }
            HasSubCommands subs = (HasSubCommands) instance;
            List<Object> subCommands1 = subs.getSubCommands();
            subCommands1.forEach(o -> introspect(commandSet, o));
        }
        if (commandSet.commands.size() < 1 && subCommands.size() < 1) {
            throw new IllegalArgumentException(
                    "Specified object has no methods with @Command annotation or does not provide subcommands via HasSubCommands: "
                    + aClass);
        }

        commandSet.commands.putAll(subCommands);
        commandSet.commandSynonyms.putAll(subSynonyms);
        commandSet.defCommand = defInvoke;
        if (commandSet.commands.size() == 1) {
            //single command
            commandSet.defCommand = commandSet.commands.keySet().iterator().next();
        }
        if (!isSub) {
            parent.commands.put(cmd, commandSet);
            synonyms.forEach(syn -> parent.commandSynonyms.put(syn, commandSet));
        } else {
            parent.commands.putAll(commandSet.commands);
            parent.commandSynonyms.putAll(subSynonyms);
        }

    }

    /**
     * Set input parser
     *
     * @param input input parser
     *
     * @return this
     */
    public ToolBelt commandInput(CommandInput input) {
        this.inputParser = input;
        return this;
    }

    /**
     * Enable or disable stacktrace printing on error
     *
     * @param printStackTrace true to print stack traces (default)
     *
     * @return this
     */
    public ToolBelt printStackTrace(boolean printStackTrace) {
        commands.printStackTrace = () -> printStackTrace;
        return this;
    }

    /**
     * Build the Tool
     *
     * @return new Tool
     */
    public Tool buckle() {
        commands.context.setInputParser(inputParser);
        errorHandlers.put(InputError.class, (err, context) -> {
            context.getOutput().warning(String.format(
                    "Input error for [%s]: %s",
                    context.getCommandsString(),
                    err.getMessage()
            ));
            context.getOutput().warning(String.format(
                    "You can use: \"%s %s\" to get help.",
                    context.getCommandsString(),
                    helpCommands.iterator().next()
            ));
            return true;
        });
        commands.context.setErrorHandlers(errorHandlers);
        commands.helpCommands = helpCommands;
        if (commands.commands.size() == 1) {
            commands.defCommand = commands.commands.keySet().iterator().next();
        }
        commands.context.pushCommand(commands.name);
        commands.context.setOutput(finalOutput());
        return commands;
    }

    private CommandOutput builtOutput;
    public ToolBelt finalOutput(CommandOutput output) {
        this.builtOutput = output;
        return this;
    }
    public CommandOutput finalOutput() {
        if (null == commandOutput) {
            commandOutput(defaultOutput());
        }
        baseFormatter = defaultBaseFormatter();
        channels.fallback(commandOutput);
        ChannelOutput channel = channels.build();
        if (null == builtOutput) {
            builtOutput = new FormattedOutput(
                    channel,
                    null != formatter ? formatter.withBase(baseFormatter) : baseFormatter
            );
        }
        return builtOutput;
    }

    public OutputFormatter defaultBaseFormatter() {
        return new NiceFormatter(ansiColor ? ansiBuilder.build() : new ToStringFormatter());
    }

    public CommandOutput defaultOutput() {
        return ansiColor ? ansiBuilder.build() : new SystemOutput();
    }

    public static interface CommandInvoker {
        String getName();

        String getDescription();

        boolean isSolo();

        boolean isDefault();

        boolean isHidden();

        Set<String> getSynonyms();

        boolean run(String[] args) throws CommandRunFailure;

        void getHelp();
    }

    private static class MethodInvoker implements CommandInvoker {
        private String name;
        private Set<String> synonyms;
        Method method;
        Object instance;
        private String description;
        private boolean solo;
        private boolean hidden;
        private boolean isdefault;
        CommandContext context;

        MethodInvoker(
                final String name,
                final Method method,
                final Object instance,
                final CommandContext context
        )
        {
            this.name = name;
            this.method = method;
            this.instance = instance;
            this.context = context;
        }


        public boolean run(String[] args) throws CommandRunFailure {
            //get configured arguments to the method
            Class[] parameters = method.getParameterTypes();
            Parameter[] params = method.getParameters();
            Object[] objArgs = new Object[parameters.length];
            for (int i = 0; i < params.length; i++) {
                Class<?> type = parameters[i];
                String paramName = getParameterName(params[i]);

                if (type.isAssignableFrom(CommandOutput.class)) {
                    objArgs[i] = context.getOutput();
                } else if (type.isAssignableFrom(String[].class)) {
                    objArgs[i] = args;
                } else {
                    Object t = null;
                    try {
                        t = context.getInputParser().parseArgs(name, args, type, paramName);
                    } catch (InputError inputError) {
                        if (context.handle(inputError, name)) {
                            return false;
                        }
                        inputError.printStackTrace();
                        return false;
                    }

                    objArgs[i] = t;
                }
            }
            Object invoke = null;
            try {
                invoke = method.invoke(instance, objArgs);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                if (e.getCause() != null) {
                    if (context.handle(e.getCause(), name)) {
                        return false;
                    }
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    }
                    if (e.getCause() instanceof CommandRunFailure) {
                        throw (CommandRunFailure) e.getCause();
                    }
                    e.getCause().printStackTrace();
                } else {
                    e.printStackTrace();
                }
                return false;
            }
            if (invoke != null && (invoke instanceof Boolean || invoke.getClass().equals(boolean.class))) {
                return ((Boolean) invoke);
            }
            //TODO: format output
            return true;
        }

        @Override
        public void getHelp() {
            Parameter[] params = method.getParameters();
            if (description != null && !"".equals(description)) {
                context.getOutput().output(
                        ANSIColorOutput.colorize(
                                ANSIColorOutput.Color.WHITE,
                                description + "\n"
                        )
                );
            }
            if (params.length == 0) {
                context.getOutput().output(
                        ANSIColorOutput.colorize(
                                ANSIColorOutput.Color.GREEN,
                                "(no options for this command)"
                        )
                );
            }
            for (int i = 0; i < params.length; i++) {
                Class<?> type = params[i].getType();
                String paramName = getParameterName(params[i]);
                if (type.isAssignableFrom(CommandOutput.class) || type.isAssignableFrom(String[].class)) {
                    continue;
                }

                String helpt = context.getInputParser().getHelp(name, type, paramName);

                context.getOutput().output(helpt);
            }
        }


        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getSynonyms() {
            return synonyms;
        }

        public void setSynonyms(Set<String> synonyms) {
            this.synonyms = synonyms;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isSolo() {
            return solo;
        }

        @Override
        public boolean isDefault() {
            return isdefault;
        }

        @Override
        public boolean isHidden() {
            return hidden;
        }
    }

    private static String getParameterName(final Parameter param) {
        if (param.getAnnotation(Arg.class) != null) {
            Arg annotation = param.getAnnotation(Arg.class);
            if (!"".equals(annotation.value())) {
                return annotation.value();
            }
        }
        return param.getName();
    }
}
