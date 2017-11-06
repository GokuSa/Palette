package beijing.hanhua.sketchpad.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * author:
 * 时间:2017/11/3
 * qq:1220289215
 * 类描述：画板工具栏的多页适配器，将画板工具分类按页放置
 */

public class PaletteToolsPagerAdapter extends PagerAdapter {

    private List<View> mViews ;

    public PaletteToolsPagerAdapter(List<View> views) {
        mViews = views;
    }

    public void update(List<View> views, List<String> titles) {
        mViews.clear();
        mViews.addAll(views);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view==object;
    }

   /* @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }*/

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mViews.get(position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }



}
