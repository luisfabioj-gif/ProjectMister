package com.projectmister.game;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.widget.ImageView;
import android.widget.LinearLayout;
/** One small decoded brand asset shared by headers and menus. */
final class BrandMark {
    private static Bitmap logo;
    static ImageView view(Context context,int sizeDp){
        if(logo==null){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeResource(context.getResources(),R.drawable.boss_xi_logo,o);o.inSampleSize=Math.max(1,Integer.highestOneBit(Math.max(o.outWidth,o.outHeight)/128));o.inJustDecodeBounds=false;logo=BitmapFactory.decodeResource(context.getResources(),R.drawable.boss_xi_logo,o);}
        ImageView v=new ImageView(context){
            private final Paint mask=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas){int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);super.onDraw(canvas);mask.setShader(new RadialGradient(getWidth()*.5f,getHeight()*.5f,Math.min(getWidth(),getHeight())*.60f,new int[]{0xffffffff,0xffffffff,0x00ffffff},new float[]{0,.62f,1},Shader.TileMode.CLAMP));mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));canvas.drawRect(0,0,getWidth(),getHeight(),mask);mask.setXfermode(null);canvas.restoreToCount(layer);}
        };v.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);v.setImageBitmap(logo);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setContentDescription("BOSS XI");int size=Math.round(sizeDp*context.getResources().getDisplayMetrics().density);v.setLayoutParams(new LinearLayout.LayoutParams(size,size));return v;
    }
}
