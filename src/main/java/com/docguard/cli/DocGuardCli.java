package com.docguard.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "docguard",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        subcommands = {CheckCommand.class, SyncCommand.class},
        description = "Static documentation drift checker and sync utility for Spring REST APIs.")
public class DocGuardCli {
    public static void main(String[] args) {
        try {
            int exitCode = new CommandLine(new DocGuardCli()).execute(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
