package com.projectmister.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/** Original resolution-independent pitch shared by the match and tactics views. */
final class PitchArt {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    void draw(Canvas c,float w,float h) {
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(15,39,34));c.drawRect(0,0,w,h,paint);
        float left=w*.025f,top=h*.035f,right=w-left,bottom=h-top;
        float pw=right-left,ph=bottom-top;
        for(int i=0;i<12;i++) {
            paint.setColor(i%2==0?Color.rgb(34,91,66):Color.rgb(37,98,71));
            c.drawRect(left+pw*i/12,top,left+pw*(i+1)/12,bottom,paint);
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.2f,h*.004f));
        paint.setColor(Color.argb(185,225,240,227));
        c.drawRect(left,top,right,bottom,paint);c.drawLine(w/2,top,w/2,bottom,paint);
        c.drawCircle(w/2,h/2,ph*.145f,paint);
        c.drawRect(left,top+ph*.21f,left+pw*.16f,bottom-ph*.21f,paint);
        c.drawRect(right-pw*.16f,top+ph*.21f,right,bottom-ph*.21f,paint);
        c.drawRect(left,top+ph*.37f,left+pw*.055f,bottom-ph*.37f,paint);
        c.drawRect(right-pw*.055f,top+ph*.37f,right,bottom-ph*.37f,paint);
        rect.set(left+pw*.11f-ph*.145f,h/2-ph*.145f,left+pw*.11f+ph*.145f,h/2+ph*.145f);
        c.drawArc(rect,-52,104,false,paint);
        rect.set(right-pw*.11f-ph*.145f,h/2-ph*.145f,right-pw*.11f+ph*.145f,h/2+ph*.145f);
        c.drawArc(rect,128,104,false,paint);
        c.drawRect(left-w*.013f,h/2-ph*.09f,left,h/2+ph*.09f,paint);
        c.drawRect(right,h/2-ph*.09f,right+w*.013f,h/2+ph*.09f,paint);
        paint.setStyle(Paint.Style.FILL);
        float spot=Math.max(1.5f,h*.005f);
        c.drawCircle(w/2,h/2,spot,paint);c.drawCircle(left+pw*.11f,h/2,spot,paint);c.drawCircle(right-pw*.11f,h/2,spot,paint);
    }
}
