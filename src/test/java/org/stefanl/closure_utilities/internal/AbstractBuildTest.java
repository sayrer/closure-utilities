package org.stefanl.closure_utilities.internal;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.stefanl.closure_utilities.utilities.FsTool;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class AbstractBuildTest<A extends AbstractBuilder<B>, B> {

    protected final A builder;

    protected final Class<B> buildOptionsClass;

    protected B builderOptions;

    protected File outputDirectory;

    protected AbstractBuildTest(@Nonnull Class<A> builderClass,
                                @Nonnull Class<B> buildOptionsClass)
            throws InstantiationException, IllegalAccessException {
        builder = builderClass.newInstance();
        this.buildOptionsClass = buildOptionsClass;
    }


    protected void setUp() throws Exception {
        builderOptions = buildOptionsClass.newInstance();
        builder.setBuildOptions(builderOptions);
        outputDirectory = FsTool.getTempDirectory();
    }

    protected void tearDown() throws Exception {
        outputDirectory = null;
        builderOptions = null;
        builder.reset();
    }

    @Nonnull
    protected File getApplicationDirectory() {
        return new File("src/test/resources/app");
    }

    @Nonnull
    protected File getApplicationDirectory(String path) {
        return new File(getApplicationDirectory(), path);
    }

    @Nonnull
    protected Collection<File> getGssSourceDirectories() {
        return Sets.newHashSet(getApplicationDirectory("src/gss/"));
    }

    @Nonnull
    protected List<String> getGssEntryPoints() {
        return Lists.newArrayList(Lists.newArrayList("company-import"));
    }

    @Nonnull
    protected Collection<File> getSoySourceDirectories() {
        return Sets.newHashSet(getApplicationDirectory("src/soy"));
    }


}