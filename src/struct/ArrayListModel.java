package struct;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.ListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/*
 * Copyright (c) 2002-2008 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Adds {@link javax.swing.ListModel} capabilities to its superclass
 * <code>ArrayList</code>, i. e. allows to observe changes in the content and
 * structure. Useful for lists that are bound to list views, for example JList,
 * JComboBox and JTable.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.7 $
 * 
 * 
 * 
 * @param <E>
 *            the type of the list elements
 */
@SuppressWarnings("serial")
public class ArrayListModel<E> extends ArrayList<E> implements ListModel<E> {

	/**
	 * Instance Creation
	 * 
	 * 
	 */
	public ArrayListModel() {
		this(40);
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the list.
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity is negative
	 */
	public ArrayListModel(int initialCapacity) {
		super(initialCapacity);

	}

	/**
	 * Constructs a list containing the elements of the specified collection, in
	 * the order they are returned by the collection's iterator. The
	 * <code>ArrayListModel</code> instance has an initial capacity of 110% the
	 * size of the specified collection.
	 * 
	 * @param c
	 *            the collection whose elements are to be placed into this list.
	 * @throws NullPointerException
	 *             if the specified collection is {@code null}
	 */
	public ArrayListModel(Collection<? extends E> c) {
		super(c);

	}

	// Overriding Superclass Behavior *****************************************

	/**
	 * Inserts the specified element at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 * 
	 * @param index
	 *            index at which the specified element is to be inserted.
	 * @param element
	 *            element to be inserted.
	 * @throws IndexOutOfBoundsException
	 *             if index is out of range
	 *             <code>(index &lt; 0 || index &gt; size())</code>.
	 */
	@Override
	public void add(int index, E element) {
		super.add(index, element);
		fireIntervalAdded(index, index);
	}

	/**
	 * Appends the specified element to the end of this list.
	 * 
	 * @param e
	 *            element to be appended to this list.
	 * @return {@code true} (as per the general contract of Collection.add).
	 */
	@Override
	public boolean add(E e) {
		int newIndex = size();
		boolean add = super.add(e);
		fireIntervalAdded(newIndex, newIndex);
		return add;
	}

	/**
	 * Inserts all of the elements in the specified Collection into this list,
	 * starting at the specified position. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (increases
	 * their indices). The new elements will appear in the list in the order
	 * that they are returned by the specified Collection's iterator.
	 * 
	 * @param index
	 *            index at which to insert first element from the specified
	 *            collection.
	 * @param c
	 *            elements to be inserted into this list.
	 * @return {@code true} if this list changed as a result of the call.
	 * @throws IndexOutOfBoundsException
	 *             if index out of range <code>(index
	 *          &lt; 0 || index &gt; size())</code>.
	 * @throws NullPointerException
	 *             if the specified Collection is null.
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		boolean changed = super.addAll(index, c);
		if (changed) {
			int lastIndex = index + c.size() - 1;
			fireIntervalAdded(index, lastIndex);
		}
		return changed;
	}

	/**
	 * Appends all of the elements in the specified Collection to the end of
	 * this list, in the order that they are returned by the specified
	 * Collection's Iterator. The behavior of this operation is undefined if the
	 * specified Collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the
	 * specified Collection is this list, and this list is nonempty.)
	 * 
	 * @param c
	 *            the elements to be inserted into this list.
	 * @return {@code true} if this list changed as a result of the call.
	 * @throws NullPointerException
	 *             if the specified collection is null.
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		int firstIndex = size();
		boolean changed = super.addAll(c);
		if (changed) {
			int lastIndex = firstIndex + c.size() - 1;
			fireIntervalAdded(firstIndex, lastIndex);
		}
		return changed;
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns.
	 */
	@Override
	public void clear() {
		if (isEmpty())
			return;

		int oldLastIndex = size() - 1;
		super.clear();
		fireIntervalRemoved(0, oldLastIndex);
	}

	/**
	 * Removes the element at the specified position in this list. Shifts any
	 * subsequent elements to the left (subtracts one from their indices).
	 * 
	 * @param index
	 *            the index of the element to removed.
	 * @return the element that was removed from the list.
	 * @throws IndexOutOfBoundsException
	 *             if index out of range <code>(index
	 *              &lt; 0 || index &gt;= size())</code>.
	 */
	@Override
	public E remove(int index) {
		E removedElement = super.remove(index);
		fireIntervalRemoved(index, index);
		return removedElement;
	}

	/**
	 * Removes a single instance of the specified element from this list, if it
	 * is present (optional operation). More formally, removes an element
	 * <tt>e</tt> such that <tt>(o==null ? e==null :
	 * o.equals(e))</tt>, if the list contains one or more such elements.
	 * Returns <tt>true</tt> if the list contained the specified element (or
	 * equivalently, if the list changed as a result of the call).
	 * <p>
	 * 
	 * This implementation looks for the index of the specified element. If it
	 * finds the element, it removes the element at this index by calling
	 * <code>#remove(int)</code> that fires a ListDataEvent.
	 * 
	 * @param o
	 *            element to be removed from this list, if present.
	 * @return <tt>true</tt> if the list contained the specified element.
	 */
	@Override
	public boolean remove(Object o) {
		int i = this.indexOf(o);
		if (i < 0)
			return true;
		this.fireIntervalRemoved(i, i);
		return remove(i).equals(o);
	}

	/**
	 * Removes from this List all of the elements whose index is between
	 * fromIndex, inclusive and toIndex, exclusive. Shifts any succeeding
	 * elements to the left (reduces their index). This call shortens the list
	 * by <code>(toIndex - fromIndex)</code> elements. (If
	 * <code>toIndex==fromIndex</code>, this operation has no effect.)
	 * 
	 * @param fromIndex
	 *            index of first element to be removed.
	 * @param toIndex
	 *            index after last element to be removed.
	 */
	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		super.removeRange(fromIndex, toIndex);
		fireIntervalRemoved(fromIndex, toIndex - 1);
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 * 
	 * @param index
	 *            index of element to replace.
	 * @param element
	 *            element to be stored at the specified position.
	 * @return the element previously at the specified position.
	 * @throws IndexOutOfBoundsException
	 *             if index out of range
	 *             <code>(index &lt; 0 || index &gt;= size())</code>.
	 */
	@Override
	public E set(int index, E element) {
		E previousElement = super.set(index, element);
		fireContentsChanged(index, index);
		return previousElement;
	}

	// ListModel Field ********************************************************

	/**
	 * Holds the registered ListDataListeners. The list that holds these
	 * listeners is initialized lazily in <code>#getEventListenerList</code>.
	 * 
	 * @see #addListDataListener(ListDataListener)
	 * @see #removeListDataListener(ListDataListener)
	 */
	private EventListenerList listenerList;

	// ListModel Implementation ***********************************************

	/**
	 * Adds a listener to the list that's notified each time a change to the
	 * data model occurs.
	 * 
	 * @param l
	 *            the <code>ListDataListener</code> to be added
	 */
	@Override
	public void addListDataListener(ListDataListener l) {
		getEventListenerList().add(ListDataListener.class, l);
	}

	/**
	 * Removes a listener from the list that's notified each time a change to
	 * the data model occurs.
	 * 
	 * @param l
	 *            the <code>ListDataListener</code> to be removed
	 */
	@Override
	public void removeListDataListener(ListDataListener l) {
		getEventListenerList().remove(ListDataListener.class, l);
	}

	/**
	 * Returns the value at the specified index.
	 * 
	 * @param index
	 *            the requested index
	 * @return the value at <code>index</code>
	 */
	@Override
	public E getElementAt(int index) {
		return get(index);
	}

	/**
	 * Returns the length of the list or 0 if there's no list.
	 * 
	 * @return the length of the list or 0 if there's no list
	 */
	@Override
	public int getSize() {
		return size();
	}

	// Explicit Change Notification *******************************************

	/**
	 * Notifies all registered <code>ListDataListeners</code> that the element
	 * at the specified index has changed. Useful if there's a content change
	 * without any structural change.
	 * <p>
	 * 
	 * This method must be called <em>after</em> the element of the list
	 * changes.
	 * 
	 * @param index
	 *            the index of the element that has changed
	 * 
	 * @see EventListenerList
	 */
	public void fireContentsChanged(int index) {
		fireContentsChanged(index, index);
	}

	// ListModel Helper Code **************************************************

	/**
	 * Returns an array of all the list data listeners registered on this
	 * <code>ArrayListModel</code>.
	 * 
	 * @return all of this model's <code>ListDataListener</code>s, or an empty
	 *         array if no list data listeners are currently registered
	 * 
	 * @see #addListDataListener(ListDataListener)
	 * @see #removeListDataListener(ListDataListener)
	 */
	public ListDataListener[] getListDataListeners() {
		return getEventListenerList().getListeners(ListDataListener.class);
	}

	/**
	 * This method must be called <em>after</em> one or more elements of the
	 * list change. The changed elements are specified by the closed interval
	 * index0, index1 -- the end points are included. Note that index0 need not
	 * be less than or equal to index1.
	 * 
	 * @param index0
	 *            one end of the new interval
	 * @param index1
	 *            the other end of the new interval
	 * @see EventListenerList
	 */
	public void fireContentsChanged(int index0, int index1) {
		Object[] listeners = getEventListenerList().getListenerList();
		ListDataEvent e = null;

		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).contentsChanged(e);
			}
		}
	}

	/**
	 * This method must be called <em>after</em> one or more elements are added
	 * to the model. The new elements are specified by a closed interval index0,
	 * index1 -- the end points are included. Note that index0 need not be less
	 * than or equal to index1.
	 * 
	 * @param index0
	 *            one end of the new interval
	 * @param index1
	 *            the other end of the new interval
	 * @see EventListenerList
	 */
	private void fireIntervalAdded(int index0, int index1) {
		Object[] listeners = getEventListenerList().getListenerList();
		ListDataEvent e = null;

		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).intervalAdded(e);
			}
		}
	}

	/**
	 * This method must be called <em>after</em> one or more elements are
	 * removed from the model. <code>index0</code> and <code>index1</code> are
	 * the end points of the interval that's been removed. Note that
	 * <code>index0</code> need not be less than or equal to <code>index1</code>
	 * .
	 * 
	 * @param index0
	 *            one end of the removed interval, including <code>index0</code>
	 * @param index1
	 *            the other end of the removed interval, including
	 *            <code>index1</code>
	 * @see EventListenerList
	 */
	private void fireIntervalRemoved(int index0, int index1) {
		Object[] listeners = getEventListenerList().getListenerList();
		ListDataEvent e = null;

		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ListDataListener.class) {
				if (e == null) {
					e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
				}
				((ListDataListener) listeners[i + 1]).intervalRemoved(e);
			}
		}
	}

	/**
	 * Lazily initializes and returns the event listener list used to notify
	 * registered listeners.
	 * 
	 * @return the event listener list used to notify listeners
	 */
	private EventListenerList getEventListenerList() {
		if (this.listenerList == null) {
			this.listenerList = new EventListenerList();
		}
		return this.listenerList;
	}

	/**
	 * If the List has Nameable objects stored in it, then this can search the
	 * list for an object with a given name, and return that object
	 * 
	 * @param name
	 *            the name of the object to search the list for
	 * @return the first Nameable object whose name matches the given input in
	 *         this list, if one exists, otherwise null
	 * @edit additional method by Chase Bonifant, not included in Karsten
	 *       Lentzsch's original version
	 */
	public E getByName(String name) {
		for (E o : this)
			if (o instanceof Nameable && ((Nameable) o).getName().equals(name))
				return o;
		return null;
	}
}
