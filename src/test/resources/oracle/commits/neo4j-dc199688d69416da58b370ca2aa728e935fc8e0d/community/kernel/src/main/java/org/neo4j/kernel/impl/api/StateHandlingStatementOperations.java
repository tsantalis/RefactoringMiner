/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.neo4j.collection.primitive.PrimitiveIntCollection;
import org.neo4j.collection.primitive.PrimitiveIntCollections;
import org.neo4j.collection.primitive.PrimitiveIntIterator;
import org.neo4j.collection.primitive.PrimitiveIntStack;
import org.neo4j.collection.primitive.PrimitiveLongIterator;
import org.neo4j.function.Predicate;
import org.neo4j.graphdb.Direction;
import org.neo4j.kernel.api.EntityType;
import org.neo4j.kernel.api.LegacyIndex;
import org.neo4j.kernel.api.LegacyIndexHits;
import org.neo4j.kernel.api.Statement;
import org.neo4j.kernel.api.constraints.MandatoryNodePropertyConstraint;
import org.neo4j.kernel.api.constraints.MandatoryRelationshipPropertyConstraint;
import org.neo4j.kernel.api.constraints.NodePropertyConstraint;
import org.neo4j.kernel.api.constraints.PropertyConstraint;
import org.neo4j.kernel.api.constraints.RelationshipPropertyConstraint;
import org.neo4j.kernel.api.constraints.UniquenessConstraint;
import org.neo4j.kernel.api.cursor.EntityCursor;
import org.neo4j.kernel.api.cursor.LabelCursor;
import org.neo4j.kernel.api.cursor.NodeCursor;
import org.neo4j.kernel.api.cursor.PropertyCursor;
import org.neo4j.kernel.api.cursor.RelationshipCursor;
import org.neo4j.kernel.api.exceptions.EntityNotFoundException;
import org.neo4j.kernel.api.exceptions.LabelNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.PropertyKeyIdNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.RelationshipTypeIdNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.legacyindex.LegacyIndexNotFoundKernelException;
import org.neo4j.kernel.api.exceptions.schema.AlreadyConstrainedException;
import org.neo4j.kernel.api.exceptions.schema.ConstraintVerificationFailedKernelException;
import org.neo4j.kernel.api.exceptions.schema.CreateConstraintFailureException;
import org.neo4j.kernel.api.exceptions.schema.DropConstraintFailureException;
import org.neo4j.kernel.api.exceptions.schema.DropIndexFailureException;
import org.neo4j.kernel.api.exceptions.schema.IllegalTokenNameException;
import org.neo4j.kernel.api.exceptions.schema.IndexBrokenKernelException;
import org.neo4j.kernel.api.exceptions.schema.SchemaRuleNotFoundException;
import org.neo4j.kernel.api.exceptions.schema.TooManyLabelsException;
import org.neo4j.kernel.api.index.IndexDescriptor;
import org.neo4j.kernel.api.index.InternalIndexState;
import org.neo4j.kernel.api.properties.DefinedProperty;
import org.neo4j.kernel.api.properties.Property;
import org.neo4j.kernel.api.properties.PropertyKeyIdIterator;
import org.neo4j.kernel.api.txstate.ReadableTxState;
import org.neo4j.kernel.api.txstate.TransactionState;
import org.neo4j.kernel.api.txstate.TxStateHolder;
import org.neo4j.kernel.impl.api.operations.CountsOperations;
import org.neo4j.kernel.impl.api.operations.EntityOperations;
import org.neo4j.kernel.impl.api.operations.KeyReadOperations;
import org.neo4j.kernel.impl.api.operations.KeyWriteOperations;
import org.neo4j.kernel.impl.api.operations.LegacyIndexReadOperations;
import org.neo4j.kernel.impl.api.operations.LegacyIndexWriteOperations;
import org.neo4j.kernel.impl.api.operations.SchemaReadOperations;
import org.neo4j.kernel.impl.api.operations.SchemaWriteOperations;
import org.neo4j.kernel.impl.api.state.ConstraintIndexCreator;
import org.neo4j.kernel.impl.api.store.CursorRelationshipIterator;
import org.neo4j.kernel.impl.api.store.RelationshipIterator;
import org.neo4j.kernel.impl.api.store.StoreReadLayer;
import org.neo4j.kernel.impl.api.store.StoreStatement;
import org.neo4j.kernel.impl.core.Token;
import org.neo4j.kernel.impl.index.IndexEntityType;
import org.neo4j.kernel.impl.index.LegacyIndexStore;
import org.neo4j.kernel.impl.store.SchemaStorage;
import org.neo4j.kernel.impl.util.Cursors;
import org.neo4j.kernel.impl.util.PrimitiveLongResourceIterator;
import org.neo4j.kernel.impl.util.diffsets.ReadableDiffSets;

import static org.neo4j.collection.primitive.PrimitiveLongCollections.single;
import static org.neo4j.helpers.collection.Iterables.filter;
import static org.neo4j.helpers.collection.IteratorUtil.iterator;
import static org.neo4j.helpers.collection.IteratorUtil.resourceIterator;
import static org.neo4j.helpers.collection.IteratorUtil.singleOrNull;
import static org.neo4j.kernel.api.StatementConstants.NO_SUCH_NODE;

