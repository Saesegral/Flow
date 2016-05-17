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
public class Wave extends Activity implements OnTouchListener {
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
        int[] uOld;
        int[] uCurrent;
        int[] uNew;
        int[] temp;

        int touchColor = 0x0000ff;
        int touchSpeed = 0x000000;
        int touchRadius = 3;

        int shift = 3; // bit shift scale from screen to bitmap

        float c = 1 / 32;
        float csqr = c * c;
        float h = .00005f;
        float hsqr = h * h;
        float a = csqr * hsqr;

        //equation constants
//        float c;
//        float dt;
//        float ds;
//        float A; // A = 4(ds*c/dt)^2
//        float B; // B = (2-A)
//            c = 1/32;
//            ds = 1;
//            dt = .005f;
//            A = 4 * c * c * ds * ds / dt / dt;
//            B = 2 - A;

//            time = System.nanoTime() / 1.0E09f;

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
            uOld = new int[bitmapWidth * bitmapHeight];
            uCurrent = new int[bitmapWidth * bitmapHeight];
            uNew = new int[bitmapWidth * bitmapHeight];

            //initial conditions
            setInitialConditions();
        }

        private void setInitialConditions() {
            for(int i = 0 ; i < uOld.length ; i++) {
                uOld[i] = 0x000000; // u'
                uCurrent[i] = 0x000000; // u(t)
                uNew[i] = (int) (uCurrent[i] + h * uOld[i]); // u(t+h)
            }
        }

        //Double Slit Experiment
        private void setBoundaryConditions() {
//            for(int i = 0 ; i < bitmapWidth ; i++)
//                if(i < (bitmapWidth >> 1) - 10 || (i > (bitmapWidth >> 1) - 5 && i < (bitmapWidth >> 1) + 5) || i > (bitmapWidth >> 1) + 10)
//                    uNew[(bitmapHeight >> 1) * bitmapWidth + i] = 0;
//            uNew[(bitmapHeight - 1) * (bitmapWidth) + (bitmapWidth >> 1)] = 0x0000ff;
        }

        //approximation to the diffusion equation, finite difference laplacian marching.
        //f(t+1,x,y)=(1/4)( f(t,x+1,y)+f(t,x-1,y)+f(t,x,y+1)+f(t,x,y-1) )
//        private void timeEvolve() {
//            for(int i = bitmapWidth ; i < uNew.length - bitmapWidth ; i++) {
//                uNew[i] = uOld[i] + (vOld[i]>>5) & 0x0000ff;
//                vNew[i] = vOld[i] +
//                        (uOld[i + 1] + uOld[i - 1] + uOld[i + bitmapWidth] + uOld[i - bitmapWidth] >> 2) &0x0000ff;
////
////                vNew[i] = vOld[i] +
////                        (int) Math.ceil((uOld[i + 1] + uOld[i - 1] + uOld[i + bitmapWidth] + uOld[i - bitmapWidth]) / 4f) & 0x0000ff;
////                if((uNew[i] & 0xff0000) == 0x000000)
////                    uNew[i] |= 0x0000ff;
//            }
//        }

//        private void timeEvolve() {
//            for(int i = bitmapWidth ; i < uNew.length - bitmapWidth ; i++) {
//                if((uNew[i] & 0x00ff00) != 0)
//                    uNew[i] = - (uNew[i] >> 8);
//                uNew[i] = (int) (A * laplacian(i) + B * uCurrent[i] + uOld[i]) & 0x0000ff;
//                if(uNew[i] < 0)
//                    uNew[i] = (- uNew[i]) << 8;
//            }
//        }

        private void timeEvolve() {
            for(int i = bitmapWidth ; i < uNew.length - bitmapWidth ; i++) {
//                if((uNew[i] & 0x00ff00) != 0)
//                    uNew[i] = - (uNew[i] >> 8);
                uNew[i] = (int) (2 * uCurrent[i] - uOld[i] + a * laplacian(i));
                if(uNew[i] < 0)
                    uNew[i] = (- uNew[i]) << 8;

                if((uNew[i] & 0x0000ff) > 0)
                    uNew[i] &= 0x0000ff;
                else
                    uNew[i] &= 0x00ff00;
            }
        }

        private int laplacian(int i) {
            return (uCurrent[i + 1] + uCurrent[i - 1] + uCurrent[i + bitmapWidth] + uCurrent[i - bitmapWidth] >> 2);
        }

        private void touchSpots(int x, int y) {
            touchSpot(x, y);
            touchSpot(x, bitmapHeight - y);
            touchSpot(bitmapWidth - x, y);
            touchSpot(bitmapWidth - x, bitmapHeight - y);
        }

        private void touchSpot(int x, int y) {
            if(bitmapWidth * (y - touchRadius) + (x - touchRadius) < 0 || bitmapWidth * (y + touchRadius) + (x + touchRadius) > uNew.length)
                return;

            uNew[bitmapWidth * y + x] = touchColor;
            uNew[bitmapWidth * y + x + 1] = touchColor;
            uNew[bitmapWidth * y + x - 1] = touchColor;
            uNew[bitmapWidth * (y + 1) + x] = touchColor;
            uNew[bitmapWidth * (y - 1) + x] = touchColor;

            uNew[bitmapWidth * y + x + 2] = touchColor;
            uNew[bitmapWidth * y + x - 2] = touchColor;
            uNew[bitmapWidth * (y + 2) + x] = touchColor;
            uNew[bitmapWidth * (y - 2) + x] = touchColor;

            uNew[bitmapWidth * (y + 1) + x + 1] = touchColor;
            uNew[bitmapWidth * (y + 1) + x - 1] = touchColor;
            uNew[bitmapWidth * (y - 1) + x + 1] = touchColor;
            uNew[bitmapWidth * (y - 1) + x - 1] = touchColor;

            uCurrent[bitmapWidth * y + x] = touchSpeed;
            uCurrent[bitmapWidth * y + x + 1] = touchSpeed;
            uCurrent[bitmapWidth * y + x - 1] = touchSpeed;
            uCurrent[bitmapWidth * (y + 1) + x] = touchSpeed;
            uCurrent[bitmapWidth * (y - 1) + x] = touchSpeed;

            uCurrent[bitmapWidth * y + x + 2] = touchSpeed;
            uCurrent[bitmapWidth * y + x - 2] = touchSpeed;
            uCurrent[bitmapWidth * (y + 2) + x] = touchSpeed;
            uCurrent[bitmapWidth * (y - 2) + x] = touchSpeed;

            uCurrent[bitmapWidth * (y + 1) + x + 1] = touchSpeed;
            uCurrent[bitmapWidth * (y + 1) + x - 1] = touchSpeed;
            uCurrent[bitmapWidth * (y - 1) + x + 1] = touchSpeed;
            uCurrent[bitmapWidth * (y - 1) + x - 1] = touchSpeed;
        }

        //Main loop
        protected void onDraw(Canvas canvas) {
            temp = uOld;
            uOld = uCurrent;
            uCurrent = uNew;
            uNew = temp;

            //This is the meat of the program, this single equation here.
            timeEvolve();

            //boundary conditions
            setBoundaryConditions();

            //touch screen
            for(int i = 0 ; i < touched.length ; i++)
                if(touched[i]) //TODO: Prevent based on radius of touch to stop glitches
                    touchSpot(((int) x[i]) >> shift, ((int) y[i]) >> shift);

            bitmap.setPixels(uNew, 0, bitmapWidth, 0, 0, bitmapWidth - 1, bitmapHeight - 1);
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