javah -d lwjgl3awt -jni -cp "target/classes:lwjgl-jawt-3.2.3.jar:lwjgl-3.2.3.jar" org.lwjgl.opengl.awt.PlatformMacOSXGLCanvas
javah -d lwjgl3awt -jni -cp "target/classes:lwjgl-jawt-3.2.3.jar:lwjgl-3.2.3.jar:/Users/fox/.m2/repository/org/lwjgl/lwjgl-vulkan/3.2.4-SNAPSHOT/lwjgl-vulkan-3.2.4-20200121.173540-5.jar" org.lwjgl.vulkan.awt.PlatformMacOSXVKCanvas
for i in lwjgl3awt/*.h; do
    sed -i "" "2s|#include <jni.h>|#include <JavaVM/jni.h>|" $i
done