public class StateHandlingStatementOperations implements
        KeyReadOperations,
        KeyWriteOperations,
        EntityOperations,
        SchemaReadOperations,
        SchemaWriteOperations,
        CountsOperations,
        LegacyIndexReadOperations,
        LegacyIndexWriteOperations
{
    private static final int KEY_LIST_INITIAL_SIZE = 16;

    private final StoreReadLayer storeLayer;
    private final LegacyPropertyTrackers legacyPropertyTrackers;
    private final ConstraintIndexCreator constraintIndexCreator;
    private final LegacyIndexStore legacyIndexStore;

    public StateHandlingStatementOperations(
            StoreReadLayer storeLayer, LegacyPropertyTrackers propertyTrackers,
            ConstraintIndexCreator constraintIndexCreator,
            LegacyIndexStore legacyIndexStore )
    {
        this.storeLayer = storeLayer;
        this.legacyPropertyTrackers = propertyTrackers;
        this.constraintIndexCreator = constraintIndexCreator;
        this.legacyIndexStore = legacyIndexStore;
    }

    // <Cursors>
    @Override
    public NodeCursor nodeCursor( KernelStatement statement, long nodeId )
    {
        NodeCursor cursor = statement.getStoreStatement().acquireSingleNodeCursor( nodeId );
        if ( statement.hasTxStateWithChanges() )
        {
            return statement.txState().augmentSingleNodeCursor( cursor );
        }
        return cursor;
    }

    public NodeCursor nodeCursor( TxStateHolder txStateHolder, StoreStatement statement, long nodeId )
    {
        NodeCursor cursor = statement.acquireSingleNodeCursor( nodeId );
        if ( txStateHolder.hasTxStateWithChanges() )
        {
            return txStateHolder.txState().augmentSingleNodeCursor( cursor );
        }
        return cursor;
    }

    @Override
    public RelationshipCursor relationshipCursor( KernelStatement statement, long relationshipId )
    {
        RelationshipCursor cursor = statement.getStoreStatement().acquireSingleRelationshipCursor( relationshipId );
        if ( statement.hasTxStateWithChanges() )
        {
            return statement.txState().augmentSingleRelationshipCursor( cursor );
        }
        return cursor;
    }

    public RelationshipCursor relationshipCursor( TxStateHolder txStateHolder, StoreStatement statement,
            long relationshipId )
    {
        RelationshipCursor cursor = statement.acquireSingleRelationshipCursor( relationshipId );
        if ( txStateHolder.hasTxStateWithChanges() )
        {
            return txStateHolder.txState().augmentSingleRelationshipCursor( cursor );
        }
        else
        {
            return cursor;
        }
    }

    @Override
    public NodeCursor nodeCursorGetAll( KernelStatement statement )
    {
        NodeCursor cursor = storeLayer.nodesGetAllCursor( statement.getStoreStatement() );
        if ( statement.hasTxStateWithChanges() )
        {
            return statement.txState().augmentNodesGetAllCursor( cursor );
        }
        return cursor;
    }

    @Override
    public RelationshipCursor relationshipCursorGetAll( KernelStatement statement )
    {
        RelationshipCursor cursor = storeLayer.relationshipsGetAllCursor( statement.getStoreStatement() );
        if ( statement.hasTxStateWithChanges() )
        {
            return statement.txState().augmentRelationshipsGetAllCursor( cursor );
        }
        return cursor;
    }

    @Override
    public NodeCursor nodeCursorGetForLabel( KernelStatement statement, int labelId )
    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor(
                storeLayer.nodesGetForLabel( statement, labelId ) );
    }

    @Override
    public NodeCursor nodeCursorGetFromIndexSeek( KernelStatement statement, IndexDescriptor index, Object value )
            throws IndexNotFoundKernelException
    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor( storeLayer.nodesGetFromIndexSeek( statement,
                index, value ) );
    }

    @Override
    public NodeCursor nodeCursorGetFromIndexScan( KernelStatement statement, IndexDescriptor index )
            throws IndexNotFoundKernelException
    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor(
                storeLayer.nodesGetFromIndexScan( statement, index ) );
    }

    @Override
    public NodeCursor nodeCursorGetFromIndexRangeSeekByNumber( KernelStatement statement,
                                                               IndexDescriptor index,
                                                               Number lower, boolean includeLower,
                                                               Number upper, boolean includeUpper )
            throws IndexNotFoundKernelException

    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor(
                storeLayer.nodesGetFromIndexRangeSeekByNumber( statement, index, lower, includeLower, upper,
                        includeUpper ) );
    }

    @Override
    public NodeCursor nodeCursorGetFromIndexRangeSeekByPrefix( KernelStatement statement, IndexDescriptor index,
                                                               String prefix )
            throws IndexNotFoundKernelException
    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor(
                storeLayer.nodesGetFromIndexRangeSeekByPrefix( statement, index, prefix ) );
    }

    @Override
    public NodeCursor nodeCursorGetFromUniqueIndexSeek( KernelStatement statement,
            IndexDescriptor index,
            Object value ) throws IndexBrokenKernelException, IndexNotFoundKernelException
    {
        // TODO Filter this properly
        return statement.getStoreStatement().acquireIteratorNodeCursor(
                storeLayer.nodeGetFromUniqueIndexSeek( statement, index, value ) );
    }

    // </Cursors>

    @Override
    public long nodeCreate( KernelStatement state )
    {
        long nodeId = storeLayer.reserveNode();
        state.txState().nodeDoCreate( nodeId );
        return nodeId;
    }

    @Override
    public void nodeDelete( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        assertNodeExists( state, nodeId );
        legacyPropertyTrackers.nodeDelete( nodeId );
        state.txState().nodeDoDelete( nodeId );
    }

    private void assertNodeExists( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        if ( !nodeExists( state, nodeId ) )
        {
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
    }

    @Override
    public long relationshipCreate( KernelStatement state, int relationshipTypeId, long startNodeId, long endNodeId )
            throws EntityNotFoundException
    {
        assertNodeExists( state, startNodeId );
        assertNodeExists( state, endNodeId );
        long id = storeLayer.reserveRelationship();
        state.txState().relationshipDoCreate( id, relationshipTypeId, startNodeId, endNodeId );
        return id;
    }

    @Override
    public void relationshipDelete( final KernelStatement state, long relationshipId ) throws EntityNotFoundException
    {
        assertRelationshipExists( state, relationshipId );

        // NOTE: We implicitly delegate to neoStoreTransaction via txState.legacyState here. This is because that
        // call returns modified properties, which node manager uses to update legacy tx state. This will be cleaned up
        // once we've removed legacy tx state.
        legacyPropertyTrackers.relationshipDelete( relationshipId );
        final TransactionState txState = state.txState();
        if ( txState.relationshipIsAddedInThisTx( relationshipId ) )
        {
            txState.relationshipDoDeleteAddedInThisTx( relationshipId );
        }
        else
        {
            try
            {
                storeLayer.relationshipVisit( relationshipId, new RelationshipVisitor<RuntimeException>()
                {
                    @Override
                    public void visit( long relId, int type, long startNode, long endNode )
                    {
                        txState.relationshipDoDelete( relId, type, startNode, endNode );
                    }
                } );
            }
            catch ( EntityNotFoundException e )
            {
                // If it doesn't exist, it doesn't exist, and the user got what she wanted.
                return;
            }
        }
    }

    private void assertRelationshipExists( KernelStatement state, long relationshipId ) throws EntityNotFoundException
    {
        if ( !relationshipExists( state, relationshipId ) )
        {
            throw new EntityNotFoundException( EntityType.RELATIONSHIP, relationshipId );
        }
    }

    @Override
    public boolean nodeExists( KernelStatement state, long nodeId )
    {
        try ( NodeCursor cursor = nodeCursor( state, nodeId ) )
        {
            return cursor.next();
        }
    }

    @Override
    public boolean relationshipExists( KernelStatement state, long relId )
    {
        try ( RelationshipCursor cursor = relationshipCursor( state, relId ) )
        {
            return cursor.next();
        }
    }

    @Override
    public boolean nodeHasLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        try ( NodeCursor nodeCursor = nodeCursor( state, nodeId ) )
        {
            if ( nodeCursor.next() )
            {
                try ( LabelCursor labelCursor = nodeCursor.labels() )
                {
                    return labelCursor.seek( labelId );
                }
            }
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
    }

    @Override
    public PrimitiveIntIterator nodeGetLabels( KernelStatement state, long nodeId ) throws EntityNotFoundException
    {
        return nodeGetLabels( state, state.getStoreStatement(), nodeId );
    }

    @Override
    public PrimitiveIntIterator nodeGetLabels( TxStateHolder txStateHolder, StoreStatement storeStatement, long nodeId )
            throws EntityNotFoundException
    {
        try ( NodeCursor nodeCursor = nodeCursor( txStateHolder, storeStatement, nodeId ) )
        {
            if ( nodeCursor.next() )
            {
                return Cursors.intIterator( nodeCursor.labels(), LabelCursor.GET_LABEL );
            }
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
    }

    public static PrimitiveIntIterator nodeGetLabels( StoreReadLayer storeLayer,
            StoreStatement statement,
            ReadableTxState txState,
            long nodeId )
            throws EntityNotFoundException
    {
        if ( txState.nodeIsDeletedInThisTx( nodeId ) )
        {
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
        if ( txState.nodeIsAddedInThisTx( nodeId ) )
        {
            return PrimitiveIntCollections.toPrimitiveIterator(
                    txState.nodeStateLabelDiffSets( nodeId ).getAdded().iterator() );
        }
        return txState.nodeStateLabelDiffSets( nodeId ).augment( storeLayer.nodeGetLabels( statement, nodeId ) );
    }

    @Override
    public PrimitiveLongIterator nodesGetAll( KernelStatement state )
    {
        return state.txState().augmentNodesGetAll( storeLayer.nodesGetAll() );
    }

    @Override

    public RelationshipIterator relationshipsGetAll( KernelStatement state )
    {
        return state.txState().augmentRelationshipsGetAll( storeLayer.relationshipsGetAll() );
    }

    @Override
    public boolean nodeAddLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        try ( NodeCursor node = nodeCursor( state, nodeId ) )
        {
            if ( !node.next() )
            {
                throw new EntityNotFoundException( EntityType.NODE, nodeId );
            }

            try ( LabelCursor labels = node.labels() )
            {
                if ( labels.seek( labelId ) )
                {
                    // Label is already in state or in store, no-op
                    return false;
                }
            }

            state.txState().nodeDoAddLabel( labelId, nodeId );

            try ( PropertyCursor properties = node.properties() )
            {
                while ( properties.next() )
                {
                    DefinedProperty property = Property.property( properties.propertyKeyId(), properties.value() );
                    indexUpdateProperty( state, nodeId, labelId, property.propertyKeyId(), null, property );
                }
            }

            return true;
        }
    }

    @Override
    public boolean nodeRemoveLabel( KernelStatement state, long nodeId, int labelId ) throws EntityNotFoundException
    {
        try ( NodeCursor node = nodeCursor( state, nodeId ) )
        {
            if ( !node.next() )
            {
                throw new EntityNotFoundException( EntityType.NODE, nodeId );
            }

            try ( LabelCursor labels = node.labels() )
            {
                if ( !labels.seek( labelId ) )
                {
                    // Label does not exist in state or in store, no-op
                    return false;
                }
            }

            state.txState().nodeDoRemoveLabel( labelId, nodeId );

            try ( PropertyCursor properties = node.properties() )
            {
                while ( properties.next() )
                {
                    DefinedProperty property = Property.property( properties.propertyKeyId(), properties.value() );
                    indexUpdateProperty( state, nodeId, labelId, property.propertyKeyId(), property, null );
                }
            }

            return true;
        }
    }

    @Override
    public PrimitiveLongIterator nodesGetForLabel( KernelStatement state, int labelId )
    {
        if ( state.hasTxStateWithChanges() )
        {
            PrimitiveLongIterator wLabelChanges =
                    state.txState().nodesWithLabelChanged( labelId ).augment(
                            storeLayer.nodesGetForLabel( state, labelId ) );
            return state.txState().addedAndRemovedNodes().augmentWithRemovals( wLabelChanges );
        }

        return storeLayer.nodesGetForLabel( state, labelId );
    }

    @Override
    public IndexDescriptor indexCreate( KernelStatement state, int labelId, int propertyKey )
    {
        IndexDescriptor rule = new IndexDescriptor( labelId, propertyKey );
        state.txState().indexRuleDoAdd( rule );
        return rule;
    }

    @Override
    public void indexDrop( KernelStatement state, IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        state.txState().indexDoDrop( descriptor );
    }

    @Override
    public void uniqueIndexDrop( KernelStatement state, IndexDescriptor descriptor ) throws DropIndexFailureException
    {
        state.txState().constraintIndexDoDrop( descriptor );
    }

    @Override
    public UniquenessConstraint uniquePropertyConstraintCreate( KernelStatement state, int labelId, int propertyKeyId )
            throws CreateConstraintFailureException
    {
        UniquenessConstraint constraint = new UniquenessConstraint( labelId, propertyKeyId );
        try
        {
            IndexDescriptor index = new IndexDescriptor( labelId, propertyKeyId );
            if ( state.txState().constraintIndexDoUnRemove( index ) ) // ..., DROP, *CREATE*
            { // creation is undoing a drop
                if ( !state.txState().constraintDoUnRemove( constraint ) ) // CREATE, ..., DROP, *CREATE*
                { // ... the drop we are undoing did itself undo a prior create...
                    state.txState().constraintDoAdd(
                            constraint, state.txState().indexCreatedForConstraint( constraint ) );
                }
            }
            else // *CREATE*
            { // create from scratch
                for ( Iterator<NodePropertyConstraint> it = storeLayer.constraintsGetForLabelAndPropertyKey(
                        labelId, propertyKeyId ); it.hasNext(); )
                {
                    if ( it.next().equals( constraint ) )
                    {
                        return constraint;
                    }
                }
                long indexId = constraintIndexCreator.createUniquenessConstraintIndex(
                        state, this, labelId, propertyKeyId );
                state.txState().constraintDoAdd( constraint, indexId );
            }
            return constraint;
        }
        catch ( ConstraintVerificationFailedKernelException | DropIndexFailureException | TransactionFailureException
                e )
        {
            throw new CreateConstraintFailureException( constraint, e );
        }
    }

    @Override
    public MandatoryNodePropertyConstraint mandatoryNodePropertyConstraintCreate( KernelStatement state, int labelId,
            int propertyKeyId ) throws CreateConstraintFailureException
    {
        MandatoryNodePropertyConstraint constraint = new MandatoryNodePropertyConstraint( labelId, propertyKeyId );
        state.txState().constraintDoAdd( constraint );
        return constraint;
    }

    @Override
    public MandatoryRelationshipPropertyConstraint mandatoryRelationshipPropertyConstraintCreate( KernelStatement state,
            int relTypeId, int propertyKeyId ) throws AlreadyConstrainedException, CreateConstraintFailureException
    {
        MandatoryRelationshipPropertyConstraint constraint =
                new MandatoryRelationshipPropertyConstraint( relTypeId, propertyKeyId );
        state.txState().constraintDoAdd( constraint );
        return constraint;
    }

    @Override
    public Iterator<NodePropertyConstraint> constraintsGetForLabelAndPropertyKey( KernelStatement state,
            int labelId, int propertyKeyId )
    {
        Iterator<NodePropertyConstraint> constraints =
                storeLayer.constraintsGetForLabelAndPropertyKey( labelId, propertyKeyId );
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintsChangesForLabelAndProperty( labelId, propertyKeyId ).apply( constraints );
        }
        return constraints;
    }

    @Override
    public Iterator<NodePropertyConstraint> constraintsGetForLabel( KernelStatement state, int labelId )
    {
        Iterator<NodePropertyConstraint> constraints = storeLayer.constraintsGetForLabel( labelId );
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintsChangesForLabel( labelId ).apply( constraints );
        }
        return constraints;
    }

    @Override
    public Iterator<RelationshipPropertyConstraint> constraintsGetForRelationshipTypeAndPropertyKey(
            KernelStatement state, int relTypeId, int propertyKeyId )
    {
        Iterator<RelationshipPropertyConstraint> constraints =
                storeLayer.constraintsGetForRelationshipTypeAndPropertyKey( relTypeId, propertyKeyId );
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState()
                    .constraintsChangesForRelationshipTypeAndProperty( relTypeId, propertyKeyId )
                    .apply( constraints );
        }
        return constraints;
    }

    @Override
    public Iterator<RelationshipPropertyConstraint> constraintsGetForRelationshipType( KernelStatement state, int typeId )
    {
        Iterator<RelationshipPropertyConstraint> constraints = storeLayer.constraintsGetForRelationshipType( typeId );
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintsChangesForRelationshipType( typeId ).apply( constraints );
        }
        return constraints;
    }

    @Override
    public Iterator<PropertyConstraint> constraintsGetAll( KernelStatement state )
    {
        Iterator<PropertyConstraint> constraints = storeLayer.constraintsGetAll();
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintsChanges().apply( constraints );
        }
        return constraints;
    }


    @Override
    public void constraintDrop( KernelStatement state, NodePropertyConstraint constraint )
    {
        state.txState().constraintDoDrop( constraint );
    }

    @Override
    public void constraintDrop( KernelStatement state, RelationshipPropertyConstraint constraint )
            throws DropConstraintFailureException
    {
        state.txState().constraintDoDrop( constraint );
    }

    @Override
    public IndexDescriptor indexesGetForLabelAndPropertyKey( KernelStatement state, int labelId, int propertyKey )
    {
        IndexDescriptor indexDescriptor = storeLayer.indexesGetForLabelAndPropertyKey( labelId, propertyKey );

        Iterator<IndexDescriptor> rules = iterator( indexDescriptor );
        if ( state.hasTxStateWithChanges() )
        {
            rules = filterByPropertyKeyId(
                    state.txState().indexDiffSetsByLabel( labelId ).apply( rules ),
                    propertyKey );
        }
        return singleOrNull( rules );
    }

    private Iterator<IndexDescriptor> filterByPropertyKeyId(
            Iterator<IndexDescriptor> descriptorIterator,
            final int propertyKey )
    {
        Predicate<IndexDescriptor> predicate = new Predicate<IndexDescriptor>()
        {
            @Override
            public boolean test( IndexDescriptor item )
            {
                return item.getPropertyKeyId() == propertyKey;
            }
        };
        return filter( predicate, descriptorIterator );
    }

    @Override
    public InternalIndexState indexGetState( KernelStatement state, IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        // If index is in our state, then return populating
        if ( state.hasTxStateWithChanges() )
        {
            if ( checkIndexState( descriptor, state.txState().indexDiffSetsByLabel( descriptor.getLabelId() ) ) )
            {
                return InternalIndexState.POPULATING;
            }
            ReadableDiffSets<IndexDescriptor> changes =
                    state.txState().constraintIndexDiffSetsByLabel( descriptor.getLabelId() );
            if ( checkIndexState( descriptor, changes ) )
            {
                return InternalIndexState.POPULATING;
            }
        }

        return storeLayer.indexGetState( descriptor );
    }

    private boolean checkIndexState( IndexDescriptor indexRule, ReadableDiffSets<IndexDescriptor> diffSet )
            throws IndexNotFoundKernelException
    {
        if ( diffSet.isAdded( indexRule ) )
        {
            return true;
        }
        if ( diffSet.isRemoved( indexRule ) )
        {
            throw new IndexNotFoundKernelException( String.format( "Index for label id %d on property id %d has been " +
                            "dropped in this transaction.",
                    indexRule.getLabelId(),
                    indexRule.getPropertyKeyId() ) );
        }
        return false;
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetForLabel( KernelStatement state, int labelId )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().indexDiffSetsByLabel( labelId )
                    .apply( storeLayer.indexesGetForLabel( labelId ) );
        }

        return storeLayer.indexesGetForLabel( labelId );
    }

    @Override
    public Iterator<IndexDescriptor> indexesGetAll( KernelStatement state )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().indexChanges().apply( storeLayer.indexesGetAll() );
        }

        return storeLayer.indexesGetAll();
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetForLabel( KernelStatement state, int labelId )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintIndexDiffSetsByLabel( labelId )
                    .apply( storeLayer.uniqueIndexesGetForLabel( labelId ) );
        }

        return storeLayer.uniqueIndexesGetForLabel( labelId );
    }

    @Override
    public Iterator<IndexDescriptor> uniqueIndexesGetAll( KernelStatement state )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().constraintIndexChanges()
                    .apply( storeLayer.uniqueIndexesGetAll() );
        }

        return storeLayer.uniqueIndexesGetAll();
    }

    @Override
    public long nodeGetFromUniqueIndexSeek( KernelStatement state, IndexDescriptor index, Object value )
            throws IndexNotFoundKernelException, IndexBrokenKernelException
    {
        PrimitiveLongResourceIterator committed = storeLayer.nodeGetFromUniqueIndexSeek( state, index, value );
        PrimitiveLongIterator exactMatches = filterExactIndexMatches( state, index, value, committed );
        PrimitiveLongIterator changesFiltered = filterIndexStateChangesForScanOrSeek(state, index, value, exactMatches);
        return single( resourceIterator( changesFiltered, committed ), NO_SUCH_NODE );
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexSeek( KernelStatement state, IndexDescriptor index, Object value )
            throws IndexNotFoundKernelException
    {
        PrimitiveLongIterator committed = storeLayer.nodesGetFromIndexSeek( state, index, value );
        PrimitiveLongIterator exactMatches = filterExactIndexMatches( state, index, value, committed );
        return filterIndexStateChangesForScanOrSeek(state, index, value, exactMatches);
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexRangeSeekByNumber( KernelStatement state, IndexDescriptor index,
                                                                     Number lower, boolean includeLower,
                                                                     Number upper, boolean includeUpper ) throws IndexNotFoundKernelException

    {
        PrimitiveLongIterator committed = storeLayer.nodesGetFromIndexRangeSeekByNumber( state, index, lower, includeLower, upper, includeUpper );
        return filterIndexStateChangesForRangeSeekByNumber(state, index, lower, includeLower, upper, includeUpper, committed);
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexRangeSeekByPrefix( KernelStatement state, IndexDescriptor index,
                                                                     String prefix ) throws IndexNotFoundKernelException
    {
        PrimitiveLongIterator committed = storeLayer.nodesGetFromIndexRangeSeekByPrefix( state, index, prefix );
        return filterIndexStateChangesForRangeSeekByPrefix(state, index, prefix, committed);
    }

    @Override
    public PrimitiveLongIterator nodesGetFromIndexScan( KernelStatement state, IndexDescriptor index )
            throws IndexNotFoundKernelException
    {
        PrimitiveLongIterator committed = storeLayer.nodesGetFromIndexScan( state, index );
        return filterIndexStateChangesForScanOrSeek(state, index, null, committed);
    }

    private PrimitiveLongIterator filterExactIndexMatches( final KernelStatement state, IndexDescriptor index,
            Object value, PrimitiveLongIterator committed )
    {
        return LookupFilter.exactIndexMatches( this, state, committed, index.getPropertyKeyId(), value );
    }

    private PrimitiveLongIterator filterIndexStateChangesForScanOrSeek(KernelStatement state, IndexDescriptor index,
                                                                       Object value, PrimitiveLongIterator nodeIds)
    {
        if ( state.hasTxStateWithChanges() )
        {
            ReadableDiffSets<Long> labelPropertyChanges = state.txState().indexUpdatesForScanOrSeek( index, value );
            ReadableDiffSets<Long> nodes = state.txState().addedAndRemovedNodes();

            // Apply to actual index lookup
            return nodes.augmentWithRemovals( labelPropertyChanges.augment( nodeIds ) );
        }
        return nodeIds;
    }

    private PrimitiveLongIterator filterIndexStateChangesForRangeSeekByNumber( KernelStatement state,
                                                                               IndexDescriptor index,
                                                                               Number lower, boolean includeLower,
                                                                               Number upper, boolean includeUpper,
                                                                               PrimitiveLongIterator nodeIds )
    {
        if ( state.hasTxStateWithChanges() )
        {
            ReadableDiffSets<Long> labelPropertyChangesForPrefix =
                    state.txState().indexUpdatesForRangeSeekByNumber( index, lower, includeLower, upper, includeUpper );
            ReadableDiffSets<Long> nodes = state.txState().addedAndRemovedNodes();

            // Apply to actual index lookup
            return nodes.augmentWithRemovals( labelPropertyChangesForPrefix.augment( nodeIds ) );
        }
        return nodeIds;

    }

    private PrimitiveLongIterator filterIndexStateChangesForRangeSeekByPrefix( KernelStatement state,
                                                                               IndexDescriptor index,
                                                                               String prefix,
                                                                               PrimitiveLongIterator nodeIds )
    {
        if ( state.hasTxStateWithChanges() )
        {
            ReadableDiffSets<Long> labelPropertyChangesForPrefix =
                    state.txState().indexUpdatesForRangeSeekByPrefix( index, prefix );
            ReadableDiffSets<Long> nodes = state.txState().addedAndRemovedNodes();

            // Apply to actual index lookup
            return nodes.augmentWithRemovals( labelPropertyChangesForPrefix.augment( nodeIds ) );
        }
        return nodeIds;
    }

    @Override
    public Property nodeSetProperty( KernelStatement state, long nodeId, DefinedProperty property )
            throws EntityNotFoundException
    {
        try ( NodeCursor node = nodeCursor( state, nodeId ) )
        {
            if ( !node.next() )
            {
                throw new EntityNotFoundException( EntityType.NODE, nodeId );
            }

            Property existingProperty;
            try ( PropertyCursor properties = node.properties() )
            {
                if ( !properties.seek( property.propertyKeyId() ) )
                {
                    legacyPropertyTrackers.nodeAddStoreProperty( nodeId, property );
                    existingProperty = Property.noProperty( property.propertyKeyId(), EntityType.NODE, nodeId );
                }
                else
                {
                    existingProperty = Property.property( properties.propertyKeyId(), properties.value() );
                    legacyPropertyTrackers.nodeChangeStoreProperty( nodeId, (DefinedProperty) existingProperty,
                            property );
                }
            }

            state.txState().nodeDoReplaceProperty( nodeId, existingProperty, property );

            PrimitiveIntCollection labelIds = getLabels( node );

            indexesUpdateProperty( state, node.getId(), labelIds, property.propertyKeyId(),
                    existingProperty instanceof DefinedProperty ? (DefinedProperty) existingProperty : null,
                    property );

            return existingProperty;
        }
    }

    @Override
    public Property relationshipSetProperty( KernelStatement state, long relationshipId, DefinedProperty property )
            throws EntityNotFoundException
    {
        try ( RelationshipCursor relationship = relationshipCursor( state, relationshipId ) )
        {
            if ( !relationship.next() )
            {
                throw new EntityNotFoundException( EntityType.RELATIONSHIP, relationshipId );
            }

            Property existingProperty;
            try ( PropertyCursor properties = relationship.properties() )
            {
                if ( !properties.seek( property.propertyKeyId() ) )
                {
                    legacyPropertyTrackers.relationshipAddStoreProperty( relationshipId, property );
                    existingProperty = Property.noProperty( property.propertyKeyId(), EntityType.RELATIONSHIP,
                            relationshipId );
                }
                else
                {
                    existingProperty = Property.property( properties.propertyKeyId(), properties.value() );
                    legacyPropertyTrackers.relationshipChangeStoreProperty( relationshipId,
                            (DefinedProperty) existingProperty,
                            property );
                }
            }

            state.txState().relationshipDoReplaceProperty( relationshipId, existingProperty, property );
            return existingProperty;
        }
    }

    @Override
    public Property graphSetProperty( KernelStatement state, DefinedProperty property )
    {
        Object existingPropertyValue = graphGetProperty( state, property.propertyKeyId() );
        Property existingProperty = existingPropertyValue == null ?
                Property.noGraphProperty( property.propertyKeyId() ) :
                Property.property( property.propertyKeyId(), existingPropertyValue );
        state.txState().graphDoReplaceProperty( existingProperty, property );
        return existingProperty;
    }

    @Override
    public Property nodeRemoveProperty( KernelStatement state, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        try ( NodeCursor node = nodeCursor( state, nodeId ) )
        {
            if ( !node.next() )
            {
                throw new EntityNotFoundException( EntityType.NODE, nodeId );
            }

            PrimitiveIntCollection labelIds = getLabels( node );

            Property existingProperty;
            try ( PropertyCursor properties = node.properties() )
            {
                if ( !properties.seek( propertyKeyId ) )
                {
                    existingProperty = Property.noProperty( propertyKeyId, EntityType.NODE, nodeId );
                }
                else
                {
                    existingProperty = Property.property( properties.propertyKeyId(), properties.value() );

                    legacyPropertyTrackers.nodeRemoveStoreProperty( nodeId, (DefinedProperty) existingProperty );
                    state.txState().nodeDoRemoveProperty( nodeId, (DefinedProperty) existingProperty );

                    indexesUpdateProperty( state, node.getId(), labelIds, propertyKeyId,
                            (DefinedProperty) existingProperty, null );
                }
            }
            return existingProperty;
        }
    }

    @Override
    public Property relationshipRemoveProperty( KernelStatement state, long relationshipId, int propertyKeyId )
            throws EntityNotFoundException
    {
        try ( RelationshipCursor relationship = relationshipCursor( state, relationshipId ) )
        {
            if ( !relationship.next() )
            {
                throw new EntityNotFoundException( EntityType.RELATIONSHIP, relationshipId );
            }

            Property existingProperty;
            try ( PropertyCursor properties = relationship.properties() )
            {
                if ( !properties.seek( propertyKeyId ) )
                {
                    existingProperty = Property.noProperty( propertyKeyId, EntityType.RELATIONSHIP, relationshipId );
                }
                else
                {
                    existingProperty = Property.property( properties.propertyKeyId(), properties.value() );

                    legacyPropertyTrackers.relationshipRemoveStoreProperty( relationshipId,
                            (DefinedProperty) existingProperty );
                    state.txState().relationshipDoRemoveProperty( relationshipId, (DefinedProperty) existingProperty );
                }
            }
            return existingProperty;
        }
    }

    @Override
    public Property graphRemoveProperty( KernelStatement state, int propertyKeyId )
    {
        Object existingPropertyValue = graphGetProperty( state, propertyKeyId );
        if ( existingPropertyValue != null )
        {
            DefinedProperty existingProperty = Property.property( propertyKeyId, existingPropertyValue );
            state.txState().graphDoRemoveProperty( existingProperty );
            return existingProperty;
        }
        else
        {
            return Property.noGraphProperty( propertyKeyId );
        }
    }

    private void indexesUpdateProperty( KernelStatement state,
            long nodeId,
            PrimitiveIntCollection labels,
            int propertyKey,
            DefinedProperty before,
            DefinedProperty after ) throws EntityNotFoundException
    {
        PrimitiveIntIterator labelIterator = labels.iterator();
        while ( labelIterator.hasNext() )
        {
            indexUpdateProperty( state, nodeId, labelIterator.next(), propertyKey, before, after );
        }
    }

    private void indexUpdateProperty( KernelStatement state, long nodeId, int labelId, int propertyKey,
            DefinedProperty before, DefinedProperty after )
    {
        IndexDescriptor descriptor = indexesGetForLabelAndPropertyKey( state, labelId, propertyKey );
        if ( descriptor != null )
        {
            state.txState().indexDoUpdateProperty( descriptor, nodeId, before, after );
        }
    }

    @Override
    public PrimitiveIntIterator nodeGetPropertyKeys( KernelStatement state, long nodeId )
            throws EntityNotFoundException
    {
        try ( NodeCursor nodeCursor = nodeCursor( state, nodeId ) )
        {
            if ( !nodeCursor.next() )
            {
                throw new EntityNotFoundException( EntityType.NODE, nodeId );
            }

            return getPropertyKeys( nodeCursor );
        }
    }

    @Override
    public boolean nodeHasProperty( KernelStatement statement, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        return nodeHasProperty( statement, statement.getStoreStatement(), nodeId, propertyKeyId );
    }

    @Override
    public boolean nodeHasProperty( TxStateHolder txStateHolder, StoreStatement storeStatement,
            long nodeId, int propertyKeyId ) throws EntityNotFoundException
    {
        try ( NodeCursor nodeCursor = nodeCursor( txStateHolder, storeStatement, nodeId ) )
        {
            return hasProperty( nodeCursor, EntityType.NODE, nodeId, propertyKeyId );
        }
    }

    @Override
    public Object nodeGetProperty( KernelStatement state, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        try ( NodeCursor nodeCursor = nodeCursor( state, nodeId ) )
        {
            return getProperty( nodeCursor, EntityType.NODE, nodeId, propertyKeyId );
        }
    }

    @Override
    public int relationshipGetType( TxStateHolder txStateHolder, StoreStatement storeStatement, long relationshipId )
            throws EntityNotFoundException
    {
        try ( RelationshipCursor cursor = relationshipCursor( txStateHolder, storeStatement, relationshipId ) )
        {
            if ( cursor.next() )
            {
                return cursor.getType();
            }
            throw new EntityNotFoundException( EntityType.RELATIONSHIP, relationshipId );
        }
    }

    @Override
    public PrimitiveIntIterator relationshipGetPropertyKeys( KernelStatement state, long relationshipId )
            throws EntityNotFoundException
    {
        try ( RelationshipCursor relCursor = relationshipCursor( state, relationshipId ) )
        {
            if ( !relCursor.next() )
            {
                throw new EntityNotFoundException( EntityType.RELATIONSHIP, relationshipId );
            }

            return getPropertyKeys( relCursor );
        }
    }

    @Override
    public boolean relationshipHasProperty( KernelStatement state,
            long relationshipId,
            int propertyKeyId ) throws EntityNotFoundException
    {
        try ( RelationshipCursor relationshipCursor = relationshipCursor( state, relationshipId ) )
        {
            return hasProperty( relationshipCursor, EntityType.RELATIONSHIP, relationshipId, propertyKeyId );
        }
    }

    @Override
    public boolean relationshipHasProperty( TxStateHolder txStateHolder, StoreStatement storeStatement,
            long relationshipId, int propertyKeyId ) throws EntityNotFoundException
    {
        try ( RelationshipCursor cursor = relationshipCursor( txStateHolder, storeStatement, relationshipId ) )
        {
            return hasProperty( cursor, EntityType.RELATIONSHIP, relationshipId, propertyKeyId );
        }
    }

    @Override
    public Object relationshipGetProperty( KernelStatement state, long relationshipId, int propertyKeyId )
            throws EntityNotFoundException
    {
        try ( RelationshipCursor relationshipCursor = relationshipCursor( state, relationshipId ) )
        {
            return getProperty( relationshipCursor, EntityType.RELATIONSHIP, relationshipId, propertyKeyId );
        }
    }

    @Override
    public PrimitiveIntIterator graphGetPropertyKeys( KernelStatement state )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return new PropertyKeyIdIterator( graphGetAllProperties( state ) );
        }

        return storeLayer.graphGetPropertyKeys( state );
    }

    @Override
    public boolean graphHasProperty( KernelStatement state, int propertyKeyId )
    {
        return graphGetProperty( state, propertyKeyId ) != null;
    }

    @Override
    public Object graphGetProperty( KernelStatement state, int propertyKeyId )
    {
        Iterator<DefinedProperty> properties = graphGetAllProperties( state );
        while ( properties.hasNext() )
        {
            DefinedProperty property = properties.next();
            if ( property.propertyKeyId() == propertyKeyId )
            {
                return property.value();
            }
        }
        return null;
    }

    private Iterator<DefinedProperty> graphGetAllProperties( KernelStatement state )
    {
        if ( state.hasTxStateWithChanges() )
        {
            return state.txState().augmentGraphProperties( storeLayer.graphGetAllProperties() );
        }

        return storeLayer.graphGetAllProperties();
    }

    @Override
    public long countsForNode( KernelStatement statement, int labelId )
    {
        return storeLayer.countsForNode( labelId );
    }

    @Override
    public long countsForRelationship( KernelStatement statement, int startLabelId, int typeId, int endLabelId )
    {
        return storeLayer.countsForRelationship( startLabelId, typeId, endLabelId );
    }

    @Override
    public long indexSize( KernelStatement statement, IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        return storeLayer.indexSize( descriptor );
    }

    @Override
    public double indexUniqueValuesPercentage( KernelStatement statement, IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        return storeLayer.indexUniqueValuesPercentage( descriptor );
    }

    @Override
    public RelationshipIterator nodeGetRelationships( KernelStatement state, long nodeId, Direction direction,
            int[] relTypes ) throws EntityNotFoundException
    {
        relTypes = deduplicate( relTypes );

        try ( final NodeCursor nodeCursor = nodeCursor( state, nodeId ) )
        {
            if ( nodeCursor.next() )
            {
                return new CursorRelationshipIterator( nodeCursor.relationships( direction, relTypes ) );
            }
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
    }

    @Override
    public RelationshipIterator nodeGetRelationships( KernelStatement state,
            long nodeId,
            Direction direction ) throws EntityNotFoundException
    {
        try ( final NodeCursor nodeCursor = nodeCursor( state, nodeId ) )
        {
            if ( nodeCursor.next() )
            {
                return new CursorRelationshipIterator( nodeCursor.relationships( direction ) );
            }
            throw new EntityNotFoundException( EntityType.NODE, nodeId );
        }
    }

    @Override
    public int nodeGetDegree( KernelStatement state,
            long nodeId,
            Direction direction,
            int relType ) throws EntityNotFoundException

    {
        if ( state.hasTxStateWithChanges() )
        {
            int degree = 0;
            if ( state.txState().nodeIsDeletedInThisTx( nodeId ) )
            {
                return 0;
            }

            if ( !state.txState().nodeIsAddedInThisTx( nodeId ) )
            {
                degree = storeLayer.nodeGetDegree( state.getStoreStatement(), nodeId, direction, relType );
            }

            return state.txState().augmentNodeDegree( nodeId, degree, direction, relType );
        }
        return storeLayer.nodeGetDegree( state.getStoreStatement(), nodeId, direction, relType );
    }

    @Override
    public int nodeGetDegree( KernelStatement state, long nodeId, Direction direction ) throws EntityNotFoundException
    {
        if ( state.hasTxStateWithChanges() )
        {
            int degree = 0;
            if ( state.txState().nodeIsDeletedInThisTx( nodeId ) )
            {
                return 0;
            }

            if ( !state.txState().nodeIsAddedInThisTx( nodeId ) )
            {
                degree = storeLayer.nodeGetDegree( state.getStoreStatement(), nodeId, direction );
            }
            return state.txState().augmentNodeDegree( nodeId, degree, direction );
        }
        return storeLayer.nodeGetDegree( state.getStoreStatement(), nodeId, direction );
    }

    @Override
    public PrimitiveIntIterator nodeGetRelationshipTypes( KernelStatement state, long nodeId )
            throws EntityNotFoundException
    {
        if ( state.hasTxStateWithChanges() && state.txState().nodeModifiedInThisTx( nodeId ) )
        {
            ReadableTxState tx = state.txState();
            if ( tx.nodeIsDeletedInThisTx( nodeId ) )
            {
                return PrimitiveIntCollections.emptyIterator();
            }

            if ( tx.nodeIsAddedInThisTx( nodeId ) )
            {
                return tx.nodeRelationshipTypes( nodeId );
            }

            Set<Integer> types = new HashSet<>();

            // Add types in the current transaction
            PrimitiveIntIterator typesInTx = tx.nodeRelationshipTypes( nodeId );
            while ( typesInTx.hasNext() )
            {
                types.add( typesInTx.next() );
            }

            // Augment with types stored on disk, minus any types where all rels of that type are deleted
            // in current tx.
            PrimitiveIntIterator committedTypes = storeLayer.nodeGetRelationshipTypes( state.getStoreStatement(),
                    nodeId );
            while ( committedTypes.hasNext() )
            {
                int current = committedTypes.next();
                if ( !types.contains( current ) && nodeGetDegree( state, nodeId, Direction.BOTH, current ) > 0 )
                {
                    types.add( current );
                }
            }

            return PrimitiveIntCollections.toPrimitiveIterator( types.iterator() );
        }
        return storeLayer.nodeGetRelationshipTypes( state.getStoreStatement(), nodeId );
    }

    //
    // Methods that delegate directly to storage
    //

    @Override
    public Long indexGetOwningUniquenessConstraintId( KernelStatement state, IndexDescriptor index )
            throws SchemaRuleNotFoundException
    {
        return storeLayer.indexGetOwningUniquenessConstraintId( index );
    }

    @Override
    public long indexGetCommittedId( KernelStatement state, IndexDescriptor index, SchemaStorage.IndexRuleKind kind )
            throws SchemaRuleNotFoundException
    {
        return storeLayer.indexGetCommittedId( index, kind );
    }

    @Override
    public String indexGetFailure( Statement state, IndexDescriptor descriptor )
            throws IndexNotFoundKernelException
    {
        return storeLayer.indexGetFailure( descriptor );
    }

    @Override
    public int labelGetForName( Statement state, String labelName )
    {
        return storeLayer.labelGetForName( labelName );
    }

    @Override
    public String labelGetName( Statement state, int labelId ) throws LabelNotFoundKernelException
    {
        return storeLayer.labelGetName( labelId );
    }

    @Override
    public int propertyKeyGetForName( Statement state, String propertyKeyName )
    {
        return storeLayer.propertyKeyGetForName( propertyKeyName );
    }

    @Override
    public String propertyKeyGetName( Statement state, int propertyKeyId ) throws PropertyKeyIdNotFoundKernelException
    {
        return storeLayer.propertyKeyGetName( propertyKeyId );
    }

    @Override
    public Iterator<Token> propertyKeyGetAllTokens( Statement state )
    {
        return storeLayer.propertyKeyGetAllTokens();
    }

    @Override
    public Iterator<Token> labelsGetAllTokens( Statement state )
    {
        return storeLayer.labelsGetAllTokens();
    }

    @Override
    public int relationshipTypeGetForName( Statement state, String relationshipTypeName )
    {
        return storeLayer.relationshipTypeGetForName( relationshipTypeName );
    }

    @Override
    public String relationshipTypeGetName( Statement state, int relationshipTypeId ) throws
            RelationshipTypeIdNotFoundKernelException
    {
        return storeLayer.relationshipTypeGetName( relationshipTypeId );
    }

    @Override
    public int labelGetOrCreateForName( Statement state, String labelName ) throws IllegalTokenNameException,
            TooManyLabelsException
    {
        return storeLayer.labelGetOrCreateForName( labelName );
    }

    @Override
    public int propertyKeyGetOrCreateForName( Statement state, String propertyKeyName ) throws IllegalTokenNameException
    {
        return storeLayer.propertyKeyGetOrCreateForName( propertyKeyName );
    }

    @Override
    public int relationshipTypeGetOrCreateForName( Statement state, String relationshipTypeName )
            throws IllegalTokenNameException
    {
        return storeLayer.relationshipTypeGetOrCreateForName( relationshipTypeName );
    }

    @Override
    public void labelCreateForName( KernelStatement state, String labelName,
            int id ) throws IllegalTokenNameException, TooManyLabelsException
    {
        state.txState().labelDoCreateForName( labelName, id );
    }

    @Override
    public void propertyKeyCreateForName( KernelStatement state,
            String propertyKeyName,
            int id ) throws IllegalTokenNameException
    {
        state.txState().propertyKeyDoCreateForName( propertyKeyName, id );

    }

    @Override
    public void relationshipTypeCreateForName( KernelStatement state,
            String relationshipTypeName,
            int id ) throws IllegalTokenNameException
    {
        state.txState().relationshipTypeDoCreateForName( relationshipTypeName, id );
    }

    private static int[] deduplicate( int[] types )
    {
        int unique = 0;
        for ( int i = 0; i < types.length; i++ )
        {
            int type = types[i];
            for ( int j = 0; j < unique; j++ )
            {
                if ( type == types[j] )
                {
                    type = -1; // signal that this relationship is not unique
                    break; // we will not find more than one conflict
                }
            }
            if ( type != -1 )
            { // this has to be done outside the inner loop, otherwise we'd never accept a single one...
                types[unique++] = types[i];
            }
        }
        if ( unique < types.length )
        {
            types = Arrays.copyOf( types, unique );
        }
        return types;
    }

    // <Legacy index>
    @Override
    public <EXCEPTION extends Exception> void relationshipVisit( KernelStatement statement,
            long relId, RelationshipVisitor<EXCEPTION> visitor ) throws EntityNotFoundException, EXCEPTION
    {
        if ( statement.hasTxStateWithChanges() )
        {
            if ( statement.txState().relationshipVisit( relId, visitor ) )
            {
                return;
            }
        }
        storeLayer.relationshipVisit( relId, visitor );
    }

    @Override
    public LegacyIndexHits nodeLegacyIndexGet( KernelStatement statement, String indexName, String key, Object value )
            throws LegacyIndexNotFoundKernelException
    {
        return statement.legacyIndexTxState().nodeChanges( indexName ).get( key, value );
    }

    @Override
    public LegacyIndexHits nodeLegacyIndexQuery( KernelStatement statement, String indexName, String key,
            Object queryOrQueryObject ) throws LegacyIndexNotFoundKernelException
    {
        return statement.legacyIndexTxState().nodeChanges( indexName ).query( key, queryOrQueryObject );
    }

    @Override
    public LegacyIndexHits nodeLegacyIndexQuery( KernelStatement statement,
            String indexName,
            Object queryOrQueryObject )
            throws LegacyIndexNotFoundKernelException
    {
        return statement.legacyIndexTxState().nodeChanges( indexName ).query( queryOrQueryObject );
    }

    @Override
    public LegacyIndexHits relationshipLegacyIndexGet( KernelStatement statement, String indexName, String key,
            Object value, long startNode, long endNode ) throws LegacyIndexNotFoundKernelException
    {
        LegacyIndex index = statement.legacyIndexTxState().relationshipChanges( indexName );
        if ( startNode != -1 || endNode != -1 )
        {
            return index.get( key, value, startNode, endNode );
        }
        return index.get( key, value );
    }

    @Override
    public LegacyIndexHits relationshipLegacyIndexQuery( KernelStatement statement, String indexName, String key,
            Object queryOrQueryObject, long startNode, long endNode ) throws LegacyIndexNotFoundKernelException
    {
        LegacyIndex index = statement.legacyIndexTxState().relationshipChanges( indexName );
        if ( startNode != -1 || endNode != -1 )
        {
            return index.query( key, queryOrQueryObject, startNode, endNode );
        }
        return index.query( key, queryOrQueryObject );
    }

    @Override
    public LegacyIndexHits relationshipLegacyIndexQuery( KernelStatement statement, String indexName,
            Object queryOrQueryObject, long startNode, long endNode ) throws LegacyIndexNotFoundKernelException
    {
        LegacyIndex index = statement.legacyIndexTxState().relationshipChanges( indexName );
        if ( startNode != -1 || endNode != -1 )
        {
            return index.query( queryOrQueryObject, startNode, endNode );
        }
        return index.query( queryOrQueryObject );
    }

    @Override
    public void nodeLegacyIndexCreateLazily( KernelStatement statement, String indexName,
            Map<String, String> customConfig )
    {
        legacyIndexStore.getOrCreateNodeIndexConfig( indexName, customConfig );
    }

    @Override
    public void nodeLegacyIndexCreate( KernelStatement statement, String indexName, Map<String, String> customConfig )
    {
        statement.txState().nodeLegacyIndexDoCreate( indexName, customConfig );
    }

    @Override
    public void relationshipLegacyIndexCreateLazily( KernelStatement statement, String indexName,
            Map<String, String> customConfig )
    {
        legacyIndexStore.getOrCreateRelationshipIndexConfig( indexName, customConfig );
    }

    @Override
    public void relationshipLegacyIndexCreate( KernelStatement statement,
            String indexName,
            Map<String, String> customConfig )
    {
        statement.txState().relationshipLegacyIndexDoCreate( indexName, customConfig );
    }

    @Override
    public void nodeAddToLegacyIndex( KernelStatement statement, String indexName, long node, String key, Object value )
            throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().nodeChanges( indexName ).addNode( node, key, value );
    }

    @Override
    public void nodeRemoveFromLegacyIndex( KernelStatement statement, String indexName, long node, String key,
            Object value ) throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().nodeChanges( indexName ).remove( node, key, value );
    }

    @Override
    public void nodeRemoveFromLegacyIndex( KernelStatement statement, String indexName, long node, String key )
            throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().nodeChanges( indexName ).remove( node, key );
    }

    @Override
    public void nodeRemoveFromLegacyIndex( KernelStatement statement, String indexName, long node )
            throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().nodeChanges( indexName ).remove( node );
    }

    @Override
    public void relationshipAddToLegacyIndex( final KernelStatement statement, final String indexName,
            final long relationship, final String key, final Object value )
            throws EntityNotFoundException, LegacyIndexNotFoundKernelException
    {
        relationshipVisit( statement, relationship, new RelationshipVisitor<LegacyIndexNotFoundKernelException>()
        {
            @Override
            public void visit( long relId, int type, long startNode, long endNode )
                    throws LegacyIndexNotFoundKernelException
            {
                statement.legacyIndexTxState().relationshipChanges( indexName ).addRelationship(
                        relationship, key, value, startNode, endNode );
            }
        } );
    }

    @Override
    public void relationshipRemoveFromLegacyIndex( final KernelStatement statement,
            final String indexName,
            long relationship,
            final String key,
            final Object value ) throws LegacyIndexNotFoundKernelException, EntityNotFoundException
    {
        try
        {
            relationshipVisit( statement, relationship, new RelationshipVisitor<LegacyIndexNotFoundKernelException>()
            {
                @Override
                public void visit( long relId, int type, long startNode, long endNode )
                        throws LegacyIndexNotFoundKernelException
                {
                    statement.legacyIndexTxState().relationshipChanges( indexName ).removeRelationship(
                            relId, key, value, startNode, endNode );
                }
            } );
        }
        catch ( EntityNotFoundException e )
        {   // Apparently this is OK
        }
    }

    @Override
    public void relationshipRemoveFromLegacyIndex( final KernelStatement statement,
            final String indexName,
            long relationship,
            final String key ) throws EntityNotFoundException, LegacyIndexNotFoundKernelException
    {
        try
        {
            relationshipVisit( statement, relationship, new RelationshipVisitor<LegacyIndexNotFoundKernelException>()
            {
                @Override
                public void visit( long relId, int type, long startNode, long endNode )
                        throws LegacyIndexNotFoundKernelException
                {
                    statement.legacyIndexTxState().relationshipChanges( indexName ).removeRelationship(
                            relId, key, startNode, endNode );
                }
            } );
        }
        catch ( EntityNotFoundException e )
        {   // Apparently this is OK
        }
    }

    @Override
    public void relationshipRemoveFromLegacyIndex( final KernelStatement statement,
            final String indexName,
            long relationship )
            throws LegacyIndexNotFoundKernelException, EntityNotFoundException
    {
        try
        {
            relationshipVisit( statement, relationship, new RelationshipVisitor<LegacyIndexNotFoundKernelException>()
            {
                @Override
                public void visit( long relId, int type, long startNode, long endNode )
                        throws LegacyIndexNotFoundKernelException
                {
                    statement.legacyIndexTxState().relationshipChanges( indexName ).removeRelationship(
                            relId, startNode, endNode );
                }
            } );
        }
        catch ( EntityNotFoundException e )
        {
            // This is a special case which is still OK. This method is called lazily where deleted relationships
            // that still are referenced by a legacy index will be added for removal in this transaction.
            // Ideally we'd want to include start/end node too, but we can't since the relationship doesn't exist.
            // So we do the "normal" remove call on the legacy index transaction changes. The downside is that
            // Some queries on this transaction state that include start/end nodes might produce invalid results.
            statement.legacyIndexTxState().relationshipChanges( indexName ).remove( relationship );
        }
    }

    @Override
    public void nodeLegacyIndexDrop( KernelStatement statement,
            String indexName ) throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().nodeChanges( indexName ).drop();
        statement.legacyIndexTxState().deleteIndex( IndexEntityType.Node, indexName );
    }

    @Override
    public void relationshipLegacyIndexDrop( KernelStatement statement, String indexName )
            throws LegacyIndexNotFoundKernelException
    {
        statement.legacyIndexTxState().relationshipChanges( indexName ).drop();
        statement.legacyIndexTxState().deleteIndex( IndexEntityType.Relationship, indexName );
    }

    @Override
    public String nodeLegacyIndexSetConfiguration( KernelStatement statement,
            String indexName,
            String key,
            String value )
            throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.setNodeIndexConfiguration( indexName, key, value );
    }

    @Override
    public String relationshipLegacyIndexSetConfiguration( KernelStatement statement, String indexName, String key,
            String value ) throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.setRelationshipIndexConfiguration( indexName, key, value );
    }

    @Override
    public String nodeLegacyIndexRemoveConfiguration( KernelStatement statement, String indexName, String key )
            throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.removeNodeIndexConfiguration( indexName, key );
    }

    @Override
    public String relationshipLegacyIndexRemoveConfiguration( KernelStatement statement, String indexName, String key )
            throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.removeRelationshipIndexConfiguration( indexName, key );
    }

    @Override
    public Map<String, String> nodeLegacyIndexGetConfiguration( KernelStatement statement, String indexName )
            throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.getNodeIndexConfiguration( indexName );
    }

    @Override
    public Map<String, String> relationshipLegacyIndexGetConfiguration( KernelStatement statement, String indexName )
            throws LegacyIndexNotFoundKernelException
    {
        return legacyIndexStore.getRelationshipIndexConfiguration( indexName );
    }

    @Override
    public String[] nodeLegacyIndexesGetAll( KernelStatement statement )
    {
        return legacyIndexStore.getAllNodeIndexNames();
    }

    @Override
    public String[] relationshipLegacyIndexesGetAll( KernelStatement statement )
    {
        return legacyIndexStore.getAllRelationshipIndexNames();
    }
    // </Legacy index>

    private PrimitiveIntIterator getPropertyKeys( EntityCursor entityCursor )
    {
        PrimitiveIntStack keys = new PrimitiveIntStack( KEY_LIST_INITIAL_SIZE );
        try ( PropertyCursor properties = entityCursor.properties() )
        {
            while ( properties.next() )
            {
                keys.push( properties.propertyKeyId() );
            }
        }

        return keys.iterator();
    }

    private boolean hasProperty( EntityCursor entityCursor, EntityType type, long nodeId, int propertyKeyId )
            throws EntityNotFoundException
    {
        if ( !entityCursor.next() )
        {
            throw new EntityNotFoundException( type, nodeId );
        }

        try ( PropertyCursor cursor = entityCursor.properties() )
        {
            return cursor.seek( propertyKeyId );
        }
    }

    private Object getProperty( EntityCursor entityCursor,
            EntityType type,
            long id,
            int propertyKeyId ) throws EntityNotFoundException

    {
        if ( !entityCursor.next() )
        {
            throw new EntityNotFoundException( type, id );
        }

        try ( PropertyCursor cursor = entityCursor.properties() )
        {
            if ( cursor.seek( propertyKeyId ) )
            {
                return cursor.value();
            }
        }

        return null;
    }

    private PrimitiveIntCollection getLabels( NodeCursor node )
    {
        PrimitiveIntStack labelIds = new PrimitiveIntStack();
        try ( LabelCursor labels = node.labels() )
        {
            while ( labels.next() )
            {
                labelIds.push( labels.getLabel() );
            }
        }
        return labelIds;
    }
}
