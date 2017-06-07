package beijing.hanhua.sketchpad.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import beijing.hanhua.sketchpad.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_sketchpad).setOnClickListener(this);
        findViewById(R.id.btn_exit).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_sketchpad:
                Intent intent = new Intent(this, SketchpadActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_exit:
                finish();
                break;
        }
    }
}
