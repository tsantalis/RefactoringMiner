package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.astuetz.PagerSlidingTabStrip;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.RepeatableImageKey;
import org.thoughtcrime.securesms.components.RepeatableImageKey.KeyEventListener;
import org.thoughtcrime.securesms.components.emoji.EmojiPageView.EmojiSelectionListener;
import org.thoughtcrime.securesms.util.ResUtil;

import java.util.LinkedList;
import java.util.List;

public class EmojiDrawer extends LinearLayoutCompat {
  private static final KeyEvent DELETE_KEY_EVENT = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);

  private LinearLayout         container;
  private ViewPager            pager;
  private List<EmojiPageModel> models;
  private PagerSlidingTabStrip strip;
  private RecentEmojiPageModel recentModel;
  private EmojiEventListener   listener;

  public EmojiDrawer(Context context) {
    this(context, null);
  }

  public EmojiDrawer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EmojiDrawer(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    final View v = LayoutInflater.from(getContext()).inflate(R.layout.emoji_drawer, this, true);
    initializeResources(v);
    initializePageModels();
    initializeEmojiGrid();
  }

  public void setEmojiEventListener(EmojiEventListener listener) {
    this.listener = listener;
  }

  private void initializeResources(View v) {
    Log.w("EmojiDrawer", "initializeResources()");
    this.container = (LinearLayout)         v.findViewById(R.id.container);
    this.pager     = (ViewPager)            v.findViewById(R.id.emoji_pager);
    this.strip     = (PagerSlidingTabStrip) v.findViewById(R.id.tabs);

    RepeatableImageKey backspace = (RepeatableImageKey)v.findViewById(R.id.backspace);
    backspace.setOnKeyEventListener(new KeyEventListener() {
      @Override public void onKeyEvent() {
        if (listener != null) listener.onKeyEvent(DELETE_KEY_EVENT);
      }
    });
  }

  public boolean isOpen() {
    return container.getVisibility() == View.VISIBLE;
  }

  private void initializeEmojiGrid() {
    pager.setAdapter(new EmojiPagerAdapter(getContext(),
                                           models,
                                           new EmojiSelectionListener() {
                                             @Override public void onEmojiSelected(String emoji) {
                                               recentModel.onCodePointSelected(emoji);
                                               if (listener != null) listener.onEmojiSelected(emoji);
                                             }
                                           }));

    if (recentModel.getEmoji().length == 0) {
      pager.setCurrentItem(1);
    }
    strip.setViewPager(pager);
  }

  private void initializePageModels() {
    this.models = new LinkedList<>();
    this.recentModel = new RecentEmojiPageModel(getContext());
    this.models.add(recentModel);
    this.models.addAll(EmojiPages.PAGES);
  }

  public static class EmojiPagerAdapter extends PagerAdapter
      implements PagerSlidingTabStrip.CustomTabProvider
  {
    private Context                context;
    private List<EmojiPageModel>   pages;
    private EmojiSelectionListener listener;

    public EmojiPagerAdapter(@NonNull Context context,
                             @NonNull List<EmojiPageModel> pages,
                             @Nullable EmojiSelectionListener listener)
    {
      super();
      this.context  = context;
      this.pages    = pages;
      this.listener = listener;
    }

    @Override
    public int getCount() {
      return pages.size();
    }

    @Override public Object instantiateItem(ViewGroup container, int position) {
      EmojiPageView page = new EmojiPageView(context);
      page.setModel(pages.get(position));
      page.setEmojiSelectedListener(listener);
      container.addView(page);
      return page;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
      container.removeView((View)object);
    }

    @Override public void setPrimaryItem(ViewGroup container, int position, Object object) {
      EmojiPageView current = (EmojiPageView) object;
      current.onSelected();
      super.setPrimaryItem(container, position, object);
    }

    @Override public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }

    @Override public View getCustomTabView(ViewGroup viewGroup, int i) {
      ImageView image = new ImageView(context);
      image.setScaleType(ScaleType.CENTER_INSIDE);
      image.setImageResource(ResUtil.getDrawableRes(context, pages.get(i).getIconAttr()));
      return image;
    }
  }

  public interface EmojiEventListener extends EmojiSelectionListener {
    void onKeyEvent(KeyEvent keyEvent);
  }
}
