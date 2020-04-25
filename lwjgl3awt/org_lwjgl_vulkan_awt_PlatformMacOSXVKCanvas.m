

#import <MetalKit/MetalKit.h>
#include <JavaVM/jawt_md.h>
#include "org_lwjgl_opengl_awt_PlatformMacOSXGLCanvas.h"


JNIEXPORT jlong JNICALL Java_org_lwjgl_vulkan_awt_PlatformMacOSXVKCanvas_createMTKView
  (JNIEnv *env, jobject object, jlong platformInfo) {
      id<JAWT_SurfaceLayers> surfaceLayers = (id)platformInfo;
      id<MTLDevice> device = MTLCreateSystemDefaultDevice();
      MTKView *view = [[MTKView alloc] initWithFrame:surfaceLayers.windowLayer.frame device:device];
      //[view.layer setNeedsDisplayOnBoundsChange:YES];
      surfaceLayers.layer = view.layer;
      return (jlong) view.layer;
  }

