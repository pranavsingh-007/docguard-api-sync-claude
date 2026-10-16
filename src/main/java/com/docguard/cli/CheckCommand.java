package com.docguard.cli;

import com.docguard.config.Config;
import com.docguard.config.ConfigValidator;
import com.docguard.docs.DriftComparator;
import com.docguard.docs.GeneratedSectionManager;
import com.docguard.docs.MarkdownGenerator;
import com.docguard.io.MarkdownFileService;
import com.docguard.model.Endpoint;
import com.docguard.service.DocGuardService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "check",
        description = "Validate that the generated documentation section matches the current source state.",
        mixinStandardHelpOptions = true)
public class CheckCommand implements Callable<Integer> {
    @Option(names = {"-s", "--source", "--src"}, required = true,
           description = "Source directory to scan")
    private Path sourceDirectory;

    @Option(names = {"-d", "--docs", "--doc"}, required = true,
           description = "Path to the Markdown documentation file")
    private Path documentationFile;

    @Override
    public Integer call() throws Exception {
        Config config = ConfigValidator.validate(sourceDirectory, documentationFile, Config.Mode.CHECK);

        String existingContent = MarkdownFileService.readUtf8(config.getDocumentationFile());
        GeneratedSectionManager.validateStructure(existingContent);

        List<Endpoint> expectedEndpoints = new DocGuardService().resolveEndpoints(config.getSourceDirectory());
        String existingGeneratedSection = GeneratedSectionManager.parse(existingContent).getGenerated();
        List<Endpoint> existingEndpoints = MarkdownGenerator.parseGeneratedContent(existingGeneratedSection);
        DriftComparator.DriftReport driftReport = DriftComparator.compare(expectedEndpoints, existingEndpoints);

        if (expectedEndpoints.isEmpty() && !driftReport.hasDrift()) {
           System.out.println("No endpoints discovered.");
           return 0;
        }

        if (!driftReport.hasDrift()) {
           System.out.println("Documentation is synchronized: " + config.getDocumentationFile());
           return 0;
        }

        if (expectedEndpoints.isEmpty()) {
           System.out.println("No endpoints discovered.");
        }
        System.err.println(DriftComparator.formatReport(driftReport));
        return 1;
    }
}
