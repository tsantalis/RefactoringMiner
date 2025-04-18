package com.joanzapata.android;

import android.widget.GridView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.ViewById;
import com.joanzapata.android.iconify.Iconify;
import com.joanzapata.android.icons.sample.R;

import static com.joanzapata.android.utils.IconUtils.sort;
import static java.lang.String.format;
import static java.util.Arrays.asList;

@EFragment(R.layout.fragment_iconlist)
public class FontAwesomeFragment extends SherlockFragment {

    @ViewById
    protected GridView gridView;

    @AfterViews
    void afterViews() {
        gridView.setAdapter(new QuickAdapter<Iconify.IconValue>(getActivity(), R.layout.item, sort(asList(Iconify.IconValue.values()))) {
            @Override
            protected void convert(BaseAdapterHelper helper, Iconify.IconValue iconValue) {
                String iconName = iconValue.toString().replaceAll("_", "-");
                helper.setText(R.id.iconText, format("{%s}  %s", iconName, iconName));
                Iconify.addIcons((TextView) helper.getView(R.id.iconText));
            }
        });
    }

    @ItemClick
    void gridView(Iconify.IconValue iconValue) {
        IconDialogFragment_.builder()
                .iconValue(iconValue)
                .build()
                .show(getFragmentManager(), IconDialogFragment.class.getSimpleName());
    }

}
