/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.joanzapata.android.iconify.Iconify;
import com.mifos.mifosxdroid.R;
import com.mifos.objects.noncore.Document;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by ishankhanna on 02/07/14.
 */
public class DocumentListAdapter extends BaseAdapter {

    LayoutInflater layoutInflater;
    Context context;
    List<Document> documents = new ArrayList<Document>();

    public DocumentListAdapter(Context context, List<Document> documents) {

        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.documents = documents;

    }


    @Override
    public int getCount() {
        return documents.size();
    }

    @Override
    public Document getItem(int i) {
        return documents.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ReusableDocumentViewHolder reusableDocumentViewHolder;

        if (view == null) {

            view = layoutInflater.inflate(R.layout.row_document_list, null);
            reusableDocumentViewHolder = new ReusableDocumentViewHolder(view);
            view.setTag(reusableDocumentViewHolder);
        } else {

            reusableDocumentViewHolder = (ReusableDocumentViewHolder) view.getTag();
        }

        Document document = documents.get(i);

        reusableDocumentViewHolder.tv_doc_name.setText(document.getName());
        reusableDocumentViewHolder.tv_doc_description.setText(document.getDescription()==null?"-":document.getDescription());

        Iconify.IconValue cloudIcon = Iconify.IconValue.fa_download;

        //TODO Implement Local Storage Check to show File Download Info
        //Iconify.IconValue storageIcon = Iconify.IconValue.fa_hdd_o;

        reusableDocumentViewHolder.tv_doc_location_icon.setText(cloudIcon.formattedName());

        Iconify.addIcons(reusableDocumentViewHolder.tv_doc_location_icon);

        return view;
    }

    public static class ReusableDocumentViewHolder {

        @InjectView(R.id.tv_doc_name)
        TextView tv_doc_name;
        @InjectView(R.id.tv_doc_descrption)
        TextView tv_doc_description;
        @InjectView(R.id.tv_doc_location_icon)
        TextView tv_doc_location_icon;

        public ReusableDocumentViewHolder(View view) { ButterKnife.inject(this, view); }

    }
}
