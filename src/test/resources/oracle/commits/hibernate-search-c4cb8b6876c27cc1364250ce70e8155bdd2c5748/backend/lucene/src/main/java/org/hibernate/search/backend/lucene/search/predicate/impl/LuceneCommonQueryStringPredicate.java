/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import static org.hibernate.search.backend.lucene.search.predicate.impl.LuceneCommonMinimumShouldMatchConstraint.minimumShouldMatch;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.CommonQueryStringPredicateBuilder;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

abstract class LuceneCommonQueryStringPredicate extends AbstractLuceneNestablePredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;
	private final List<String> fieldPaths;

	private final Query query;

	protected LuceneCommonQueryStringPredicate(Builder builder) {
		super( builder );
		nestedPathHierarchy = builder.firstFieldState.field().nestedPathHierarchy();
		fieldPaths = new ArrayList<>( builder.fieldStates.keySet() );
		query = builder.buildQuery();
	}

	static void checkFieldsAreAcceptable(String queryName,
			Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates) {
		List<String> badFields = new ArrayList<>();

		for ( LuceneCommonQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
			if ( state.field().type().searchAnalyzerOrNormalizer() == null ) {
				badFields.add(
						String.format( Locale.ROOT, "{'%s':%s}", state.field().absolutePath(),
								state.field().type().valueClass() ) );
			}
		}
		if ( !badFields.isEmpty() ) {
			throw new SearchException( queryName + " queries are not allowed for non-string fields. "
					+ "Fields violating this constraint are: " + badFields );
		}
	}

	static PredicateRequestContext contextForField(LuceneCommonQueryStringPredicateBuilderFieldState state) {
		// Note we want to build a predicate context that won't trigger implicit nesting
		//  when we build inner queries as the nesting part is going to be covered by the main predicate
		List<String> nestedPathHierarchy = state.field().nestedPathHierarchy();
		String expectedNestedPath = nestedPathHierarchy.isEmpty()
				? null
				: nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );

		return PredicateRequestContext.withoutSession().withNestedPath( expectedNestedPath );
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return query;
	}

	@Override
	protected List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	protected List<String> getFieldPathsForErrorMessage() {
		return fieldPaths;
	}

	public abstract static class Builder extends AbstractBuilder implements CommonQueryStringPredicateBuilder {
		private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

		private LuceneCommonQueryStringPredicateBuilderFieldState firstFieldState;
		private final Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStates = new LinkedHashMap<>();
		protected BooleanOperator defaultOperator = BooleanOperator.OR;
		protected String queryString;
		private Analyzer overrideAnalyzer;
		private boolean ignoreAnalyzer = false;
		private NavigableMap<Integer, LuceneCommonMinimumShouldMatchConstraint> minimumShouldMatchConstraints;

		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
			this.analysisDefinitionRegistry = scope.analysisDefinitionRegistry();
		}

		@Override
		public void defaultOperator(BooleanOperator operator) {
			this.defaultOperator = operator;
		}

		@Override
		public FieldState field(String fieldPath) {
			LuceneCommonQueryStringPredicateBuilderFieldState fieldState = fieldStates.get( fieldPath );
			if ( fieldState == null ) {
				fieldState = scope.fieldQueryElement( fieldPath, typeKey() );
				if ( firstFieldState == null ) {
					firstFieldState = fieldState;
				}
				else {
					SearchIndexSchemaElementContextHelper.checkNestedDocumentPathCompatibility( firstFieldState.field(),
							fieldState.field() );
				}
				fieldStates.put( fieldPath, fieldState );
			}
			return fieldState;
		}

		@Override
		public void queryString(String queryString) {
			this.queryString = queryString;
		}

		@Override
		public void analyzer(String analyzerName) {
			this.overrideAnalyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
			if ( overrideAnalyzer == null ) {
				throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexNames( scope.hibernateSearchIndexNames() ) );
			}
		}

		@Override
		public void skipAnalysis() {
			this.ignoreAnalyzer = true;
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new LuceneCommonMinimumShouldMatchConstraint( matchingClausesNumber, null )
			);
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			addMinimumShouldMatchConstraint(
					ignoreConstraintCeiling,
					new LuceneCommonMinimumShouldMatchConstraint( null, matchingClausesPercent )
			);
		}

		private void addMinimumShouldMatchConstraint(int ignoreConstraintCeiling,
				LuceneCommonMinimumShouldMatchConstraint constraint) {
			if ( minimumShouldMatchConstraints == null ) {
				// We'll need to go through the data in ascending order, so use a TreeMap
				minimumShouldMatchConstraints = new TreeMap<>();
			}
			Object previous = minimumShouldMatchConstraints.put( ignoreConstraintCeiling, constraint );
			if ( previous != null ) {
				throw log.minimumShouldMatchConflictingConstraints( ignoreConstraintCeiling );
			}
		}

		protected Query applyMinimumShouldMatch(Query query) {
			if ( minimumShouldMatchConstraints == null ) {
				return query;
			}
			if ( query instanceof BooleanQuery ) {
				BooleanQuery booleanQuery = (BooleanQuery) query;
				int shouldClauses = (int) booleanQuery.clauses().stream().map( BooleanClause::getOccur )
						.filter( BooleanClause.Occur.SHOULD::equals )
						.count();
				int minimumShouldMatch = minimumShouldMatch( minimumShouldMatchConstraints, shouldClauses );

				BooleanQuery.Builder builder = new BooleanQuery.Builder();
				for ( BooleanClause clause : booleanQuery.clauses() ) {
					builder.add( clause );
				}

				query = builder.setMinimumNumberShouldMatch( minimumShouldMatch ).build();
			}
			return query;
		}

		protected Query addMatchAllForBoolMustNotOnly(Query query) {
			if ( query instanceof BooleanQuery ) {
				BooleanQuery booleanQuery = (BooleanQuery) query;
				long notMustNot = booleanQuery.clauses().stream().map( BooleanClause::getOccur )
						.filter( Predicate.not( BooleanClause.Occur.MUST_NOT::equals ) )
						.count();

				if ( notMustNot == 0 && !booleanQuery.clauses().isEmpty() ) {
					// means we only have must not clauses,
					// and we want to add a match all in this case!
					BooleanQuery.Builder builder = new BooleanQuery.Builder();
					for ( BooleanClause clause : booleanQuery.clauses() ) {
						builder.add( clause );
					}
					builder.add( new BooleanClause( new MatchAllDocsQuery(), BooleanClause.Occur.MUST ) );
					query = builder.build();
				}
			}
			return query;
		}

		protected abstract Query buildQuery();

		protected abstract SearchQueryElementTypeKey<LuceneCommonQueryStringPredicateBuilderFieldState> typeKey();

		protected Analyzer buildAnalyzer() {
			if ( ignoreAnalyzer ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}
			if ( overrideAnalyzer != null ) {
				return overrideAnalyzer;
			}
			if ( fieldStates.size() == 1 ) {
				return fieldStates.values().iterator().next().field().type().searchAnalyzerOrNormalizer();
			}

			/*
			 * We need to build a new scoped analyzer to address the case of search queries targeting
			 * multiple indexes, where index A defines "field1" but not "field2",
			 * and index B defines "field2" but not "field1".
			 * In that case, neither the scoped analyzer for index A nor the scoped analyzer for index B would work.
			 *
			 * An alternative exists, but I am not sure it would perform significantly better.
			 * Let us consider that all targeted indexes are compatible for the targeted fields,
			 * i.e. if an index defines a field, it always has the same analyzer as the same field in other indexes.
			 * This compatibility would allow us to simply use a "chaining" analyzer,
			 * which would hold a list of each scoped analyzer for each index,
			 * and, when asked for the analyzer to delegate to,
			 * would pick the first analyzer returned by any of the scoped analyzers in its list.
			 */
			ScopedAnalyzer.Builder builder = new ScopedAnalyzer.Builder();
			for ( LuceneCommonQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
				// Warning: we must use field().absolutePath(), not the key in the map,
				// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
				builder.setAnalyzer( state.field().absolutePath(), state.field().type().searchAnalyzerOrNormalizer() );
			}
			return builder.build();
		}

		protected Map<String, Float> buildWeights() {
			Map<String, Float> weights = new LinkedHashMap<>();
			for ( LuceneCommonQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
				Float boost = state.boost();
				if ( boost == null ) {
					boost = 1f;
				}

				// Warning: we must use field().absolutePath(), not the key in the map,
				// because that key may be a relative path when using SearchPredicateFactory.withRoot(...)
				weights.put( state.field().absolutePath(), boost );
			}
			return weights;
		}

		protected Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStateLookup() {
			Map<String, LuceneCommonQueryStringPredicateBuilderFieldState> fieldStatesRemapped = new HashMap<>();
			for ( LuceneCommonQueryStringPredicateBuilderFieldState state : fieldStates.values() ) {
				// See warning in the buildWeights(). we have to have the same keys in the map,
				//  as query parsers will be working with what we pass in the weights map.
				fieldStatesRemapped.put( state.field().absolutePath(), state );
			}
			return fieldStatesRemapped;
		}
	}
}
