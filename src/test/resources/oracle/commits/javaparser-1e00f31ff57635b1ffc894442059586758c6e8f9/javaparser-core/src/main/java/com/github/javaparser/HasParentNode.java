/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2024 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package com.github.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.Observable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * An object that can have a parent node.
 */
public interface HasParentNode<T> extends Observable {

    /**
     * Returns true if the parent has a parent
     */
    default boolean hasParentNode() {
        return getParentNode().isPresent();
    }

    /**
     * Returns the parent node, or {@code Optional.empty} if no parent is set.
     */
    Optional<Node> getParentNode();

    /**
     * Sets the parent node.
     *
     * @param parentNode the parent node, or {@code null} to set no parent.
     * @return {@code this}
     */
    T setParentNode(Node parentNode);

    /**
     * Returns the parent node from the perspective of the children of this node.
     * <p>
     * That is, this method returns {@code this} for everything except {@code NodeList}. A {@code NodeList} returns its
     * parent node instead. This is because a {@code NodeList} sets the parent of all its children to its own parent
     * node (see {@link com.github.javaparser.ast.NodeList} for details).
     */
    Node getParentNodeForChildren();

    /**
     * Walks the parents of this node and returns the first node of type {@code type}, or {@code empty()} if none is
     * found. The given type may also be an interface type, such as one of the {@code NodeWith...} interface types.
     */
    default <N> Optional<N> findAncestor(Class<N>... types) {
        return findAncestor(x -> true, types);
    }

    /**
     * Walks the parents of this node and returns the first node of type {@code type} that matches {@code predicate}, or
     * {@code empty()} if none is found. The given type may also be an interface type, such as one of the
     * {@code NodeWith...} interface types.
     * @deprecated
     * This method is no longer acceptable to find ancestor.
     * <p> Use {@link findAncestor(Predicate, Class)} instead.
     */
    @Deprecated
    default <N> Optional<N> findAncestor(Class<N> type, Predicate<N> predicate) {
        return findAncestor(predicate, type);
    }

    /**
     * Walks the parents of this node and returns the first node that matches one of types {@code types}, or
     * {@code empty()} if none is found. The given type may also be an interface type, such as one of the
     * {@code NodeWith...} interface types.
     * @param <N>
     */
    default <N> Optional<N> findAncestor(Predicate<N> predicate, Class<N>... types) {
        if (!hasParentNode())
            return Optional.empty();
        Node parent = getParentNode().get();
        Optional<Class<N>> oType = Arrays.stream(types).filter(type -> type.isAssignableFrom(parent.getClass()) && predicate.test(type.cast(parent))).findFirst();
        if (oType.isPresent()) {
            return Optional.of(oType.get().cast(parent));
        }
        return parent.findAncestor(predicate, types);
    }

    /**
     * Determines whether this {@code HasParentNode} node is a descendant of the given node. A node is <i>not</i> a
     * descendant of itself.
     *
     * @param ancestor the node for which to determine whether it has this node as an ancestor.
     * @return {@code true} if this node is a descendant of the given node, and {@code false} otherwise.
     * @see Node#isAncestorOf(Node)
     */
    default boolean isDescendantOf(Node ancestor) {
        return findAncestor(n -> n == ancestor, Node.class).isPresent();
    }
}
