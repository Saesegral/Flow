package com.kainui.flow;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

@TargetApi(5)
public class Test extends Activity implements OnTouchListener {
    RenderView rv;
    float[] x = new float[10];
    float[] y = new float[10];
    boolean[] touched = new boolean[10];
    int[] id = new int[10];


    class RenderView extends View {
        int bitmapWidth;
        int bitmapHeight;
        int screenWidth;
        int screenHeight;
        Rect bitmapRect;
        Rect screenRect;
        Bitmap bitmap;
        int[] tempOld;
        int[] tempNew;
        int[] hold;

        boolean colorTimerToggle = true;
        boolean widthTimerToggle = true;
        boolean heightTimerToggle = true;
        int colorTimer = 0;
        int widthTimer = 0;
        int heightTimer = 0;
        int timer = 0;

        int a = 0;
        int b = 0;

        int touchColor = 0x00ff00;

        int shift = 2; // bit shift scale from screen to bitmap

        public RenderView(Context context) {
            super(context);
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            screenWidth = display.getWidth();
            screenHeight = display.getHeight();
            bitmapWidth = screenWidth >> shift;
            bitmapHeight = screenHeight >> shift;
            bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.RGB_565);
            bitmapRect = new Rect(0, 0, bitmapWidth, bitmapHeight);
            screenRect = new Rect(0, 0, screenWidth, screenHeight);
            tempOld = new int[bitmapWidth * bitmapHeight];
            tempNew = new int[bitmapWidth * bitmapHeight];
            hold = new int[bitmapWidth * bitmapHeight];

            //initial conditions
            setInitialConditions();
        }

        private void setInitialConditions() {
            for(int i = 0 ; i < tempOld.length ; i++) {
                tempOld[i] = 0x000000;
                tempNew[i] = 0x000000;
            }
        }

        //creates line through center
//        private void setBoundaryConditions() {
//            for(int i = 0 ; i < bitmapWidth ; i += 5)
//                tempNew[(bitmapHeight >> 1) * bitmapWidth + i] = touchColor;
//        }

        private void incTimers() {
            timer++;

//            if(timer % 3 == 0) {
            if(colorTimerToggle)
                colorTimer++;
            else
                colorTimer--;
            if(colorTimer == 0 || colorTimer == 255)
                colorTimerToggle = ! colorTimerToggle;
//            }

            if(widthTimerToggle)
                widthTimer++;
            else
                widthTimer--;
            if(widthTimer == 1 || widthTimer == screenWidth - 1)
                widthTimerToggle = ! widthTimerToggle;

            if(heightTimerToggle)
                heightTimer++;
            else
                heightTimer--;
            if(heightTimer == 1 || heightTimer == screenHeight - 1)
                heightTimerToggle = ! heightTimerToggle;
        }

        private void setBoundaryConditions() {
            int r = 40;
            a = (int) ((bitmapWidth >> 1) + r * Math.cos(Math.PI * widthTimer / 200));
            b = (int) ((bitmapHeight >> 1) + r * Math.sin(Math.PI * heightTimer / 200));

            touchSpot(a, b, r);
//            touchSpot(a + 40, b + 40);
//
//            a = (int) (r * Math.cos(Math.PI * time / 50));
//            b = (int) (r * Math.sin(Math.PI * time / 50));
//            touchSpot(a + time % bitmapWidth, b + time % bitmapHeight);
//            touchSpot(a + (time + 5) % bitmapWidth, - b + (time + 5) % bitmapHeight);

        }

//        private void setBoundaryConditions() {
//            for(int i = 0 ; i < bitmapWidth ; i ++) {
//                tempNew[i] = touchColor;
//                tempNew[(bitmapHeight -1)* bitmapWidth + i] = touchColor;
//            }
//        }

        //approximation to the diffusion equation, finite difference laplacian marching.
        //f(t+1,x,y)=(1/4)( f(t,x+1,y)+f(t,x-1,y)+f(t,x,y+1)+f(t,x,y-1) )
        private void timeEvolve() {
            for(int i = bitmapWidth ; i < tempNew.length - bitmapWidth ; i++) {
                tempNew[i] = laplacian(i) ;
//                tempNew[i] |= (colorTimer << 8) + (255 - colorTimer);
//                tempNew[i] |= 0x0000ff - (tempNew[i] >> 16);
//                if((tempNew[i] & 0xff0000) == 0x000000)
//                    tempNew[i] |= 0x0000ff;
            }
        }

