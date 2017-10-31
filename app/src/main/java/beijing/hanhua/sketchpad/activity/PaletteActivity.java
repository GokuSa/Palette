package beijing.hanhua.sketchpad.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import beijing.hanhua.sketchpad.R;
import beijing.hanhua.sketchpad.databinding.ActivityPaletteBinding;

/**
 * todo 使用tabs+ViewPager + Fragment 实现画板控制栏
 * 解决Fragment 之间通信
 */
public class PaletteActivity extends AppCompatActivity {
    private static final String TAG = "PaletteActivity";
    private ActivityPaletteBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_palette);
        mBinding.setIsPaintTypeVisible(false);
        mBinding.palette.changeVideoSource("one");
    }

    public void one(View view) {
        mBinding.palette.changeVideoSource("one");

    }

    public void two(View view) {
        mBinding.palette.changeVideoSource("two");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.palette.exit();
    }


    //绘画类型选项的可见性切换
    public void switchPaintType(View view) {
        mBinding.setIsPaintTypeVisible(!mBinding.getIsPaintTypeVisible());
    }

    /**
     * 设置画笔类型  自由绘制  画直线 画圆
     *
     * @param view
     */
    public void setDrawPaintType(View view) {
        view.requestFocusFromTouch();
        switch (view.getId()) {
            case R.id.tv_drawPath:
                mBinding.palette.setDrawPathMode();
                break;
            case R.id.tv_drawLine:
                mBinding.palette.setDrawLineMode();
                break;
            case R.id.tv_drawOval:
                mBinding.palette.setDrawOvalMode();
                break;
        }
    }

    public void operatePalette(View view) {
        view.requestFocusFromTouch();
        switch (view.getId()) {
            case R.id.tv_clearPalette:
                mBinding.palette.clearPaletteByHand();
                break;
                case R.id.tv_erase:
                mBinding.palette.setEraserMode();
                break;
                case R.id.tv_openPalette:
                break;
        }
    }
}
