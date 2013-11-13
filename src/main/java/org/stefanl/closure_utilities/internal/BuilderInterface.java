package org.stefanl.closure_utilities.internal;


import javax.annotation.Nonnull;

public interface BuilderInterface<A, B> {

    public void checkOptions(@Nonnull A options) throws BuildOptionsException;

    @Nonnull
    public B build(@Nonnull A options) throws BuildException;
}

