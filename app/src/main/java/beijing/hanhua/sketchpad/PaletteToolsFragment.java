package beijing.hanhua.sketchpad;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import beijing.hanhua.sketchpad.adapter.PaletteToolsPagerAdapter;
import beijing.hanhua.sketchpad.customview.SquareImageView;
import beijing.hanhua.sketchpad.databinding.FragmentPaletteToolsBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintColorBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintTextBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintTypeBinding;
import beijing.hanhua.sketchpad.databinding.LayoutTabItemBinding;
import beijing.hanhua.sketchpad.util.Common;

/**
 * 管理 画板工具视图
 * 实现点击 与 Actviity的通信
 */
public class PaletteToolsFragment extends Fragment {
    private static final String TAG = PaletteToolsFragment.class.getSimpleName();
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private FragmentPaletteToolsBinding mBinding;
    /**
     * 标题icon资源 -1表示无
     */
    private int[] mTabIconResIds = {R.drawable.bg_draw_path, -1, android.R.color.holo_blue_bright};
    /**
     * tabs的标题
     */
    private String[] mTabTitles = {"画笔", "预设文字", "调色板"};
    /**
     * 画笔类型的提示文字
     */
    private String[] mPaintTypeText = {"自由画笔", "直线", "画圆"};
    /**
     * 画笔icon
     */
    private int[] mPaintTypeIcon = {R.drawable.bg_draw_path, R.drawable.bg_draw_line, R.drawable.bg_draw_oval};
    /**
     * 画笔颜色
     */
    private int[] mPaintColors = {R.color.palette_blue, R.color.palette_green, R.color.palette_cyan, R.color.palette_red,
            R.color.palette_pink, R.color.palette_orange, R.color.palette_black, R.color.palette_yellow};
    /**
     * 能绘制的文字
     */
    private String[] mPaintText = {"神州视翰", "shine", "YES", "OK"};
    /**
     * ViewPager 视图的集合
     */
    List<View> mViewPagerContentList = new ArrayList<>();

    public PaletteToolsFragment() {
        // Required empty public constructor
    }

    public static PaletteToolsFragment newInstance() {
        Bundle args = new Bundle();
        PaletteToolsFragment fragment = new PaletteToolsFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_palette_tools, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViewPager();
        setUpTabLayout();
    }

    /**
     * 初始化ViewPager
     * 含有三个页面  画板类型 预设文字 调色板
     */
    private void setUpViewPager() {
        mViewPager = mBinding.viewpager;
        //初始化需要的视图
        loadPaintTypeDynamic();
        loadPaintTextDynamic();
        loadPaintColorDynamic();

        PaletteToolsPagerAdapter adapter = new PaletteToolsPagerAdapter(mViewPagerContentList);
        mViewPager.setAdapter(adapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.addOnPageChangeListener(mPageChangeListener);
    }


    /**
     * 动态加载画笔类型视图
     * 目前有自由画笔 直线 花园三种类型
     */
    private void loadPaintTypeDynamic() {
        LayoutPaintTypeBinding paintTypeBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.layout_paint_type, null, false);
        for (int i = 0, paintTypeSize = mPaintTypeText.length; i < paintTypeSize; i++) {
            TextView textView = new TextView(getActivity());
            textView.setText(mPaintTypeText[i]);
            textView.setTextSize(Common.sp2px(getActivity(), 12));
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(getResources().getColor(android.R.color.white));
            textView.setBackground(getResources().getDrawable(R.drawable.selector_palette_text));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Common.dp2px(getActivity(), 50));
//            设置左边距
            textView.setPadding(24, 0, 0, 0);
//            设置textView  的Drawable
            Drawable drawable = getResources().getDrawable(mPaintTypeIcon[i]);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            textView.setCompoundDrawables(drawable, null, null, null);
//            textView.setCompoundDrawablePadding(20);
            textView.setFocusable(true);
            textView.setOnClickListener(mDrawPaintTypeClickListener);
            textView.setTag(i);
            paintTypeBinding.llPaintType.addView(textView, layoutParams);
        }
        mViewPagerContentList.add(paintTypeBinding.getRoot());

    }

    /**
     * 动态加载要绘制的文字
     */
    private void loadPaintTextDynamic() {
        LayoutPaintTextBinding paintTextBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.layout_paint_text, null, false);
//        paintTextBinding.setPaletteTools(this);
        for (int i = 0, len = mPaintText.length; i < len; i++) {
            TextView textView = new TextView(getActivity());
            textView.setText(mPaintText[i]);
            textView.setTextColor(getResources().getColor(android.R.color.white));
            textView.setTextSize(Common.sp2px(getActivity(), 12));
            textView.setGravity(Gravity.CENTER);
            textView.setBackground(getResources().getDrawable(R.drawable.selector_palette_text));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Common.dp2px(getActivity(), 50));
            textView.setFocusable(true);
            textView.setTag(i);
            textView.setOnClickListener(mPaintTextOnClickListner);
            paintTextBinding.llPaintText.addView(textView, layoutParams);
        }

        mViewPagerContentList.add(paintTextBinding.getRoot());


    }

    /**
     * 动态加载画笔颜色
     */
    private void loadPaintColorDynamic() {
        LayoutPaintColorBinding paintColorBinding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.layout_paint_color, null, false);
