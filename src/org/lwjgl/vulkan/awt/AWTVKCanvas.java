package org.lwjgl.vulkan.awt;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.awt.SimpleDemo.*;
import static org.lwjgl.vulkan.awt.VKUtil.VK_FLAGS_NONE;
import static org.lwjgl.vulkan.awt.VKUtil.translateVulkanResult;

/**
 * An AWT {@link Canvas} that supports to be drawn on using Vulkan.
 * 
 * @author Kai Burjack
 */
public abstract class AWTVKCanvas extends Canvas {
    private static final long serialVersionUID = 1L;
    private static PlatformVKCanvas platformCanvas;
    static {
        String platformClassName;
        switch (Platform.get()) {
        case WINDOWS:
            platformClassName = "org.lwjgl.vulkan.awt.PlatformWin32VKCanvas";
            break;
        case LINUX:
            platformClassName = "org.lwjgl.vulkan.awt.PlatformX11VKCanvas";
            break;
        case MACOSX:
            platformClassName = "org.lwjgl.vulkan.awt.PlatformMacOSXVKCanvas";
            break;
        default:
            throw new AssertionError("NYI");
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends PlatformVKCanvas> clazz = (Class<? extends PlatformVKCanvas>) AWTVKCanvas.class.getClassLoader().loadClass(platformClassName);
            platformCanvas = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Platform-specific VKCanvas class not found: " + platformClassName);
        } catch (InstantiationException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        }
    }

    private final VKData data;
    public long surface;

    /*
     * All resources that must be reallocated on window resize.
     */
    public static Swapchain swapchain;
    public static int width, height;
    public static long[] framebuffers;
    public static VkCommandBuffer[] renderCommandBuffers;

    //final long debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
    VkPhysicalDevice physicalDevice;
    DeviceAndGraphicsQueueFamily deviceAndGraphicsQueueFamily;
    VkDevice device;
    int queueFamilyIndex;
    VkPhysicalDeviceMemoryProperties memoryProperties;

    // Create static Vulkan resources
    ColorFormatAndSpace colorFormatAndSpace;
    long commandPool;
    VkCommandBuffer setupCommandBuffer;
    VkQueue queue;
    long renderPass;
    long renderCommandPool;
    Vertices vertices;
    long pipeline;
    SwapchainRecreator swapchainRecreator;

    IntBuffer pImageIndex;
    int currentBuffer;
    PointerBuffer pCommandBuffers;
    LongBuffer pSwapchains;
    LongBuffer pImageAcquiredSemaphore;
    LongBuffer pRenderCompleteSemaphore;

    // Info struct to create a semaphore
    VkSemaphoreCreateInfo semaphoreCreateInfo;

    // Info struct to submit a command buffer which will wait on the semaphore
    VkSubmitInfo submitInfo;

    // Info struct to present the current swapchain image to the display
    VkPresentInfoKHR presentInfo;

    public static class Swapchain {
        long swapchainHandle;
        long[] images;
        long[] imageViews;
    }

    public static class DeviceAndGraphicsQueueFamily {
        VkDevice device;
        int queueFamilyIndex;
        VkPhysicalDeviceMemoryProperties memoryProperties;
    }

    public static class ColorFormatAndSpace {
        int colorFormat;
        int colorSpace;
    }

    public static class Vertices {
        long verticesBuf;
        VkPipelineVertexInputStateCreateInfo createInfo;
    }

    final class SwapchainRecreator {
        boolean mustRecreate = true;
        void recreate() {
            // Begin the setup command buffer (the one we will use for swapchain/framebuffer creation)
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            int err = vkBeginCommandBuffer(setupCommandBuffer, cmdBufInfo);
            cmdBufInfo.free();
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to begin setup command buffer: " + translateVulkanResult(err));
            }
            long oldChain = swapchain != null ? swapchain.swapchainHandle : VK_NULL_HANDLE;
            // Create the swapchain (this will also add a memory barrier to initialize the framebuffer images)
            swapchain = createSwapChain(device, physicalDevice, surface, oldChain, setupCommandBuffer,
                    width, height, colorFormatAndSpace.colorFormat, colorFormatAndSpace.colorSpace);
            err = vkEndCommandBuffer(setupCommandBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to end setup command buffer: " + translateVulkanResult(err));
            }
            submitCommandBuffer(queue, setupCommandBuffer);
            vkQueueWaitIdle(queue);

            if (framebuffers != null) {
                for (int i = 0; i < framebuffers.length; i++)
                    vkDestroyFramebuffer(device, framebuffers[i], null);
            }
            framebuffers = createFramebuffers(device, swapchain, renderPass, width, height);
            // Create render command buffers
            if (renderCommandBuffers != null) {
                vkResetCommandPool(device, renderCommandPool, VK_FLAGS_NONE);
            }
            renderCommandBuffers = createRenderCommandBuffers(device, renderCommandPool, framebuffers, renderPass, width, height, pipeline,
                    vertices.verticesBuf);

            mustRecreate = false;
        }
    }

    protected AWTVKCanvas(VKData data) {
        this.data = data;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = e.getComponent().getWidth();
                int h = e.getComponent().getHeight();
                if (w <= 0 || h <= 0 || swapchainRecreator == null)
                    return;
                width = w;
                height = h;
                swapchainRecreator.mustRecreate = true;
            }
        });

    }

    @Override
    public void paint(Graphics g) {
        boolean created = false;
        if (surface == 0L) {
            try {
                surface = platformCanvas.create(this, data);
                created = true;
            } catch (AWTException e) {
                throw new RuntimeException("Exception while creating the Vulkan surface", e);
            }
        }
        if (created) {
            try {
                initVK();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        paintVK();
        if (platformCanvas instanceof PlatformMacOSXVKCanvas) {
            PlatformMacOSXVKCanvas.caFlush();
        }
    }

    /**
     * Determine whether there is presentation support for the given {@link VkPhysicalDevice} in a command queue of the specified <code>queueFamiliy</code>.
     * 
     * @param physicalDevice
     *            the Vulkan {@link VkPhysicalDevice}
     * @param queueFamily
     *            the command queue family
     * @return <code>true</code> of <code>false</code>
     */
    public boolean getPhysicalDevicePresentationSupport(VkPhysicalDevice physicalDevice, int queueFamily) {
        return platformCanvas.getPhysicalDevicePresentationSupport(physicalDevice, queueFamily);
    }

    /**
     * Will be called once after the Vulkan surface has been created.
     */
    public abstract void initVK() throws IOException;

    /**
     * Will be called whenever the {@link Canvas} needs to paint itself.
     */
    public abstract void paintVK();

}
