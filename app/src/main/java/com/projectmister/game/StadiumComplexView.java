package com.projectmister.game;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import android.view.*;
import java.io.InputStream;
import java.util.function.IntConsumer;

/** Five aligned architectural stages, composited per saved component, with tappable plots. */
public final class StadiumComplexView extends View {
    private static final int[] CODES={101,102,103,104,1,2,3,4,5,6,7,8,9,10,11,12};
    private static final String[] NAMES={"North stand","East stand","South stand","West stand","Training centre","Medical centre","Performance gym","Youth academy","Club shop","Bars & concourse","Restaurants","Hospitality","Fan zone","Parking","Media centre","Club museum"};
    private static final float[][] PLOTS={
        {.135f,.035f,.475f,.222f},{.472f,.105f,.625f,.445f},{.112f,.357f,.582f,.565f},{.005f,.105f,.149f,.499f},
        {.697f,.215f,.907f,.333f},{.648f,.388f,.747f,.537f},{.750f,.368f,.854f,.530f},{.859f,.320f,.994f,.540f},
        {.604f,.538f,.735f,.681f},{.423f,.547f,.570f,.685f},{.117f,.555f,.321f,.713f},{.180f,.483f,.414f,.545f},
        {.321f,.553f,.419f,.716f},{.045f,.713f,.371f,.912f},{.801f,.633f,.969f,.828f},{.622f,.677f,.799f,.840f}
    };
    private static final LruCache<Integer,Bitmap> CACHE=new LruCache<Integer,Bitmap>(12*1024*1024){@Override protected int sizeOf(Integer key,Bitmap image){return image.getAllocationByteCount();}};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final int[] levels;
    private final int project,focus;
    private int selected;
    private final IntConsumer action;
    private final ScaleGestureDetector zoomDetector;
    private float zoom=1,panX,panY,downX,downY,lastX,lastY;
    private boolean dragged;

