/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.service;

import org.jetbrains.annotations.NotNull;

public class IntContributor implements Contributor<Integer, Void, Context<Void>> {

    public static final int VALUE = 6;

    @NotNull
    @Override
    public Class<Void> getConfigType() {
        return Void.class;
    }

    @NotNull
    @Override
    public Integer createInstance(Context<Void> context) {
        return VALUE;
    }
}
