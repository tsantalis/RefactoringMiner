package io.usethesource.vallang;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class ValueFactoryProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(io.usethesource.vallang.impl.reference.ValueFactory.getInstance()),
                Arguments.of(io.usethesource.vallang.impl.persistent.ValueFactory.getInstance())
               );
    }
}