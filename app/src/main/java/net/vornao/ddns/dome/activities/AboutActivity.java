package net.vornao.ddns.dome.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.vornao.ddns.dome.R;

import java.util.Random;

public class AboutActivity extends AppCompatActivity {


    @SuppressLint("ClickableViewAccessibility") //we don't need this for now
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        RelativeLayout relativeLayout = findViewById(R.id.relativeAbout);

        int[] resIDs = new int[] {R.drawable.ic_outline_lightbulb_24, R.drawable.ic_lightbulb};
        Random rnd = new Random();

        relativeLayout.setOnTouchListener((v, event) -> {

            // easter egg
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                ImageView iv = new ImageView(getApplicationContext());
                lp.setMargins(x, y, 0, 0);
                iv.setLayoutParams(lp);
                iv.setImageDrawable(getResources().getDrawable(resIDs[rnd.nextInt(resIDs.length)%2], getTheme()));
                iv.animate()
                        .translationY(getResources().getDisplayMetrics().heightPixels/3f)
                        .setInterpolator(new AccelerateInterpolator())
                        .setInterpolator(new BounceInterpolator())
                        .setDuration(1500);
                ((ViewGroup) v).addView(iv);
            }
            return false;
        });


        // animate view every 1500ms
        TextView[] textViews = new TextView[]{
            findViewById(R.id.apptitle),
            findViewById(R.id.subtitle),
            findViewById(R.id.description0),
            findViewById(R.id.description1),
            findViewById(R.id.description2),
            findViewById(R.id.footer0),
            findViewById(R.id.footer1),
        };

        for(TextView t: textViews) {
            t.setVisibility(View.GONE);
            t.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.textview_animation));
        }

        new Thread(() -> {
            for(TextView t : textViews) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> t.setVisibility(View.VISIBLE));
            }
        }).start();
    }
}
