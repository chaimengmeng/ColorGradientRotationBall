package com.example.administrator.ring;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 点击圆环开始旋转，再次点击停止旋转
 *
 * @author Chmm
 */
public class CustomRingView extends View {
    //画笔
    private Paint mPaint;
    //旋转圆的半径
    private float radius;
    //笔刷宽度
    private float strokeWidth;

    //旋转渐变的两个颜色
    private int startColor;
    private int endColor;

    //旋转的角度
    private float angle;

    //旋转的圆的中心坐标
    private float mCenterX;
    private float mCenterY;

    private ValueAnimator animator;

    public CustomRingView(Context context) {
        super(context);
    }

    public CustomRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //获取属性信息
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomRingView);

        radius = typedArray.getFloat(R.styleable.CustomRingView_radius, -1);
        strokeWidth = typedArray.getFloat(R.styleable.CustomRingView_startColor, 10);

        startColor = typedArray.getColor(R.styleable.CustomRingView_startColor, Color.parseColor("#000000"));
        endColor = typedArray.getColor(R.styleable.CustomRingView_endColor, Color.parseColor("#FFFFFF"));

        typedArray.recycle();

        //初始化画笔
        initPaint();
    }

    public CustomRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initPaint() {
        mPaint = new Paint();
        //抗锯齿
        mPaint.setAntiAlias(true);
        // 防抖动
        mPaint.setDither(true);
        // 开启图像过滤，对位图进行滤波处理。
        mPaint.setFilterBitmap(true);
        //空心圆
        mPaint.setStyle(Paint.Style.STROKE);
        //画笔宽度
        mPaint.setStrokeWidth(strokeWidth);
        //设置笔刷样式为圆形
        mPaint.setStrokeCap(Paint.Cap.ROUND);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //设置渐变色,这个渐变色是全局（360°）渐变，不是圆弧的渐变，一定要在onDraw里设置
        int colorSweep[] = {endColor, startColor};
//        int colorSweep[] = {Color.parseColor("#FFFFFF"), Color.parseColor("#7ddaaa")};

        SweepGradient sweepGradient = new SweepGradient(mCenterX, mCenterY, colorSweep, null);
        //按照圆心旋转
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, mCenterX, mCenterY);
        sweepGradient.setLocalMatrix(matrix);

        mPaint.setShader(sweepGradient);

        //通过圆去设置圆弧
        RectF rectF = new RectF();
        rectF.left = mCenterX - radius;
        rectF.right = mCenterX + radius;
        rectF.top = mCenterY - radius;
        rectF.bottom = mCenterY + radius;

        //  angle + 10 是为了有突兀的点的出现
        canvas.drawArc(rectF, angle + 10, 270, false, mPaint);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startAnimation() {

        if (animator != null && animator.isPaused()) {
            animator.resume();
            return;
        } else if (animator != null) {
            animator.pause();
            return;
        }

        animator = ValueAnimator.ofFloat(0, 1.0f);
        //动画时长，让进度条在CountDown时间内正好从0-360走完，
        animator.setDuration(1500);
        animator.setInterpolator(new LinearInterpolator());//匀速
        animator.setRepeatCount(-1);//表示不循环，-1表示无限循环
        //值从0-1.0F 的动画，动画时长为countdownTime，ValueAnimator没有跟任何的控件相关联，那也正好说明ValueAnimator只是对值做动画运算，而不是针对控件的，我们需要监听ValueAnimator的动画过程来自己对控件做操作
        //添加监听器,监听动画过程中值的实时变化(animation.getAnimatedValue()得到的值就是0-1.0)
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                /**
                 * 这里我们已经知道ValueAnimator只是对值做动画运算，而不是针对控件的，因为我们设置的区间值为0-1.0f
                 * 所以animation.getAnimatedValue()得到的值也是在[0.0-1.0]区间，而我们在画进度条弧度时，设置的当前角度为360*currentAngle，
                 * 因此，当我们的区间值变为1.0的时候弧度刚好转了360度
                 */
                angle = 360 * (float) animation.getAnimatedValue();
                //实时刷新view，这样我们的进度条弧度就动起来了
//                invalidate();
                postInvalidateDelayed(10);
            }
        });
        //开启动画
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mCenterX = MeasureSpec.getSize(widthMeasureSpec) / 2;
        mCenterY = MeasureSpec.getSize(heightMeasureSpec) / 2;

        //计算圆的半径,如果已经设置好半径则不进行下一步计算
        if (radius != -1)
            return;

        //如果view 的宽、高不一致，则取小的一方设置半径
        if (getRadius(widthMeasureSpec) > getRadius(heightMeasureSpec)) {
            radius = getRadius(heightMeasureSpec);

        } else {
            radius = getRadius(widthMeasureSpec);
        }
    }

    public float getRadius(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);

        switch (mode) {
            case MeasureSpec.UNSPECIFIED: {//如果没有指定大小，就设置为默认大小,如有padding，则需左右padding设置一样，上下padding设置一样
                radius = size / 2 - getPaddingLeft() - strokeWidth / 2;
                break;
            }
            case MeasureSpec.AT_MOST: {//如果测量模式是最大取值为size

                //我们将大小取最大值,你也可以取其他值
                radius = size / 2 - getPaddingLeft() - strokeWidth / 2;
                break;
            }
            case MeasureSpec.EXACTLY: {//如果是固定的大小，那就不要去改变它
                break;
            }
        }
        return radius;
    }

}
