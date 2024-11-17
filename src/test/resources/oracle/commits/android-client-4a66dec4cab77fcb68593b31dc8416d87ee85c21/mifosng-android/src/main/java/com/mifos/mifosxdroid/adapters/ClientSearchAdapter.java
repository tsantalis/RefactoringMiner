/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */

package com.mifos.mifosxdroid.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.mifos.mifosxdroid.R;
import com.mifos.mifosxdroid.core.adapters.MifosBaseListAdapter;
import com.mifos.objects.SearchedEntity;

import java.util.List;

/**
 * @author fomenkoo
 */
public class ClientSearchAdapter extends MifosBaseListAdapter<SearchedEntity> {

    private ColorGenerator mColorGenerator = ColorGenerator.MATERIAL;
    private TextDrawable.IBuilder mDrawableBuilder;

    public ClientSearchAdapter(Context context, List<SearchedEntity> list, int layoutId) {
        super(context, list, layoutId);
        mDrawableBuilder = TextDrawable.builder().round();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final SearchedEntity item;

        if (convertView == null) {
            convertView = getLayout();
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        item = getItem(position);
        holder.name.setText(item.getDescription());
        TextDrawable drawable = mDrawableBuilder.build(String.valueOf(item.getEntityName().charAt(0)), mColorGenerator.getColor(item.getEntityName()));
        holder.icon.setImageDrawable(drawable);
        return convertView;
    }

    class ViewHolder {
        private ImageView icon;
        private TextView name;

        public ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            name = (TextView) view.findViewById(R.id.name);
        }
    }
}
