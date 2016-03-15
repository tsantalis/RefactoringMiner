package br.ufmg.dcc.labsoft.refdetector.model;

import java.util.Collection;

public abstract class Filter<T> {
	
	public abstract boolean accept(T element);
	
	public Filter<T> and(Filter<T> f) {
		return new AndFilter<T>(this, f);
	}

	public Filter<T> or(Filter<T> f) {
		return new OrFilter<T>(this, f);
	}
	
	public static <T> Filter<T> notIn(Collection<T> set) {
		return new NotFilter<T>(new InFilter<T>(set));
	}

	public static <T> Filter<T> isNotEqual(T entity) {
		return new NotFilter<T>(new EqualFilter<T>(entity));
	}

	public static <T> Filter<T> isEqual(T entity) {
		return new EqualFilter<T>(entity);
	}

	public static <T> Filter<T> in(Collection<T> set) {
		return new InFilter<T>(set);
	}
}

class NotFilter<T> extends Filter<T> {
	private final Filter<T> f;
	
	public NotFilter(Filter<T> f) {
		this.f = f;
	}

	@Override
	public boolean accept(T element) {
		return !f.accept(element);
	}
}

class AndFilter<T> extends Filter<T> {
	private final Filter<T> f1;
	private final Filter<T> f2;
	
	public AndFilter(Filter<T> f1, Filter<T> f2) {
		this.f1 = f1;
		this.f2 = f2;
	}

	@Override
	public boolean accept(T element) {
		return f1.accept(element) && f2.accept(element);
	}
}

class OrFilter<T> extends Filter<T> {
	private final Filter<T> f1;
	private final Filter<T> f2;
	
	public OrFilter(Filter<T> f1, Filter<T> f2) {
		this.f1 = f1;
		this.f2 = f2;
	}
	
	@Override
	public boolean accept(T element) {
		return f1.accept(element) || f2.accept(element);
	}
}

class InFilter<T> extends Filter<T> {
	private final Collection<T> elements;
	
	public InFilter(Collection<T> elements) {
		super();
		this.elements = elements;
	}

	@Override
	public boolean accept(T element) {
		return elements.contains(element);
	}
}

class EqualFilter<T> extends Filter<T> {
	private final T entity;
	
	public EqualFilter(T entity) {
		super();
		this.entity = entity;
	}

	@Override
	public boolean accept(T element) {
		return element.equals(entity);
	}
}
