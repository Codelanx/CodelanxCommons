package com.codelanx.commons.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Implicitly sorted collection, which will remain sorted upon insertion.
 * Note this breaks some contracts of {@link java.util.List List} (no insertion order)
 *
 * If no comparator is provided and no elements within the list implement
 * {@link Comparable}, then the list is sorted by order of their hashcodes
 *
 * Due to the nature of determining comparability, this list type should almost never
 * be rawtyped without an explicit comparator
 *
 * @since 0.3.2
 * @author 1Rogue
 * @version 0.3.2
 *
 * @param <E> The type of the {@link java.util.List List}
 */
//TODO: Clonable?
public class ImplicitSortedList<E> extends ArrayList<E> {

    //null if natural sorting, otherwise defined sort
    private final Comparator<? super E> comparator;

    public ImplicitSortedList(int capacity) {
        super(capacity);
        this.comparator = null;
    }

    public ImplicitSortedList(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    public ImplicitSortedList(Collection<? extends E> collection) {
        super(collection);
        this.comparator = null;
        this.sort();
    }

    public ImplicitSortedList(Collection<? extends E> collection, Comparator<? super E> comparator) {
        super(collection);
        this.comparator = comparator;
        this.sort();
    }

    protected final void sort() {
        Comparator<? super E> comp = null;
        if (this.comparator == null) {
            if (this.size() > 0) {
                E elem = this.iterator().next();
                if (Comparable.class.isAssignableFrom(elem.getClass())) {
                    comp = (o1, o2) -> ((Comparable<? super E>) o1).compareTo(o2);
                }
            } else {
                comp = (o1, o2) -> Integer.compare(o1.hashCode(), o2.hashCode());
            }
        } else {
            comp = this.comparator;
        }
        Collections.sort(this, comp);
    }

    @Override
    public void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " utilizes a comparator on struction");
    }

    @Override
    public boolean add(E e) {
        boolean back = super.add(e);
        this.sort();
        return back;
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("Cannot insert into a specific index, use #add(E)");
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean back = super.addAll(c);
        this.sort();
        return back;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("Cannot insert into a specific index, use #addAll(Collection)");
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("Cannot set a specific index, use #remove(index) and #add(E)");
    }

    @Override
    public ImplicitSortedList<E> subList(int fromIndex, int toIndex) {
        List<E> back = super.subList(fromIndex, toIndex);
        return new ImplicitSortedList<>(back, this.comparator);
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        super.replaceAll(operator);
        this.sort();
    }

}
