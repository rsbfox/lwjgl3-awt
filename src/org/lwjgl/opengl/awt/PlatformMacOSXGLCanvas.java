package org.lwjgl.opengl.awt;

import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;
import org.lwjgl.system.macosx.ObjCRuntime;

import java.awt.*;

import static org.lwjgl.opengl.CGL.*;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.jawt.JAWTFunctions.*;
import static org.lwjgl.system.macosx.ObjCRuntime.objc_getClass;
import static org.lwjgl.system.macosx.ObjCRuntime.sel_getUid;

public class PlatformMacOSXGLCanvas implements PlatformGLCanvas {
    public static final JAWT awt;
    private static final long objc_msgSend;
    static {
        awt = JAWT.calloc();
        awt.version(JAWT_VERSION_1_7);
        if (!JAWT_GetAWT(awt))
            throw new AssertionError("GetAWT failed");
        System.loadLibrary("lwjgl3awt");
        objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
    }

    public JAWTDrawingSurface ds;
    private long view;
    private long context;

    private native long createView(long platformInfo);

    @Override
    public long create(Canvas canvas, GLData attribs, GLData effective) throws AWTException {
        this.ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        JAWTDrawingSurface ds = JAWT_GetDrawingSurface(canvas, awt.GetDrawingSurface());
        try {
            lock();
            try {
                JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
                try {
                    long windowLayer = invokePPP(dsi.platformInfo(), sel_getUid("windowLayer"), objc_msgSend);
                    // http://forum.lwjgl.org/index.php?topic=6933.msg36495#msg36495
                    // long frame = invokePPP(windowLayer, sel_getUid("frame"), objc_msgSend);
                    // long NSOpenGLView = objc_getClass("NSOpenGLView");
                    // long view = invokePPP(NSOpenGLView, sel_getUid("alloc"), objc_msgSend);
                    // long pixelFormat = invokePPP(NSOpenGLView, sel_getUid("defaultPixelFormat"), objc_msgSend);
                    // view = invokePPPPPP(view, sel_getUid("initWithFrame"), frame, sel_getUid("pixelFormat"), pixelFormat, objc_msgSend);
                    view = createView(dsi.platformInfo());
                    long openGLContext = invokePPP(view, sel_getUid("openGLContext"), objc_msgSend);
                    context = invokePPP(openGLContext, sel_getUid("CGLContextObj"), objc_msgSend);
                    return context;
                } finally {
                    JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                }
            } finally {
                unlock();
            }
        } finally {
            JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
        }
    }

    @Override
    public boolean swapBuffers() {
        glFlush();
        //CGLFlushDrawable(context);
        // fixes render not showing till resize but breaks draw on demand test
        long CATransaction = objc_getClass("CATransaction");
        invokePPP(CATransaction, sel_getUid("flush"), objc_msgSend);
        return true;
    }

    @Override
    public boolean deleteContext(long context) {
        glFinish();
        invokePPP(view, sel_getUid("removeFromSuperviewWithoutNeedingDisplay"), objc_msgSend);
        invokePPP(view, sel_getUid("release"), objc_msgSend);
        return false;
    }

    @Override
    public boolean makeCurrent(long context) {
        CGLSetCurrentContext(context);
        return true;
    }

    @Override
    public boolean isCurrent(long context) {
        return false;
    }


    @Override
    public boolean delayBeforeSwapNV(float seconds) {
        return false;
    }

    @Override
    public void lock() throws AWTException {
        int lock = JAWT_DrawingSurface_Lock(ds, ds.Lock());
        if ((lock & JAWT_LOCK_ERROR) != 0)
            throw new AWTException("JAWT_DrawingSurface_Lock() failed");
    }

    @Override
    public void unlock() throws AWTException {
        JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
    }

    @Override
    public void dispose() {
        JAWT_FreeDrawingSurface(this.ds, awt.FreeDrawingSurface());
        this.ds = null;
    }

}
