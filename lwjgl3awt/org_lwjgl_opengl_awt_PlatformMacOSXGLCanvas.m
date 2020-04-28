
#import <Cocoa/Cocoa.h>
#include <JavaVM/jawt_md.h>
#include "org_lwjgl_opengl_awt_PlatformMacOSXGLCanvas.h"


JNIEXPORT jlong JNICALL Java_org_lwjgl_opengl_awt_PlatformMacOSXGLCanvas_createView
 (JNIEnv *env, jobject object, jlong platformInfo, jint width, jint height) {
     id <JAWT_SurfaceLayers> surfaceLayers = (id)platformInfo;
     CGRect frame = CGRectMake(0, surfaceLayers.windowLayer.frame.size.height-height, width, height);
     NSOpenGLPixelFormatAttribute attrs[] =
     {
         NSOpenGLPFAOpenGLProfile,
         NSOpenGLProfileVersion4_1Core,
         NSOpenGLPFAMultisample,
         NSOpenGLPFASampleBuffers, 1,
         NSOpenGLPFASamples, 16,
         0
     };
     NSOpenGLPixelFormat *pixelFormat = [[[NSOpenGLPixelFormat alloc] initWithAttributes:attrs] autorelease];
     NSOpenGLView *view = [[NSOpenGLView alloc] initWithFrame:frame pixelFormat:pixelFormat];
     [view setWantsLayer:YES];
     surfaceLayers.layer = view.layer;
     return (jlong) view;
 }
