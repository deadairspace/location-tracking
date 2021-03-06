package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;

/**
 * Created by Sergio on 6/9/17.
 */

public class FloorMap {

    // Layers currently displayed
    private int[] layerID;
    private int currentLitID = 0;
    private ImageView theMap;
    private Context context;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;

    public FloorMap(Context context, Activity activity){
        this.context = context;
        this.theMap = (ImageView) activity.findViewById(R.id.the3dMap);
        paint = new Paint();

        // Save IDs of all drawables
        this.layerID = new int[21];
        this.layerID[0] = this.context.getResources().getIdentifier("base", "drawable", context.getPackageName());
        for (int i = 1; i < 21; i++) {
            this.layerID[i] = this.context.getResources().getIdentifier("c" + i, "drawable", context.getPackageName());
        }

        clear();
        redraw();
    }

    public void clear() {
        bitmap = BitmapFactory.decodeResource(context.getResources(), layerID[currentLitID]).copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);
    }

    public void addParticle(ParticleFilter.Particle particle) {
        int x = particle.getX();
        int y = particle.getY();
        int color = Color.RED | ((int)(0xFF * (1 - particle.getWeight())) << 8);
        paint.setColor(color);
        canvas.drawCircle(x, y, 2, paint);
    }

    public void redraw() {
        theMap.setImageBitmap(bitmap);
        theMap.invalidate();
    }

    public int getMapHeight() {
        return canvas.getHeight();
    }

    public int getMapWidth() {
        return canvas.getWidth();
    }

    public void updateRooms(int litRoom){
        currentLitID = litRoom + 1;     // +1 since the .png files start at 1
    }
}
