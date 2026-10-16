package com.docguard.cli;

import com.docguard.config.Config;
import com.docguard.config.ConfigValidator;
import com.docguard.docs.GeneratedSectionManager;
import com.docguard.docs.MarkdownGenerator;
import com.docguard.io.MarkdownFileService;
import com.docguard.service.DocGuardService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "sync",
        description = "Synchronize the generated documentation section while preserving manual content.",
        mixinStandardHelpOptions = true)
public class SyncCommand implements Callable<Integer> {
    @Option(names = {"-s", "--source", "--src"}, required = true,
           description = "Source directory to scan")
    private Path sourceDirectory;

    @Option(names = {"-d", "--docs", "--doc"}, required = true,
           description = "Path to the Markdown documentation file")
    private Path documentationFile;

    @Override
    public Integer call() throws Exception {
        Config config = ConfigValidator.validate(sourceDirectory, documentationFile, Config.Mode.SYNC);

        String existingContent = MarkdownFileService.readUtf8(config.getDocumentationFile());
        GeneratedSectionManager.validateStructure(existingContent);

        String replacement = MarkdownGenerator.generate(new DocGuardService().resolveEndpoints(config.getSourceDirectory()));
        String updatedContent = GeneratedSectionManager.replaceGeneratedSection(existingContent, replacement);
        MarkdownFileService.writeUtf8(config.getDocumentationFile(), updatedContent);

        System.out.println("Documentation synced successfully: " + config.getDocumentationFile());
        return 0;
    }
}
