package com.projectmister.game;

import android.graphics.*;

/** Original stadium artwork drawn on the same metric perspective as the live play. */
final class StadiumPitch {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private Bitmap backdrop;
    private int cachedW,cachedH;
    private boolean cachedTactics;
    private PitchProjection plane;
    void draw(Canvas canvas,int w,int h,boolean tactics){
        if(w<=0||h<=0)return;
        if(backdrop==null||cachedW!=w||cachedH!=h||cachedTactics!=tactics){
            if(backdrop!=null)backdrop.recycle();cachedW=w;cachedH=h;cachedTactics=tactics;
            backdrop=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);plane=new PitchProjection(w,h,tactics);
            paintStadium(new Canvas(backdrop),w,h,tactics);
        }
        canvas.drawBitmap(backdrop,0,0,null);
    }
    private void polygon(Canvas c,int colour,float... xy){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(colour);path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();c.drawPath(path,p);
    }
    private void patch(Canvas c,int colour,float x1,float y1,float x2,float y2){
        float ax=PitchProjection.mx(x1),ay=PitchProjection.my(y1),bx=PitchProjection.mx(x2),by=PitchProjection.my(y2);
        polygon(c,colour,plane.x(ax,ay),plane.y(ax,ay),plane.x(bx,ay),plane.y(bx,ay),plane.x(bx,by),plane.y(bx,by),plane.x(ax,by),plane.y(ax,by));
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2){
        float ax=PitchProjection.mx(x1),ay=PitchProjection.my(y1),bx=PitchProjection.mx(x2),by=PitchProjection.my(y2);
        c.drawLine(plane.x(ax,ay),plane.y(ax,ay),plane.x(bx,by),plane.y(bx,by),p);
    }
    private void box(Canvas c,float x1,float y1,float x2,float y2){line(c,x1,y1,x2,y1);line(c,x2,y1,x2,y2);line(c,x2,y2,x1,y2);line(c,x1,y2,x1,y1);}
    private void arc(Canvas c,float cx,float cy,float radius,float from,float to){
        path.reset();for(int i=0;i<=72;i++){double a=Math.toRadians(from+(to-from)*i/72f);float x=PitchProjection.mx(cx+radius*(float)Math.cos(a)),y=PitchProjection.my(cy+radius*(float)Math.sin(a));if(i==0)path.moveTo(plane.x(x,y),plane.y(x,y));else path.lineTo(plane.x(x,y),plane.y(x,y));}c.drawPath(path,p);
    }
    private void spot(Canvas c,float x,float y,float radius){float nx=PitchProjection.mx(x),ny=PitchProjection.my(y);p.setStyle(Paint.Style.FILL);c.drawCircle(plane.x(nx,ny),plane.y(nx,ny),radius*plane.scale(ny),p);p.setStyle(Paint.Style.STROKE);}
    private void paintStadium(Canvas c,int w,int h,boolean tactics){
        c.drawColor(0xff17221b);
        float skyline=h*(tactics?.015f:.04f),standBottom=h*(tactics?.135f:.235f);
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,0,standBottom,0xff45514c,0xff141c1b,Shader.TileMode.CLAMP));c.drawRect(0,0,w,standBottom,p);p.setShader(null);
        // Roof trusses and the curved seating bowl, with aisles and individual seats.
        for(int tier=0;tier<3;tier++){
            float top=skyline+tier*(standBottom-skyline)/3f,bottom=top+(standBottom-skyline)/3f;
            polygon(c,0xff242c2b,0,top,w,top,w*.93f,bottom,w*.07f,bottom);
            float spacing=Math.max(3,w/185f);
            p.setStrokeWidth(Math.max(.8f,h*.0018f));
            for(int row=0;row<5;row++){
                float y=top+(bottom-top)*(row+.5f)/5;
                for(int seat=0;seat<185;seat++){
                    float x=seat*spacing;
                    if(seat%23<2||seat%61<2)continue;
                    int light=(seat*37+row*17+tier*29)%29;
                    p.setColor(Color.rgb(54+light,61+light,58+light));c.drawLine(x,y,x+Math.max(1,spacing*.55f),y,p);
                }
            }
            p.setColor(0xff75807a);c.drawLine(0,top,w,top,p);
            for(int aisle=0;aisle<=16;aisle++){float x=w*aisle/16f;p.setColor(0xffa9b2aa);c.drawLine(x,top,w*.5f+(x-w*.5f)*.91f,bottom,p);}
        }
        p.setStrokeWidth(Math.max(1,h*.003f));p.setColor(0xff8d9890);
        c.drawLine(0,skyline,w,skyline,p);c.drawLine(0,skyline+h*.035f,w,skyline+h*.035f,p);
        for(int beam=0;beam<28;beam++){float x=w*beam/28f;c.drawLine(x,skyline,x+w/56f,skyline+h*.035f,p);c.drawLine(x+w/56f,skyline+h*.035f,x+w/28f,skyline,p);}
        // A continuous grass apron behind the touchlines, rather than lines floating on a rectangle.
        p.setShader(new LinearGradient(0,standBottom,0,h,0xff34582a,0xff3f6e23,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRect(0,standBottom,w,h,p);p.setShader(null);
        patch(c,0xff4a7628,-10,-12,115,80);
        for(int i=0;i<14;i++)patch(c,i%2==0?0xff4f7d2c:0xff608e35,i*105f/14,-1,(i+1)*105f/14,69);
        // Subtle cross-cut turf texture remains deterministic and is cached once per size.
        for(int row=0;row<90;row++){
            float yy=row*68f/90;p.setColor(row%2==0?0x0840a431:0x090a2609);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.5f,h*.001f));line(c,0,yy,105,yy);
        }
        advertising(c,w,h);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.1f,h*.0036f));p.setColor(0xffd7e7b7);
        box(c,0,0,105,68);line(c,52.5f,0,52.5f,68);arc(c,52.5f,34,9.15f,0,360);
        box(c,0,13.84f,16.5f,54.16f);box(c,88.5f,13.84f,105,54.16f);
        box(c,0,24.84f,5.5f,43.16f);box(c,99.5f,24.84f,105,43.16f);
        float d=(float)Math.toDegrees(Math.acos(5.5/9.15));arc(c,11,34,9.15f,-d,d);arc(c,94,34,9.15f,180-d,180+d);
        arc(c,0,0,1,0,90);arc(c,105,0,1,90,180);arc(c,0,68,1,270,360);arc(c,105,68,1,180,270);
        spot(c,52.5f,34,Math.max(1,h*.0032f));spot(c,11,34,Math.max(.8f,h*.0028f));spot(c,94,34,Math.max(.8f,h*.0028f));
        goal(c,0,-2.5f,h);goal(c,105,107.5f,h);
        for(float x:new float[]{0,105})for(float y:new float[]{0,68}){
            float px=plane.x(PitchProjection.mx(x),PitchProjection.my(y)),py=plane.y(PitchProjection.mx(x),PitchProjection.my(y));float flag=Math.max(4,h*.032f)*plane.scale(PitchProjection.my(y));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,h*.002f));p.setColor(0xffe6dfbd);c.drawLine(px,py,px,py-flag,p);polygon(c,0xffdb493b,px,py-flag,px+flag*.55f,py-flag*.86f,px,py-flag*.60f);
        }
        // Soft bowl shadows and a vignette keep the bright pitch central.
        p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(w*.5f,h*.65f,w*.7f,new int[]{0x00000000,0x00000000,0x44202b15},new float[]{0,.54f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
    }
    private void advertising(Canvas c,int w,int h){
        float y=-3.0f,raise=Math.max(5,h*.04f);
        float l=plane.x(PitchProjection.mx(-1),PitchProjection.my(y)),r=plane.x(PitchProjection.mx(106),PitchProjection.my(y)),yy=plane.y(PitchProjection.mx(0),PitchProjection.my(y));
        polygon(c,0xff131a16,l,yy-raise,r,yy-raise,r,yy,l,yy);
        p.setColor(0xffd8eea3);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));p.setTextSize(Math.max(4,h*.021f));p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<7;i++)c.drawText("BOSS XI",l+(r-l)*(i+.5f)/7f,yy-raise*.27f,p);
        // Side boards recede with the same ground-plane transform.
        for(float x:new float[]{-3,108}){
            float a=plane.x(PitchProjection.mx(x),PitchProjection.my(-3)),b=plane.x(PitchProjection.mx(x),PitchProjection.my(71)),ya=plane.y(0,PitchProjection.my(-3)),yb=plane.y(0,PitchProjection.my(71));
            polygon(c,0xff182018,a,ya-raise,b,yb-raise,b,yb,a,ya);
        }
    }
    private void goal(Canvas c,float front,float back,float h){
        float y1=30.34f,y2=37.66f;float z=Math.max(5,h*.048f);
        float a=PitchProjection.mx(front),b=PitchProjection.mx(back),u=PitchProjection.my(y1),v=PitchProjection.my(y2);
        float ax=plane.x(a,u),ay=plane.y(a,u),bx=plane.x(a,v),by=plane.y(a,v),cx=plane.x(b,u),cy=plane.y(b,u),dx=plane.x(b,v),dy=plane.y(b,v);
        polygon(c,0x456a8067,ax,ay-z,cx,cy-z,dx,dy-z,bx,by-z);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.45f,h*.0011f));p.setColor(0x99d9e3d1);
        for(int i=0;i<=8;i++){float f=i/8f;c.drawLine(cx+(dx-cx)*f,cy+(dy-cy)*f,cx+(dx-cx)*f,cy+(dy-cy)*f-z,p);c.drawLine(ax+(bx-ax)*f,ay+(by-ay)*f-z,cx+(dx-cx)*f,cy+(dy-cy)*f-z,p);}
        for(int i=0;i<=4;i++){float q=z*i/4;c.drawLine(cx,cy-q,dx,dy-q,p);c.drawLine(ax,ay-q,cx,cy-q,p);c.drawLine(bx,by-q,dx,dy-q,p);}
        p.setColor(0xffedf1e4);p.setStrokeWidth(Math.max(1.3f,h*.0045f));c.drawLine(ax,ay,ax,ay-z,p);c.drawLine(ax,ay-z,bx,by-z,p);c.drawLine(bx,by-z,bx,by,p);
        p.setStrokeWidth(Math.max(.8f,h*.0023f));c.drawLine(ax,ay-z,cx,cy-z,p);c.drawLine(bx,by-z,dx,dy-z,p);c.drawLine(cx,cy-z,dx,dy-z,p);c.drawLine(cx,cy-z,cx,cy,p);c.drawLine(dx,dy-z,dx,dy,p);
        p.setStyle(Paint.Style.FILL);
    }
}
