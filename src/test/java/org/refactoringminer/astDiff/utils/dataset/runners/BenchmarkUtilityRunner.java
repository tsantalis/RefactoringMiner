package org.refactoringminer.astDiff.utils.dataset.runners;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import java.util.List;

public class BenchmarkUtilityRunner {
    public static void main(String[] args) {
        // Create instances of command classes
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