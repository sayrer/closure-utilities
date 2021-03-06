package slieb.closure.build;

import com.google.common.collect.Lists;
import slieb.closure.build.gss.DefaultGssBuilder;
import slieb.closure.build.gss.GssOptions;
import slieb.closure.build.gss.GssResult;
import slieb.closure.build.html.HtmlBuilder;
import slieb.closure.build.html.HtmlOptions;
import slieb.closure.build.html.HtmlResult;
import slieb.closure.build.internal.AbstractBuilder;
import slieb.closure.build.internal.BuildException;
import slieb.closure.build.internal.BuildOptionsException;
import slieb.closure.build.javascript.JsBuilder;
import slieb.closure.build.javascript.JsOptions;
import slieb.closure.build.javascript.JsResult;
import slieb.closure.build.soy.SoyOptions;
import slieb.closure.build.soy.SoyResult;
import slieb.closure.build.soy.DefaultSoyBuilder;
import slieb.closure.internal.GlobalsConverter;
import slieb.closure.render.DefaultHtmlRenderer;
import slieb.closure.render.SoyHtmlRenderer;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class ClosureBuilder
        extends AbstractBuilder<ClosureOptions, ClosureResult> {

    @Nonnull
    @Override
    protected ClosureResult buildInternal(
            @Nonnull final ClosureOptions options) throws Exception {
        final InternalData data = new InternalData();
        buildCommands(Lists.newArrayList(BuildCommand.ALL), options, data);
        return data.toResult();
    }

    @Nonnull
    public ClosureResult buildCommands(@Nonnull final ClosureOptions options,
                                       @Nonnull final BuildCommand... commands)
            throws BuildException {
        final InternalData data = new InternalData();
        buildCommands(Lists.newArrayList(commands), options, data);
        return data.toResult();
    }

    protected void buildCommands(
            @Nonnull final Collection<BuildCommand> commands,
            @Nonnull final ClosureOptions options,
            @Nonnull final InternalData data)
            throws BuildException {

        final boolean doAll = commands.contains(BuildCommand.ALL);

        if (doAll || commands.contains(BuildCommand.STYLESHEETS)) {
            buildGss(options, data);
        }

        if (doAll || commands.contains(BuildCommand.TEMPLATES)) {
            buildSoy(options, data);
        }

        if (doAll || commands.contains(BuildCommand.JAVASCRIPT)) {
            buildJs(options, data);
        }

        if (doAll || commands.contains(BuildCommand.HTML)) {
            buildHtml(options, data);
        }
    }


    private static class InternalData {
        private File generatedStylesheet;

        private File generatedRenameMap;

        private File soyOutputDirectory;

        private File htmlOutputFile;

        private File jsBaseFile;
        private File jsOutputDepsFile;
        private File jsOutputDefinesFile;
        private File jsOutputFile;

        private List<File> calculatedScriptFiles;

        @Nonnull
        private ClosureResult toResult() {
            return new ClosureResult(generatedStylesheet, generatedRenameMap,
                    soyOutputDirectory, htmlOutputFile, jsBaseFile,
                    jsOutputDepsFile, jsOutputDefinesFile, jsOutputFile);
        }
    }

    public enum BuildCommand {

        ALL, STYLESHEETS, TEMPLATES, JAVASCRIPT, HTML;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        @Nonnull
        public static BuildCommand fromText(String text) {
            return valueOf(text.trim().toUpperCase());
        }
    }


    private final DefaultGssBuilder gssBuilder = new DefaultGssBuilder();

    private final DefaultSoyBuilder soyBuilder = new DefaultSoyBuilder();

    private final JsBuilder jsBuilder = new JsBuilder();

    private final HtmlBuilder htmlBuilder = new HtmlBuilder();

    public static final String DEFAULT_STYLESHEET_FILENAME = "style.css";

    @Nonnull
    public File getGssOutputFile(@Nonnull final ClosureOptions options) {
        final File outputStylesheetFile = options.getOutputStylesheetFile();
        if (outputStylesheetFile != null) {
            return outputStylesheetFile;
        } else {
            return new File(options.getOutputDirectory(),
                    DEFAULT_STYLESHEET_FILENAME);
        }
    }


    @Nonnull
    public GssOptions getGssBuildOptions(
            @Nonnull final ClosureOptions options) {
        final GssOptions gssBuildOptions = new GssOptions();
        gssBuildOptions.setShouldCalculateDependencies(true);
        gssBuildOptions.setAssetsDirectory(options.getAssetsDirectory());
        gssBuildOptions.setEntryPoints(options.getGssEntryPoints());
        gssBuildOptions.setSourceDirectories(options.getGssSourceDirectories());
        gssBuildOptions.setRenameMap(options.getCssClassRenameMap());
        gssBuildOptions.setShouldGenerateForDebug(options.getShouldDebug());
        gssBuildOptions.setShouldGenerateForProduction(options
                .getShouldCompile());
        gssBuildOptions.setOutputFile(getGssOutputFile(options));
        gssBuildOptions.setAssetsUri(options.getAssetsUri());
        return gssBuildOptions;
    }

    protected void buildGss(@Nonnull final ClosureOptions options,
                            @Nonnull final InternalData internalData)
            throws BuildException {
        final GssOptions gssOptions = getGssBuildOptions(options);
        final GssResult gssResult = gssBuilder.build(gssOptions);
        internalData.generatedStylesheet = gssResult.getGeneratedStylesheet();
        internalData.generatedRenameMap = gssResult.getGeneratedRenameMap();
    }


    public ClosureResult buildGssOnly(@Nonnull final ClosureOptions options)
            throws BuildException {
        final InternalData internalData = new InternalData();
        buildGss(options, internalData);
        return internalData.toResult();
    }

    public final static String COMPILED_TEMPLATES_DIRECTORY_NAME =
            "compiled-templates";

    @Nonnull
    public File getSoyOutputDirectory(
            @Nonnull final ClosureOptions options) {
        final File soyOutputDirectory = options.getSoyOutputDirectory();
        if (soyOutputDirectory != null) {
            return soyOutputDirectory;
        } else {
            return getOutputFile(options, COMPILED_TEMPLATES_DIRECTORY_NAME);
        }
    }

    @Nonnull
    public File getOutputDirectory(@Nonnull final ClosureOptions
                                           options) {
        final File outputDirectory = options.getOutputDirectory();
        if (outputDirectory != null) {
            return outputDirectory;
        } else {
            throw new NullPointerException(UNSPECIFIED_OUTPUT_DIRECTORY);
        }
    }

    @Nonnull
    private File getOutputFile(
            @Nonnull final ClosureOptions options,
            @Nonnull final String fileName) {
        return new File(getOutputDirectory(options), fileName);
    }

    @Nonnull
    private File getJsOutputFile(
            @Nonnull final ClosureOptions options,
            @Nullable final File value,
            @Nonnull final String defaultFileName) {
        if (value != null) {
            return value;
        } else {
            return getOutputFile(options, defaultFileName);
        }
    }

    @Nonnull
    private SoyOptions getSoyBuildOptions(@Nonnull final ClosureOptions
                                                  options) {
        final SoyOptions soyOptions = new SoyOptions();
        soyOptions.setSourceDirectories(options.getSoySourceDirectories());
        soyOptions.setOutputDirectory(getSoyOutputDirectory(options));
        return soyOptions;
    }

    private void buildSoy(@Nonnull final ClosureOptions options,
                          @Nonnull final InternalData internalData)
            throws BuildException {
        final SoyOptions soyOptions = getSoyBuildOptions(options);
        final SoyResult soyResult = soyBuilder.build(soyOptions);
        internalData.soyOutputDirectory = soyResult.getOutputDirectory();
    }

    public ClosureResult buildSoyOnly(@Nonnull final ClosureOptions options)
            throws BuildException {
        final InternalData internalData = new InternalData();
        buildSoy(options, internalData);
        return internalData.toResult();
    }

    private static final GlobalsConverter globalsConverter =
            new GlobalsConverter();

    public static void getJsGlobalsFromConfigurations(
            @Nonnull final Map<String, Object> globals,
            @Nonnull final List<Configuration> configurations) {
        CompositeConfiguration compositeConfiguration =
                new CompositeConfiguration();
        for (Configuration configuration : configurations) {
            compositeConfiguration.copy(configuration);
        }
        Configuration config =
                compositeConfiguration.interpolatedConfiguration();
        Iterator<String> keysInterator = config.getKeys();
        while (keysInterator.hasNext()) {
            String key = keysInterator.next();
            String stringValue = config.getString(key);
            Object value = globalsConverter.convertValue(stringValue);
            globals.put(key, value);
        }
    }

    public Map<String, Object> getJsGlobals(
            @Nonnull final ClosureOptions options) {
        final HashMap<String, Object> globals = new HashMap<>();
        final List<Configuration> configurations = options.getConfigurations();
        if (configurations != null) {
            getJsGlobalsFromConfigurations(globals, configurations);
        }
        return globals;
    }

    @Nonnull
    private JsOptions getJsBuildOptions(
            @Nonnull final ClosureOptions options) {
        final JsOptions jsOptions = new JsOptions(options);
        jsOptions.setOutputFile(
                getJsOutputFile(options,
                        options.getJavascriptOutputFile(),
                        "script.min.js"));
        jsOptions.setOutputDefinesFile(
                getJsOutputFile(options,
                        options.getJavascriptDefinesOutputFile(),
                        "defines.js"));
        jsOptions.setOutputDependencyFile(
                getJsOutputFile(options,
                        options.getJavascriptDependencyOutputFile(),
                        "deps.js"));
        jsOptions.setEntryPoints(options.getJavascriptEntryPoints());
        jsOptions.setEntryFiles(options.getJavascriptEntryFiles());
        jsOptions.setSourceDirectories(
                options.getJavascriptSourceDirectories(false));
        jsOptions.setMessageBundle(options.getMessageBundle());
        jsOptions.setGlobals(getJsGlobals(options));
        return jsOptions;
    }

    private void buildJs(@Nonnull final ClosureOptions options,
                         @Nonnull final InternalData internalData)
            throws BuildException {
        final JsOptions jsOptions = getJsBuildOptions(options);
        JsResult jsResult = jsBuilder.build(jsOptions);
        internalData.calculatedScriptFiles = jsResult.getScriptFiles();
        internalData.jsBaseFile = jsResult.getBaseFile();
        internalData.jsOutputDepsFile = jsResult.getOutputDepsFile();
        internalData.jsOutputDefinesFile = jsResult.getOutputDefinesFile();
        internalData.jsOutputFile = jsResult.getOutputFile();
    }

    public ClosureResult buildJsOnly(@Nonnull final ClosureOptions options)
            throws BuildException {
        final InternalData internalData = new InternalData();
        buildJs(options, internalData);
        return internalData.toResult();
    }

    private final static String DEFAULT_HTML_PAGE_NAME =
            "index.html";

    @Nonnull
    private File getHtmlOutputFile(@Nonnull final ClosureOptions options) {
        final File htmlFile = options.getOutputHtmlFile();
        if (htmlFile != null) {
            return htmlFile;
        } else {
            return getOutputFile(options, DEFAULT_HTML_PAGE_NAME);
        }
    }

    @Nonnull
    private List<File> getStylesheetsForHtmlBuild(
            @Nonnull final ClosureOptions options,
            @Nonnull final InternalData internalData) {
        final List<File> stylesheets = new ArrayList<>();
        final List<File> externalStylesheets =
                options.getExternalStylesheets();
        if (externalStylesheets != null && !externalStylesheets.isEmpty()) {
            stylesheets.addAll(externalStylesheets);
        }

        if (internalData.generatedStylesheet != null) {
            stylesheets.add(internalData.generatedStylesheet);
        }
        return stylesheets;
    }

    @Nonnull
    private List<File> getJavascriptFilesForHtmlBuild(
            @Nonnull final ClosureOptions options,
            @Nonnull final InternalData internalData) {

        final List<File> javascriptFiles = new ArrayList<>();
        final List<File> externalScripts = options.getExternalScriptFiles();
        if (externalScripts != null && !externalScripts.isEmpty()) {
            javascriptFiles.addAll(externalScripts);
        }

        if (options.getShouldCompile()) {
            final File outFile = internalData.jsOutputFile;
            if (outFile != null) {
                javascriptFiles.add(outFile);
            }
        } else {
            final List<File> calculatedSourceFiles =
                    internalData.calculatedScriptFiles;
            // jsBuilder.getScriptsFilesToCompile();
            if (calculatedSourceFiles != null) {
                javascriptFiles.addAll(calculatedSourceFiles);
            }
        }
        return javascriptFiles;
    }

    @Nonnull
    private HtmlOptions getHtmlBuildOptions(
            @Nonnull final ClosureOptions options,
            @Nonnull final InternalData internalData) {
        final HtmlOptions htmlOptions = new HtmlOptions(options);

        htmlOptions.setOutputFile(getHtmlOutputFile(options));
        htmlOptions.setStylesheetFiles(getStylesheetsForHtmlBuild(options,
                internalData));
        htmlOptions.setJavascriptFiles(getJavascriptFilesForHtmlBuild
                (options, internalData));
        String templateName = options.getHtmlTemplate();
        Collection<File> sourceDirectories = options.getSoySourceDirectories();
        if (templateName != null &&
                sourceDirectories != null &&
                !sourceDirectories.isEmpty()) {
            SoyHtmlRenderer soyHtmlRenderer =
                    new SoyHtmlRenderer(sourceDirectories, templateName);
            htmlOptions.setHtmlRenderer(soyHtmlRenderer);
        } else {
            htmlOptions.setHtmlRenderer(new DefaultHtmlRenderer());
        }

        htmlOptions.setContent(options.getHtmlContent());
        htmlOptions.setLocationMap(null);
        htmlOptions.setShouldBuildInline(false);
        return htmlOptions;
    }


    private void buildHtml(@Nonnull final ClosureOptions options,
                           @Nonnull final InternalData internalData)
            throws BuildException {
        final HtmlOptions htmlOptions = getHtmlBuildOptions(options,
                internalData);
        final HtmlResult result = htmlBuilder.build(htmlOptions);
        internalData.htmlOutputFile = result.getGeneratedHtmlFile();
    }

    @Nonnull
    public ClosureResult buildHtmlOnly(@Nonnull final ClosureOptions options)
            throws BuildException {
        final InternalData internalData = new InternalData();
        buildHtml(options, internalData);
        return internalData.toResult();
    }


    private static final String UNSPECIFIED_OUTPUT_DIRECTORY =
            "Closure output directory has not been specified.";

    @Override
    public void checkOptions(@Nonnull ClosureOptions options) throws
            BuildOptionsException {
        final File outputDirectory = options.getOutputDirectory();
        if (outputDirectory == null) {
            throw new BuildOptionsException(UNSPECIFIED_OUTPUT_DIRECTORY);
        }
    }


}
