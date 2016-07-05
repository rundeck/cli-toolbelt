package example;

import com.simplifyops.toolbelt.*;

import java.io.IOException;

@SubCommand
public class App {

    public static void main(String[] args) throws IOException, CommandRunFailure {
        ToolBelt.with("example", new App()).runMain(args, true);
    }

    @Command(description = "Simple example")
    public void simple() {
        System.out.println("Easily create commandlines with simple annotation");
    }

    @Command(description = "Fancy example")
    public boolean fancy(@Arg("string") String val) {
        System.out.println("Basic commandline parsing " + val);
        return true;
    }
}
