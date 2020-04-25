
#import <Cocoa/Cocoa.h>
#include <JavaVM/jawt_md.h>
#include "org_lwjgl_opengl_awt_PlatformMacOSXGLCanvas.h"


JNIEXPORT jlong JNICALL Java_org_lwjgl_opengl_awt_PlatformMacOSXGLCanvas_createView
 (JNIEnv *env, jobject object, jlong platformInfo) {
     id <JAWT_SurfaceLayers> surfaceLayers = (id)platformInfo;
     NSOpenGLView *view = [[NSOpenGLView alloc] initWithFrame:surfaceLayers.windowLayer.frame pixelFormat:nil];
     [view setWantsLayer:YES];
     [view.layer setNeedsDisplayOnBoundsChange:YES];
     surfaceLayers.layer = view.layer;
     return (jlong) view;
 }
