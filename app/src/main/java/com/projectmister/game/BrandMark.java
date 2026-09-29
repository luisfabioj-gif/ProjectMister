package com.projectmister.game;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.LinearLayout;
/** One small decoded brand asset shared by headers and menus. */
final class BrandMark {
    private static Bitmap logo;
    static ImageView view(Context context,int sizeDp){
        if(logo==null){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeResource(context.getResources(),R.drawable.boss_xi_logo,o);o.inSampleSize=Math.max(1,Integer.highestOneBit(Math.max(o.outWidth,o.outHeight)/128));o.inJustDecodeBounds=false;logo=BitmapFactory.decodeResource(context.getResources(),R.drawable.boss_xi_logo,o);}
        ImageView v=new ImageView(context);v.setImageBitmap(logo);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setContentDescription("BOSS XI");int size=Math.round(sizeDp*context.getResources().getDisplayMetrics().density);v.setLayoutParams(new LinearLayout.LayoutParams(size,size));return v;
    }
}
