package gr.uom.java.xmi.TypeFactMiner;

import com.t2r.common.models.ast.TypeGraphOuterClass.TypeGraph;
import com.t2r.common.models.refactorings.JarInfoOuterClass.JarInfo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static com.t2r.common.utilities.PrettyPrinter.looselyEqual;
import static com.t2r.common.utilities.PrettyPrinter.pretty;
import static gr.uom.java.xmi.TypeFactMiner.TypFct.getExternalComposition;

public class CompositionUtil {

    public static boolean getCompositionRelation(TypeGraph tg1, TypeGraph tg2, GlobalContext gc, Set<JarInfo> jars , Path pathToJars){
        return getCompositionBiRelation(tg1, tg2, gc, jars, pathToJars) || getCompositionBiRelation(tg2, tg1, gc, jars, pathToJars);
    }

    private static boolean getCompositionBiRelation(TypeGraph tg1, TypeGraph tg2, GlobalContext gc, Set<JarInfo> jars , Path pathToJars){
        return Optional.ofNullable(Optional.ofNullable(gc.getInternalCompositionMap().get(pretty(tg1))).stream()
                .flatMap(x -> x.stream())
                .anyMatch(c -> looselyEqual(c.getType(),tg2)))
                .or(() -> Optional.ofNullable(gc.getJdkComposition().get(pretty(tg1)))
                    .map(x -> x.stream().anyMatch(c -> c.endsWith(pretty(tg2)))))
                .orElseGet(() -> jars.stream().map(x -> getExternalComposition(pathToJars, x))
                        .flatMap(x -> x.entrySet().stream()).filter(x-> x.getKey().equals(pretty(tg1)))
                        .anyMatch(x -> x.getValue().stream().anyMatch(c -> c.endsWith(pretty(tg2)))));
    }
}