    public StadiumComplexView(Context context,int[] levels,int project,int selected,IntConsumer action){this(context,levels,project,selected,action,0);}
    public StadiumComplexView(Context context,int[] levels,int project,int selected,IntConsumer action,int focus){
        super(context);if(levels.length!=CODES.length)throw new IllegalArgumentException("Incomplete complex");
        this.levels=levels.clone();for(int n:levels)if(n<1||n>5)throw new IllegalArgumentException("Invalid facility level");
        this.project=project;this.selected=selected;this.action=action;this.focus=focus;
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(action!=null);setFocusable(true);
        StringBuilder description=new StringBuilder("Stadium complex. ");
        for(int i=0;i<CODES.length;i++)description.append(NAMES[i]).append(" level ").append(levels[i]).append(CODES[i]==project?", under construction. ":". ");
        setContentDescription(description.toString());
        zoomDetector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector detector){zoom=Math.max(1,Math.min(3,zoom*detector.getScaleFactor()));if(zoom==1){panX=0;panY=0;}invalidate();return true;}});
    }
    public static int[] codes(){return CODES.clone();}
    public void select(int code){selected=code;invalidate();}
    public static String name(int code){int i=index(code);return i<0?"Stadium complex":NAMES[i];}
    private static int index(int code){for(int i=0;i<CODES.length;i++)if(CODES[i]==code)return i;return -1;}
    private Bitmap stage(int level){
        synchronized(CACHE){Bitmap b=CACHE.get(level);if(b!=null&&!b.isRecycled())return b;
            try(InputStream in=getContext().getAssets().open("stadium/complex-level-"+level+".webp")){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.RGB_565;
                b=BitmapFactory.decodeStream(in,null,options);if(b!=null)CACHE.put(level,b);return b;
            }catch(Exception absent){return null;}}
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,resolveSize(focus==0?Math.round(width*853f/1280):Math.round(width*.55f),heightSpec));}
    private RectF plot(int i){float[] p=PLOTS[i];return new RectF(p[0]*getWidth(),p[1]*getHeight(),p[2]*getWidth(),p[3]*getHeight());}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);Bitmap base=stage(1);if(base==null){canvas.drawColor(0xff17252b);return;}
        int f=index(focus);
        if(f>=0){float[] p=PLOTS[f];float cx=(p[0]+p[2])/2*base.getWidth(),cy=(p[1]+p[3])/2*base.getHeight(),w=(p[2]-p[0]+.06f)*base.getWidth(),h=(p[3]-p[1]+.06f)*base.getHeight(),aspect=getWidth()/(float)Math.max(1,getHeight());if(w/h<aspect)w=h*aspect;else h=w/aspect;int l=Math.max(0,(int)(cx-w/2)),t=Math.max(0,(int)(cy-h/2)),r=Math.min(base.getWidth(),(int)(cx+w/2)),b=Math.min(base.getHeight(),(int)(cy+h/2));Bitmap tile=stage(levels[f]);canvas.drawBitmap(tile==null?base:tile,new Rect(l,t,r,b),new RectF(0,0,getWidth(),getHeight()),paint);return;}
        canvas.save();canvas.translate(getWidth()/2f+panX,getHeight()/2f+panY);canvas.scale(zoom,zoom);canvas.translate(-getWidth()/2f,-getHeight()/2f);
        RectF whole=new RectF(0,0,getWidth(),getHeight());canvas.drawBitmap(base,null,whole,paint);
        // Plot coordinates keep the original pitch/roads fixed while independent upgrades grow.
        for(int i=0;i<CODES.length;i++)if(levels[i]>1){Bitmap image=stage(levels[i]);if(image==null)continue;canvas.save();canvas.clipRect(plot(i));canvas.drawBitmap(image,null,whole,paint);canvas.restore();}
        for(int i=0;i<CODES.length;i++){
            RectF r=plot(i);
            if(CODES[i]==project){paint.setColor(0x55ffc563);canvas.drawRect(r,paint);paint.setColor(0xffedb453);paint.setStrokeWidth(Math.max(1,getWidth()*.002f));canvas.save();canvas.clipRect(r);for(float x=r.left-r.height();x<r.right;x+=getWidth()*.025f)canvas.drawLine(x,r.bottom,x+r.height(),r.top,paint);canvas.restore();}
            if(CODES[i]==selected){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2,getWidth()*.004f));paint.setColor(0xff5bddb0);canvas.drawRoundRect(r,4,4,paint);paint.setStyle(Paint.Style.FILL);}
            float cx=r.centerX(),cy=r.bottom-getHeight()*.011f,rad=Math.max(1.7f,getWidth()*.004f);
            paint.setColor(0xc7111d27);canvas.drawRoundRect(cx-rad*8,cy-rad*2,cx+rad*8,cy+rad*2,rad*2,rad*2,paint);
            for(int n=0;n<5;n++){paint.setColor(n<levels[i]?0xff77f0c6:0xff647078);canvas.drawCircle(cx+(n-2)*rad*2.7f,cy,rad,paint);}
        }
        canvas.restore();
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(action==null)return false;zoomDetector.onTouchEvent(event);
        if(event.getPointerCount()>1){getParent().requestDisallowInterceptTouchEvent(true);dragged=true;return true;}
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=lastX=event.getX();downY=lastY=event.getY();dragged=false;return true;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(event.getX()-downX)+Math.abs(event.getY()-downY)>12*getResources().getDisplayMetrics().density)dragged=true;
                if(zoom>1){getParent().requestDisallowInterceptTouchEvent(true);panX=Math.max(-getWidth()*(zoom-1)/2,Math.min(getWidth()*(zoom-1)/2,panX+event.getX()-lastX));panY=Math.max(-getHeight()*(zoom-1)/2,Math.min(getHeight()*(zoom-1)/2,panY+event.getY()-lastY));invalidate();}
                lastX=event.getX();lastY=event.getY();return true;
            case MotionEvent.ACTION_UP:
                if(!dragged&&!zoomDetector.isInProgress()){
                    float x=(event.getX()-getWidth()/2f-panX)/zoom+getWidth()/2f,y=(event.getY()-getHeight()/2f-panY)/zoom+getHeight()/2f;
                    // Small facilities are tested first where facade plots sit inside a stand.
                    for(int i=CODES.length-1;i>=0;i--)if(plot(i).contains(x,y)){performClick();action.accept(CODES[i]);break;}
                }getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
        }return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
