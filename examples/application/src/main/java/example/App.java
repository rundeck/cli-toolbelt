package example;

import com.simplifyops.toolbelt.*;

import java.io.IOException;

/**
 * Created by greg on 7/5/16.
 */
@SubCommand
public class App {

    public static void main(String[] args) throws IOException, CommandRunFailure {
        ToolBelt.with(
                "example",
                new App(),
                new Sub()
        ).runMain(args, true);
    }

    @Command(description = "Start here, run: example begin")
    public void begin() {
        System.out.println("Easily create commandlines with simple annotation");
        System.out.println();
        System.out.println("next try: example colorize");
    }

    @Command
    public void colorize(CommandOutput output) {
        output.output("Control output, and ansi colorization");
        output.warning("If you enable it");
        output.output(ANSIColorOutput.colorize(
                "You can even ",
                ANSIColorOutput.Color.BLUE,
                "get fancy",
                " if you want to."
        ));
        output.output("");
        output.output("Try setting TERM=blah in your environment, and run: `example colorize` again");
        output.output("");
        output.output(ANSIColorOutput.colorize(
                "next try: ",
                ANSIColorOutput.Color.RED,
                "example fail"
        ));

    }

    @Command(description = "Demonstrate failure")
    public boolean fail(CommandOutput output) {
        output.error("Return false, or throw an exception, to fail");
        output.warning("The exit code should be: 2");
        output.output("next try: example sub");
        return false;
    }


}
