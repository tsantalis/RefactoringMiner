/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/
package fr.inria.atlanmod.neoemf.datastore.estores.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

/**
 * A {@link TransientEStoreImpl} that belongs to a single {@link EObject} owner
 * 
 */
public class OwnedTransientEStoreImpl extends TransientEStoreImpl {

	protected EObject owner;

	public OwnedTransientEStoreImpl(EObject owner) {
		this.owner = owner;
	}

	@Override
	public Object get(InternalEObject eObject, EStructuralFeature feature, int index) {
		assert owner == eObject;
		return super.get(eObject, feature, index);
	}

	@Override
	public Object set(InternalEObject eObject, EStructuralFeature feature, int index, Object value) {
		assert owner == eObject;
		return super.set(eObject, feature, index, value);
	}

	@Override
	public void add(InternalEObject eObject, EStructuralFeature feature, int index, Object value) {
		assert owner == eObject;
		super.add(eObject, feature, index, value);
	}

	@Override
	public Object remove(InternalEObject eObject, EStructuralFeature feature, int index) {
		assert owner == eObject;
		return super.remove(eObject, feature, index);
	}

	@Override
	public Object move(InternalEObject eObject, EStructuralFeature feature, int targetIndex, int sourceIndex) {
		assert owner == eObject;
		return super.move(eObject, feature, targetIndex, sourceIndex);
	}

	@Override
	public void clear(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		super.clear(eObject, feature);
	}

	@Override
	public boolean isSet(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		return super.isSet(eObject, feature);
	}

	@Override
	public void unset(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		super.unset(eObject, feature);
	}

	@Override
	public int size(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		return super.size(eObject, feature);
	}

	@Override
	public int indexOf(InternalEObject eObject, EStructuralFeature feature, Object value) {
		assert owner == eObject;
		return super.indexOf(eObject, feature, value);
	}

	@Override
	public int lastIndexOf(InternalEObject eObject, EStructuralFeature feature, Object value) {
		assert owner == eObject;
		return super.lastIndexOf(eObject, feature, value);
	}

	@Override
	public Object[] toArray(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		return super.toArray(eObject, feature);
	}

	@Override
	public <T> T[] toArray(InternalEObject eObject, EStructuralFeature feature, T[] array) {
		assert owner == eObject;
		return super.toArray(eObject, feature, array);
	}

	@Override
	public boolean isEmpty(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		return super.isEmpty(eObject, feature);
	}

	@Override
	public boolean contains(InternalEObject eObject, EStructuralFeature feature, Object value) {
		assert owner == eObject;
		return super.contains(eObject, feature, value);
	}

	@Override
	public int hashCode(InternalEObject eObject, EStructuralFeature feature) {
		assert owner == eObject;
		return super.hashCode(eObject, feature);
	}

	@Override
	public InternalEObject getContainer(InternalEObject eObject) {
		assert owner == eObject;
		return super.getContainer(eObject);
	}

	@Override
	public EStructuralFeature getContainingFeature(InternalEObject eObject) {
		assert owner == eObject;
		return super.getContainingFeature(eObject);
	}

	@Override
	public EObject create(EClass eClass) {
		return super.create(eClass);
	}
}
