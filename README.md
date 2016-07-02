Defines a set of commands with subcommands, using annotations to indicate methods to expose as subcommands. 

"Commands" represent an invocable named action, with optional arguments.  A "Command" may also be a container
for other "Commands" (i.e. "Subcommands"). In that case, the parent "command" is called a "command container". 

The simplest structure is a Class with Methods, where the Class is the command container, and the methods are the
sub commands. 

For further nesting, the class can implement {@link com.simplifyops.toolbelt.HasSubCommands} and return
other command container objects. 

Simplest usage: 


~~~ {.java}
class Greet{
   @Command void hi(@Arg("name") String name){
       System.out.println("Hello, "+name+".");
   }

   @Command void remark(@Arg("age") int age){
       System.out.println("I see you are "+age+" years old.");
   }
}

class Main{
   public static void main(String[] args){
       ToolBelt.with(new Greet()).runMain(args);
   }
}
~~~

Commandline:  


   $ java ... Main greet hi --name bob

   Hello, bob.

   $ java ... Main greet remark --age 33

   I see you are 33 years old.



This constructs a `Tool` object, with a command "greet" (based on the class name
Greet).  "greet" has a "hi" and a "remark" subcommand. The class must have at least 
one method annotated with `@Command`.  The parameters of that
method (if any) should be annotated with `@Arg` to define their names.
(Alternately, if you compile your java class with `-parameters` flag to javac, the parameter names will be
introspected.)

This will use the `SimpleCommandInput` to parse "--somearg value" for a
method parameter with arg name "somearg".

You can define multiple commands (with their subcommands) in one step: 

           ToolBelt.with(new Command1(), new Command2(),...).runMain(args);

For more advanced usage, see below:

Use `com.simplifyops.toolbelt.ToolBelt#belt()` to create a builder.


~~~~ {.java}
ToolBelt.belt()
 .defaultHelp()
 .systemOutput()
 .addCommands(
   new MyCommand(),
   new MyCommand2()
 )
 .setParser(new JewelInput())
 .buckle();
~~~~


Within your MyCommand classes, use the {@link com.simplifyops.toolbelt.Command @Command} annotation to indicate the
class is a top-level command (optional).  Add the same annotation on any methods within the class to expose them
as subcommands. At least one method should be annotated this way.


~~~ {.java}
@Command(description = "Does something", name="doit")
public class MyCommand{
   @Command(name="sub1") public void sub1(InputArgs args){
       System.out.println("Input args: "+args);
   }
}
~~~


The annotation can exclude the "name" attribute, and the lowercase name of the class or method is used as the
command/subcommand.  The method parameters will be parsed using the input parser (e.g. JewelCLI), so
`InputArgs` must be defined with appropriate annotations.


## Using HasSubCommand interface:


If a @Command annotated class wants to define a subcommand which is also a container (has subcommands of
its own), It should implement {@link com.simplifyops.toolbelt.HasSubCommands}, and return a list of command
container objects.


~~~{.java}
@Command class First implements HasSubCommands{

   @Command void something(){

       System.out.println("This method prints something")

   }

   List&lt;Object&gt; getCommands(){

       return Arrays.asList(new Second());

   }
}
@Command class Second{

   @Command void third(){

       System.out.println("Third level nested command");

   }
}
~~~

This will define an interface like:


   $ java ... First

   Available commands: [something, second]

   $ java ... First something

   This method prints something

   $ java ... First second

   Availabe commands: [third]

   $ java ... First second third

   Third level nested command