//        动态加载调色板颜色
        int margin = 12;
        for (int i = 0; i < mPaintColors.length; i++) {
//            宽为0，
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(new ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT));
            layoutParams.setMargins(margin, margin, margin, margin);
//            行列的位置，列数为4
            GridLayout.Spec rowSpec = GridLayout.spec(i / 4);
//            weight为1
            GridLayout.Spec colSpec = GridLayout.spec(i % 4, 1.0f);
//            正方形图片
            SquareImageView imageView = new SquareImageView(getActivity());
            imageView.setImageResource(mPaintColors[i]);
            layoutParams.rowSpec = rowSpec;
            layoutParams.columnSpec = colSpec;
//            设置tag 点击时使用
            imageView.setTag(i);
            imageView.setOnClickListener(mPaintingColorListner);
            paintColorBinding.glPaintColorContainer.addView(imageView, layoutParams);
        }
        mViewPagerContentList.add(paintColorBinding.getRoot());

    }

    /**
     * 初始化Tabs，并关联ViewPager
     */
    private void setUpTabLayout() {
        mTabLayout = mBinding.tabs;
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        mTabLayout.setSmoothScrollingEnabled(true);
        mTabLayout.setupWithViewPager(mViewPager);

//        使用自定义的视图作为标题，因为需要动态修改icon
        for (int i = 0, tabCount = mTabLayout.getTabCount(); i < tabCount; i++) {
            TabLayout.Tab tabAt = mTabLayout.getTabAt(i);
            View tabCustomView = getTabCustomView(mTabIconResIds[i], mTabTitles[i]);
            if (tabAt != null) {
                tabAt.setCustomView(tabCustomView);
            }
        }
    }

    private View.OnClickListener mDrawPaintTypeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            view.requestFocusFromTouch();
            int currentItem = mViewPager.getCurrentItem();
            TabLayout.Tab tabAt = mTabLayout.getTabAt(currentItem);
            if (tabAt == null) {
                return;
            }
            LayoutTabItemBinding binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
            Integer tag = (Integer) view.getTag();
            binding.icon.setImageResource(mPaintTypeIcon[tag]);

        }
    };

    private View.OnClickListener mPaintTextOnClickListner = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int currentItem = mViewPager.getCurrentItem();
            TabLayout.Tab tabAt = mTabLayout.getTabAt(currentItem);
            if (tabAt == null) {
                return;
            }
            view.requestFocusFromTouch();
            Integer tag = (Integer) view.getTag();
        }
    };

    /*
    * 设置画笔类型的点击
    * */
    public void setDrawPaintType(View view) {
        view.requestFocusFromTouch();
        int currentItem = mViewPager.getCurrentItem();
        TabLayout.Tab tabAt = mTabLayout.getTabAt(currentItem);
        if (tabAt == null) {
            return;
        }
        LayoutTabItemBinding binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
        switch (view.getId()) {
            case R.id.tv_drawPath:
                binding.icon.setImageResource(R.drawable.bg_draw_path);
                break;
            case R.id.tv_drawLine:
                binding.icon.setImageResource(R.drawable.bg_draw_line);
                break;
            case R.id.tv_drawOval:
                binding.icon.setImageResource(R.drawable.bg_draw_oval);
                break;
        }
    }

    /*
    *
    * 设置预设文字
    * */
    public void setDrawText(View view) {
        int currentItem = mViewPager.getCurrentItem();
        TabLayout.Tab tabAt = mTabLayout.getTabAt(currentItem);
        if (tabAt == null) {
            return;
        }
        view.requestFocusFromTouch();
//        LayoutTabItemBinding  binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
//        binding.title.setText(String.format("预设文字\n(%s)", ((TextView) view).getText()));
       /* switch (view.getId()) {
            case R.id.tv_text_one:
                break;
            case R.id.tv_text_two:

                break;
        }*/
    }


    /*画板颜色的点击
    * */
    private View.OnClickListener mPaintingColorListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Integer i = (Integer) v.getTag();
            TabLayout.Tab tabAt = mTabLayout.getTabAt(mViewPager.getCurrentItem());
            if (tabAt == null) {
                return;
            }
            LayoutTabItemBinding binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
            binding.icon.setImageResource(mPaintColors[i]);
        }
    };

    /*
    *创建Tabs的自定义标题视图 含一个imageview 和一个TextView
    * */
    public View getTabCustomView(int resId, String title) {
        LayoutTabItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getActivity()), R.layout.layout_tab_item, null, false);
        if (resId > 0) {
            binding.icon.setImageResource(resId);
        }
        binding.title.setText(title);
        binding.getRoot().setTag(binding);
        return binding.getRoot();
    }

    private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
}
