package org.refactoringminer.astDiff.utils.dataset.runners;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.util.List;

public class BenchmarkUtilityRunner {
    public static void main(String[] args) {
//        System.setProperty("rm.jdt.comments", "false");
        // Create instances of command classes
        String url;
        url = "https://github.com/kuujo/copycat/commit/19a49f8f36b2f6d82534dc13504d672e41a3a8d1";
        url = "https://github.com/mockito/mockito/commit/2d036ecf1d7170b4ec7346579a1ef8904109530a";
        url = "https://github.com/JetBrains/MPS/commit/ce4b0e22659c16ae83d421f9621fd3e922750764";






        //insert url to args
        String[] temp = new String[args.length + 1];
        System.arraycopy(args, 0, temp, 0, args.length);
        temp[temp.length - 1] = url;

        args = temp;
        AddCommand addCommand = new AddCommand();
        RemoveCommand removeCommand = new RemoveCommand();

        // Initialize JCommander and register command classes
        JCommander jCommander = JCommander.newBuilder()
                .addCommand("add", addCommand)
                .addCommand("remove", removeCommand)
                .build();

        try {
            // Parse the command-line arguments
            jCommander.parse(args);
            if (jCommander.getParsedCommand() == null) {
                // If no specific command is provided or the base command is executed with --help, display usage for all commands
                jCommander.usage();
            } else {
                List<Object> objects = jCommander.getCommands().get(jCommander.getParsedCommand()).getObjects();
                if (objects.size() != 1) {
                    throw new IllegalStateException("Expected exactly one object");
                }
                // Execute the run method of the parsed command
                ((BaseCommand) objects.get(0)).execute();
            }
        } catch (ParameterException e) {
            // Print error message and usage information
            System.err.println(e.getMessage());
            jCommander.usage();
        }
    }
}