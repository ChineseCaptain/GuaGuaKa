package com.uu.guaguaka;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * 刮刮卡View
 * Created by UU on 2017/4/11.
 */
public class GuaGuaView extends View {

    private Paint mPrizeTextPaint;//奖区的文字画笔
    private Paint mCoverPaint;//刮奖层的画笔
    private int mPrizeTextSize = 30;//奖区字体大小
    private int mPrizeTextColor = Color.BLACK;//奖区字体颜色
    private String mPrizeContent = "谢谢惠顾";//奖区内容
    private int mCoverColor = Color.BLACK;//刮涂层颜色
    private Rect mTextBound;
    /**
     * 刮涂层的画布
     * 因为要跟随用户的触摸路径
     */
    private Canvas mCoverCanvas;
    /**
     * 刮涂层最终的bitmap
     * 包括原本的涂层，以及用户touch轨迹
     */
    private Bitmap mBitmap;
    private Path mTouchPath;//用户的触摸轨迹，在每次重绘时用到
    private float mLastX;
    private float mLastY;

    private volatile boolean isCompleted = false;//是否已经完成，共享内存，保持多线程的可见性
    OnCompletedListener mCompletedListener;//绘制完成后的监听事件

    public interface OnCompletedListener {
        void onCompleted();
    }

    public GuaGuaView(Context context) {
        this(context, null);
    }

    public GuaGuaView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GuaGuaView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //获取自定义的属性值
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.GuaGuaView);
        mPrizeContent = a.getString(R.styleable.GuaGuaView_text);
        mPrizeTextColor = a.getColor(R.styleable.GuaGuaView_text_color, Color.BLACK);
        mPrizeTextSize = a.getDimensionPixelSize(R.styleable.GuaGuaView_text_size,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                22, getResources().getDisplayMetrics()));
        a.recycle();
        //初始化操作
        init();
    }

    private void init(){
        //初始化奖区文字画笔
        mPrizeTextPaint = new Paint();
        //初始化挂图层画笔
        mCoverPaint = new Paint();
        mCoverPaint.setColor(Color.parseColor("#c0c0c0"));
        mCoverPaint.setAntiAlias(true);
        mCoverPaint.setDither(true);
        mCoverPaint.setStrokeJoin(Paint.Join.ROUND);
        mCoverPaint.setStrokeCap(Paint.Cap.ROUND);
        mCoverPaint.setStyle(Paint.Style.FILL);
        mCoverPaint.setStrokeWidth(20);
        //计算奖区文字区域的大小，用于后面计算奖区文字起始点的坐标
        mPrizeTextPaint.setStyle(Paint.Style.FILL);
        mPrizeTextPaint.setColor(mPrizeTextColor);
        mPrizeTextPaint.setTextSize(mPrizeTextSize);
        mTextBound = new Rect();
        mPrizeTextPaint.getTextBounds(mPrizeContent, 0, mPrizeContent.length(), mTextBound);
        //初始化touch的path
        mTouchPath = new Path();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        //初始化刮涂层的画布,并将刮涂层的内容全部保存到bitmap对象中。
        //最后在onDraw中调用控件的canvas去绘制这个bitmap，从而实现刮奖的效果。
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCoverCanvas = new Canvas(mBitmap);
        mCoverCanvas.drawColor(Color.parseColor("#c0c0c0"));//先draw src，后面会draw dist
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制奖区内容，这里要绘制在整个View的正中间.
        canvas.drawText(mPrizeContent, getWidth() / 2 - mTextBound.width() / 2,
                getHeight() / 2 + mTextBound.height() / 2, mPrizeTextPaint);
        //如果没有刮完的话，需要绘制用户touch的轨迹，以及刮涂层
        if (!isCompleted) {
            drawPath();
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mTouchPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                mTouchPath.lineTo(event.getX(), event.getY());
                float dx = Math.abs(x - mLastX);
                float dy = Math.abs(y - mLastY);
                if (dx > 3 || dy > 3) {
                    mTouchPath.lineTo(x, y);
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                //计算已经刮完的像素
                if (!isCompleted)
                    new Thread(mRunnable).start();
                break;
        }
        if (!isCompleted)
            invalidate();
        return true;//消费掉touch事件
    }

    /**
     * 绘制用户手指刮过的路径
     */
    private void drawPath() {
        mCoverPaint.setStyle(Paint.Style.STROKE);
        mCoverPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        mCoverCanvas.drawPath(mTouchPath, mCoverPaint);
    }

    private Runnable mRunnable = new Runnable()
    {
        @Override
        public void run() {
            int w = getWidth();
            int h = getHeight();

            float wipeArea = 0;
            float totalArea = w * h;
            Bitmap bitmap = mBitmap;
            int[] mPixels = new int[w * h];

            bitmap.getPixels(mPixels, 0, w, 0, 0, w, h);
            //计算被擦除的区域（也就是像素值为0）的像素数之和
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int index = i + j * w;
                    if (mPixels[index] == 0) {
                        wipeArea++;
                    }
                }
            }
            //计算擦除的像素数与总像素数的百分比
            if (wipeArea > 0 && totalArea > 0) {
                int percent = (int) (wipeArea * 100 / totalArea);
                if (percent > 60) {
                    isCompleted = true;
                    postInvalidate();
                }
            }
        }
    };

    public int getmPrizeTextSize() {
        return mPrizeTextSize;
    }

    public void setmPrizeTextSize(int mPrizeTextSize) {
        this.mPrizeTextSize = mPrizeTextSize;
        mPrizeTextPaint.setTextSize(mPrizeTextSize);
        //重新计算文字区域的大小
        mPrizeTextPaint.getTextBounds(mPrizeContent, 0, mPrizeContent.length(), mTextBound);
    }

    public int getmPrizeTextColor() {
        return mPrizeTextColor;
    }

    public void setmPrizeTextColor(int mPrizeTextColor) {
        this.mPrizeTextColor = mPrizeTextColor;
        mPrizeTextPaint.setColor(mPrizeTextColor);
    }

    public String getmPrizeContent() {
        return mPrizeContent;
    }

    public void setmPrizeContent(String mPrizeContent) {
        this.mPrizeContent = mPrizeContent;
        //重新计算文字区域的大小
        mPrizeTextPaint.getTextBounds(mPrizeContent, 0, mPrizeContent.length(), mTextBound);
        //重绘
        invalidate();
    }

    public int getmCoverColor() {
        return mCoverColor;
    }

    public void setmCoverColor(int mCoverColor) {
        this.mCoverColor = mCoverColor;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void reset() {
        isCompleted = false;
        mTouchPath.reset();
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mCoverCanvas = new Canvas(mBitmap);
        mCoverCanvas.drawColor(Color.parseColor("#c0c0c0"));//先draw src，后面会draw dist
    }
}
