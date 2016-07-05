package example;

import com.simplifyops.toolbelt.Arg;
import com.simplifyops.toolbelt.Command;

/**
 * Created by greg on 7/5/16.
 */
@Command(description = "Easily add a command 'container' with a set of subcommands.\n\n" +
                       "Try: example sub demo1 ")
public class Sub {

    @Command(description = "Arguments can be parsed automatically")
    public void demo1(@Arg("arg1") String astring, @Arg("arg2") Integer anint) {
        if (null != anint) {

            System.out.printf("You specified an integer for --arg2: %d%n", anint);
            System.out.println();
            System.out.println("Try: example sub demo1 --arg2 not-an-integer");
            System.out.println("(Note: This will cause an error!)");
        } else if (null == astring) {
            System.out.println("Arguments can be easily added: (you didn't pass one)");
            System.out.println("Try: example sub demo1 --arg1 your-name");
        } else {
            System.out.printf("Hi, %s!%n", astring);

            System.out.println();
            System.out.println("Try: example sub demo1 --arg2 123");
        }
    }

    @Command(description = "Or passed directly")
    public void demo2(String[] args) {
        System.out.printf("You said what? %d words%n", args.length);
    }
}
