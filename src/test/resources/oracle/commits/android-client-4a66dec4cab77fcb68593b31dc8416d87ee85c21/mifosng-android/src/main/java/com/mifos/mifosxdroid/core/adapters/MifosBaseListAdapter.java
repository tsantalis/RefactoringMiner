/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.core.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author fomenkoo
 */
public abstract class MifosBaseListAdapter<T> extends BaseAdapter {
    private final Object mLock = new Object();

    private List<T> list;
    private Context context;
    private int layoutId;
    private LayoutInflater inflater;

    public MifosBaseListAdapter(Context context, List<T> list, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getLayout();
    }

    /**
     * Adds the specified object at the end of the array.
     *
     * @param object The object to add at the end of the array.
     */
    public void add(T object) {
        synchronized (mLock) {
            list.add(object);
        }
        notifyDataSetChanged();
    }

    /**
     * Adds the specified Collection at the end of the array.
     *
     * @param collection The Collection to add at the end of the array.
     */
    public void addAll(Collection<? extends T> collection) {
        synchronized (mLock) {
            list.addAll(collection);
        }
        notifyDataSetChanged();
    }

    /**
     * Adds the specified items at the end of the array.
     *
     * @param items The items to add at the end of the array.
     */
    public void addAll(T... items) {
        synchronized (mLock) {
            Collections.addAll(list, items);
        }
        notifyDataSetChanged();
    }

    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object The object to insert into the array.
     * @param index  The index at which the object must be inserted.
     */
    public void insert(T object, int index) {
        synchronized (mLock) {
            list.add(index, object);
        }
        notifyDataSetChanged();
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object The object to remove.
     */
    public void remove(T object) {
        synchronized (mLock) {
            list.remove(object);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove all elements from the list.
     */
    public void clear() {
        synchronized (mLock) {
            list.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * Sorts the content of this adapter using the specified comparator.
     *
     * @param comparator The comparator used to sort the objects contained
     *                   in this adapter.
     */
    public void sort(Comparator<? super T> comparator) {
        synchronized (mLock) {
            Collections.sort(list, comparator);
        }
        notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    public int getCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public T getItem(int position) {
        return (list != null && position >= 0 && position < getCount()) ? list.get(position) : null;
    }

    /**
     * Returns the position of the specified item in the array.
     *
     * @param item The item to retrieve the position of.
     * @return The position of the specified item.
     */
    public int getPosition(T item) {
        return list != null ? list.indexOf(item) : -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the context associated with this array adapter. The context is used
     * to create views from the resource passed to the constructor.
     *
     * @return The Context associated with this adapter.
     */
    public Context getContext() {
        return context;
    }

    public Resources getResources() {
        return getContext().getResources();
    }

    public void setList(List<T> list) {
        synchronized (mLock) {
            this.list = list;
        }
        notifyDataSetChanged();
    }

    public List<T> getList() {
        return list;
    }

    public View getLayout() {
        return getInflater().inflate(layoutId, null);
    }

    public LayoutInflater getInflater() {
        if (inflater == null) {
            inflater = LayoutInflater.from(context);
        }
        return inflater;
    }

    /**
     * Handler for button elements in listview
     */
    View.OnClickListener buttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ListView listView = (ListView) v.getParent();
            int position = listView.getPositionForView(v);
            listView.performItemClick(listView.getChildAt(position), position, listView.getItemIdAtPosition(position));
        }
    };

}