        private int laplacian(int i) {
            return (tempOld[i + 1] + tempOld[i - 1] + tempOld[i + bitmapWidth] + tempOld[i - bitmapWidth] >> 2);
        }

        //draws the pixel at the 4 mirrored locations
        private void drawFour(final int x, final int y, final int color) {
            tempNew[bitmapWidth * y + x] = color;
            tempNew[bitmapWidth * (bitmapHeight - y) + x] = color;
            tempNew[bitmapWidth * y + bitmapWidth - x] = color;
            tempNew[bitmapWidth * (bitmapHeight - y) + bitmapWidth - x] = color;
        }

        private void touchSpot(final int x, final int y, final int r) {
            //protects from out of bounds error
            if(bitmapWidth * (y - r) + (x - r) < 0 || bitmapWidth * (y + r) + (x + r) > tempNew.length)
                return;

//            drawFour(x, y, touchColor);
//            drawFour(x + 1, y, touchColor);
//            drawFour(x - 1, y, touchColor);
//            drawFour(x, y + 1, touchColor);
//            drawFour(x, y - 1, touchColor);


            tempNew[bitmapWidth * y + x] = touchColor;
            tempNew[bitmapWidth * y + x + 1] = touchColor;
            tempNew[bitmapWidth * y + x - 1] = touchColor;
            tempNew[bitmapWidth * (y + 1) + x] = touchColor;
            tempNew[bitmapWidth * (y - 1) + x] = touchColor;

            tempNew[bitmapWidth * y + x + 2] = touchColor;
            tempNew[bitmapWidth * y + x - 2] = touchColor;
            tempNew[bitmapWidth * (y + 2) + x] = touchColor;
            tempNew[bitmapWidth * (y - 2) + x] = touchColor;

            tempNew[bitmapWidth * (y + 1) + x + 1] = touchColor;
            tempNew[bitmapWidth * (y + 1) + x - 1] = touchColor;
            tempNew[bitmapWidth * (y - 1) + x + 1] = touchColor;
            tempNew[bitmapWidth * (y - 1) + x - 1] = touchColor;
        }

        //Main loop
        protected void onDraw(Canvas canvas) {
            hold = tempOld;
            tempOld = tempNew;
            tempNew = hold;

            incTimers();

            //make something so that there's a flow, perhaps circular to go with the diffusion.

            //This is the meat of the program, this single equation here.
            timeEvolve();

            //boundary conditions
            setBoundaryConditions();

            //touch screen
            for(int i = 0 ; i < touched.length ; i++)
                if(touched[i])
                    touchSpot(((int) x[i]) >> shift, ((int) y[i]) >> shift, 5);

            bitmap.setPixels(tempNew, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
            canvas.drawBitmap(bitmap, bitmapRect, screenRect, null);

            invalidate();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(LayoutParams.FLAG_KEEP_SCREEN_ON,
                LayoutParams.FLAG_KEEP_SCREEN_ON);
        rv = new RenderView(this);
        rv.setOnTouchListener(this);

        setContentView(rv);
        for(int i = 0 ; i < 10 ; i++)
            id[i] = - 1;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        int pointerCount = event.getPointerCount();
        for(int i = 0 ; i < 10 ; i++) {
            if(i >= pointerCount) {
                touched[i] = false;
                id[i] = - 1;
                continue;
            }
            if(event.getAction() != MotionEvent.ACTION_MOVE && i != pointerIndex) {
                // if it's an up/down/cancel/out event, mask the id to see if we should process it for this touch point
                continue;
            }
            int pointerId = event.getPointerId(i);
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    touched[i] = true;
                    id[i] = pointerId;
                    x[i] = (int) event.getX(i);
                    y[i] = (int) event.getY(i);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_CANCEL:
                    touched[i] = false;
                    id[i] = - 1;
                    x[i] = (int) event.getX(i);
                    y[i] = (int) event.getY(i);
                    break;

                case MotionEvent.ACTION_MOVE:
                    touched[i] = true;
                    id[i] = pointerId;
                    x[i] = (int) event.getX(i);
                    y[i] = (int) event.getY(i);
                    break;
            }
        }
//        rv.invalidate();
        return true;
    }

}
