package io.usethesource.vallang;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class ValueProvider implements ArgumentsProvider {
    private static final TypeFactory tf = TypeFactory.getInstance();
    private static final IValueFactory referenceFactory = io.usethesource.vallang.impl.reference.ValueFactory.getInstance();
    private static final IValueFactory persistentFactory = io.usethesource.vallang.impl.persistent.ValueFactory.getInstance();
    private static final TypeStore store = new TypeStore();
    private static final Random rnd = new Random();
    private static final RandomValueGenerator referenceGen = new RandomValueGenerator(referenceFactory, rnd, 5, 10, true);
    private static final RandomValueGenerator persistentGen = new RandomValueGenerator(persistentFactory, rnd, 5, 10, true);
    private static final int MAX = 1000;
    
    private static final Map<Class<?>, Supplier<Type>> types = Stream.of(
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IInteger.class, () -> tf.integerType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IReal.class, () -> tf.realType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IRational.class, () -> tf.rationalType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(INumber.class, () -> tf.numberType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IString.class, () -> tf.stringType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(ISourceLocation.class, () -> tf.sourceLocationType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IValue.class, () -> tf.valueType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(INode.class, () -> tf.nodeType()),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IList.class, () -> tf.listType(tf.randomType())),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(ISet.class, () -> tf.setType(tf.randomType())),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(ITuple.class, () -> tf.tupleType(tf.randomType(), tf.randomType())),
            new AbstractMap.SimpleEntry<Class<?>, Supplier<Type>>(IMap.class, () -> tf.mapType(tf.randomType(), tf.randomType()))
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Method method = context.getTestMethod().get();
        
        // we generate as many values as there are IValue parameters,
        // either all parameters are with the reference factory,
        // or they are all with the persistent factory:
        
        return Stream.generate(() -> Arguments.of(
                rnd.nextBoolean() ?
                Arrays.stream(method.getParameterTypes()).map(
                        cl -> cl.isAssignableFrom(IValueFactory.class) 
                                ? referenceFactory
                                : referenceValue(cl)
                        ).toArray()
                :
                    Arrays.stream(method.getParameterTypes()).map(
                            cl -> cl.isAssignableFrom(IValueFactory.class) 
                                    ? persistentFactory
                                    : persistentValue(cl)
                            ).toArray()  
                )).limit(MAX);
    }

    private IValue persistentValue(Class<?> cl) {
        return persistentGen.generate(types.getOrDefault(cl, () -> tf.valueType()).get(), store, Collections.emptyMap());
    }

    private IValue referenceValue(Class<?> cl) {
        return referenceGen.generate(types.getOrDefault(cl, () -> tf.valueType()).get(), store, Collections.emptyMap());
    }
}