package beijing.hanhua.sketchpad;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import beijing.hanhua.sketchpad.activity.PaletteActivity;
import beijing.hanhua.sketchpad.adapter.PaletteToolsPagerAdapter;
import beijing.hanhua.sketchpad.customview.SquareImageView;
import beijing.hanhua.sketchpad.databinding.FragmentPaletteToolsBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintColorBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintTextBinding;
import beijing.hanhua.sketchpad.databinding.LayoutPaintTypeBinding;
import beijing.hanhua.sketchpad.databinding.LayoutTabItemBinding;
import beijing.hanhua.sketchpad.util.Common;

import static beijing.hanhua.sketchpad.activity.PaletteActivity.sPaintColors;

/**
 * 管理 画板工具视图
 * 写到 画板状态的更新
 */
public class PaletteToolsFragment extends Fragment {
    private static final String TAG = PaletteToolsFragment.class.getSimpleName();
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private FragmentPaletteToolsBinding mBinding;
    /**
     * 默认设置对应的标题icon资源 -1表示无
     * 默认自由画笔 绘制文字无  颜色为蓝色
     */
    private int[] mTabIconResIds = {R.drawable.bg_draw_path, -1, R.color.palette_blue};
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
        mBinding.setPaletteTools(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        setUpCurrentStatus(mPaintTypeIcon[0],mPaintTypeText[0],sPaintColors[0]);
        setUpViewPager();
        setUpTabLayout();
    }

    /**
     * 建立当前画板状态
     * @param paintTypeIconId 画笔类型icon 的资源id
     * @param text 预设文字的 文本
     * @param paintColorIconId  画笔颜色icon的资源id
     */
    private void setUpCurrentStatus(int paintTypeIconId,String text,int paintColorIconId) {
        Drawable paintType=null;
        if (paintTypeIconId > 0) {
            paintType = getResources().getDrawable(paintTypeIconId);
            paintType.setBounds(0, 0, 48, 48);
        }
        Drawable paintColor=null;
        if (paintColorIconId>0) {
            paintColor = getResources().getDrawable(paintColorIconId);
            paintColor.setBounds(0, 0, 48, 48);
        }
        mBinding.tvPaintStatus.setCompoundDrawables(paintType,null,paintColor,null);
        mBinding.tvPaintStatus.setText(text);
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
            textView.setOnClickListener(mPaintTypeClickListener);
//            把选项的索引作为标记，点击事件一次区分点击的位置
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
//            variableNameView.setTextAppearance(context, R.style.VariableName);动态设置样式
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
        int margin = 16;
        for (int i = 0,paintColorLength=sPaintColors.length; i < paintColorLength; i++) {
//            宽为0，
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(new ViewGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT));
            layoutParams.setMargins(margin, margin, margin, margin);
//            行列的位置，列数为4
            GridLayout.Spec rowSpec = GridLayout.spec(i / 4);
//            weight为1
            GridLayout.Spec colSpec = GridLayout.spec(i % 4, 1.0f);
            layoutParams.rowSpec = rowSpec;
            layoutParams.columnSpec = colSpec;
//            正方形图片
            SquareImageView imageView = new SquareImageView(getActivity());
            imageView.setImageResource(sPaintColors[i]);
//            设置tag 点击时使用
            imageView.setTag(i);
            imageView.setOnClickListener(mPaintingColorListner);
            paintColorBinding.glPaintColorContainer.addView(imageView, layoutParams);
        }
//        获取控件大小的正确方式
        mViewPager.post(()->{
            Log.d(TAG, "mViewPager.getWidth():" + mViewPager.getWidth());});
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

    /**
     * 画笔类型的点击事件
     */
    private View.OnClickListener mPaintTypeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            view.requestFocusFromTouch();
            int currentItem = mViewPager.getCurrentItem();
            TabLayout.Tab tabAt = mTabLayout.getTabAt(currentItem);
            if (tabAt == null) {
                return;
            }
            LayoutTabItemBinding binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
            Integer index = (Integer) view.getTag();
//            更新画笔类型icon 告诉用户当前的画笔类型
            binding.icon.setImageResource(mPaintTypeIcon[index]);
//            让Actviity更新画板的画笔类型
            PaletteActivity paletteActivity = (PaletteActivity) getActivity();
            if (paletteActivity != null) {
                paletteActivity.setDrawPaintType(index);
            }
        }
    };

    private View.OnClickListener mPaintTextOnClickListner = view -> {
        view.requestFocusFromTouch();
        Integer index = (Integer) view.getTag();
        //            让Actviity更新画板的画笔文字
        PaletteActivity paletteActivity = (PaletteActivity) getActivity();
        if (paletteActivity != null) {
            paletteActivity.setDrawText(mPaintText[index]);
        }
    };


    /*画板颜色的点击
    * */
    private View.OnClickListener mPaintingColorListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Integer index = (Integer) v.getTag();
            TabLayout.Tab tabAt = mTabLayout.getTabAt(mViewPager.getCurrentItem());
            if (tabAt == null) {
                return;
            }

            LayoutTabItemBinding binding = (LayoutTabItemBinding) tabAt.getCustomView().getTag();
//            设置画笔颜色icon 告知用户当前选择的颜色
            binding.icon.setImageResource(sPaintColors[index]);
            //            让Actviity更新画板的画笔颜色,这里传的是索引 不是具体的颜色
            PaletteActivity paletteActivity = (PaletteActivity) getActivity();
            if (paletteActivity != null) {
                paletteActivity.setPaintColor(index);
            }
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

    /**
     * 对画板的操作
     * @param view
     */
    public void operatePalette(View view) {
        view.requestFocusFromTouch();
        PaletteActivity paletteActivity = (PaletteActivity) getActivity();
        if (paletteActivity == null) {
            return;
        }
        paletteActivity.operatePalette(view.getId());

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
