package org.stefanl.closure_utilities;


import org.stefanl.closure_utilities.internal.DependencyCalculator;
import org.stefanl.closure_utilities.stylesheets.GssBuildOptions;
import org.stefanl.closure_utilities.stylesheets.GssBuilder;
import org.stefanl.closure_utilities.stylesheets.GssDependencyParser;
import org.stefanl.closure_utilities.stylesheets.GssSourceFile;
import org.stefanl.closure_utilities.utilities.FsTool;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

public class App {

    static final GssDependencyParser gssDependencyParser =
            new GssDependencyParser();

    static final Function<File, GssSourceFile> FILE_TO_GSS_SOURCE =
            new Function<File, GssSourceFile>() {
                @Nullable
                @Override
                public GssSourceFile apply(@Nullable File file) {
                    final GssSourceFile gssSourceFile =
                            new GssSourceFile(file);
                    try (final Reader reader = new FileReader(file)) {
                        gssDependencyParser.parse(gssSourceFile, reader);
                    } catch (IOException ioException) {

                    }
                    return gssSourceFile;
                }
            };

    static final GssBuilder gssBuilder = new GssBuilder();

    public static void main(String[] args) throws Exception {
        final File outputDirectory = new File("build");
        final GssBuildOptions gssBuildOptions = new GssBuildOptions();
        final File gssSourceDirectory = new File
                ("src/test/resources/app/src/gss");
        final Collection<File> gssFiles = FsTool.find(gssSourceDirectory,
                "gss");
        final Collection<GssSourceFile> gssSourceFiles =
                Collections2.transform(gssFiles, FILE_TO_GSS_SOURCE);
        final DependencyCalculator<GssSourceFile> gssDependencyCalculator =
                new DependencyCalculator<GssSourceFile>(gssSourceFiles);
        final List<GssSourceFile> calculatedGssSourceFiles =
                gssDependencyCalculator.getDependencyList("company-import");
        gssBuildOptions.setAssetsDirectory(new File(outputDirectory, "assets"));
        gssBuildOptions.setOutputFile(new File(outputDirectory, "style.css"));
        final Function<GssSourceFile, File> X = new Function<GssSourceFile,
                File>() {
            @Nullable
            @Override
            public File apply(@Nullable GssSourceFile gssSourceFile) {
                return new File(gssSourceFile.getSourceLocation());
            }
        };
        gssBuildOptions.setSourceFiles(Lists.transform
                (calculatedGssSourceFiles, X));
        gssBuilder.setBuildOptions(gssBuildOptions);
        gssBuilder.build();
    }
}