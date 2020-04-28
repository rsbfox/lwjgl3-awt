package org.lwjgl.opengl.awt;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawElements;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glClearColor;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glEnable;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glGetAttribLocation;
import static org.lwjgl.opengl.GL20C.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20C.glGetProgrami;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glUniform1i;
import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL20C.glViewport;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryUtil.memUTF8;


/**
 * Renders a simple textured sphere using OpenGL 4.0 Core Profile.
 *
 * @author Kai Burjack
 * Adapted for AWTGLCanvas
 */
public class SimpleTexturedSphere {
    public static void main(String[] args) {
        JFrame frame = new JFrame("AWT test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(1024, 768));
        GLData data = new GLData();
        data.samples = 4;
        data.swapInterval = 0;
        AWTGLCanvas canvas;
        frame.add(canvas = new AWTGLCanvas(data) {
            private static final long serialVersionUID = 1L;
            static final int rings = 80;
            static final int sectors = 80;
            int vao;
            int sphereProgram;
            int sphereProgram_inputPosition;
            int sphereProgram_inputTextureCoords;
            int sphereProgram_matrixUniform;
            Matrix4f m = new Matrix4f();
            FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
            float time;
            long lastTime = System.nanoTime();
            public void initGL() {
                createCapabilities();
                try {
                    createTexture();
                    createQuadProgram();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                createSphere();
                glClearColor(0.02f, 0.03f, 0.04f, 1.0f);
                glEnable(GL_DEPTH_TEST);
                glEnable(GL_CULL_FACE);
            }
            public void paintGL() {
                glViewport(0, 0, getWidth(), getHeight());
                long thisTime = System.nanoTime();
                float elapsedTime = (thisTime - lastTime) * 1E-9f;
                lastTime = thisTime;
                update(elapsedTime);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glUseProgram(sphereProgram);
                glUniformMatrix4fv(sphereProgram_matrixUniform, false, m.get(matrixBuffer));
                glBindVertexArray(vao);
                glDrawElements(GL_TRIANGLES, (rings - 1) * (sectors - 1) * 6, GL_UNSIGNED_INT, 0L);
                glBindVertexArray(0);
                glUseProgram(0);
                swapBuffers();
            }

            void createQuadProgram() throws IOException {
                int program = glCreateProgram();
                int vshader = createShader("org/lwjgl/demo/texturedSphere.vs", GL_VERTEX_SHADER);
                int fshader = createShader("org/lwjgl/demo/texturedSphere.fs", GL_FRAGMENT_SHADER);
                glAttachShader(program, vshader);
                glAttachShader(program, fshader);
                glLinkProgram(program);
                int linked = glGetProgrami(program, GL_LINK_STATUS);
                String programLog = glGetProgramInfoLog(program);
                if (programLog.trim().length() > 0)
                    System.err.println(programLog);
                if (linked == 0)
                    throw new AssertionError("Could not link program");
                glUseProgram(program);
                int texLocation = glGetUniformLocation(program, "tex");
                glUniform1i(texLocation, 0);
                sphereProgram_matrixUniform = glGetUniformLocation(program, "matrix");
                sphereProgram_inputPosition = glGetAttribLocation(program, "position");
                sphereProgram_inputTextureCoords = glGetAttribLocation(program, "texCoords");
                glUseProgram(0);
                this.sphereProgram = program;
            }

            void createSphere() {
                vao = glGenVertexArrays();
                glBindVertexArray(vao);
                int vbo = glGenBuffers();
                /* Generate vertex buffer */
                float PI = (float) Math.PI;
                float R = 1f / (rings - 1);
                float S = 1f / (sectors - 1);
                FloatBuffer fb = MemoryUtil.memAllocFloat(rings * sectors * (3 + 2));
                for (int r = 0; r < rings; r++) {
                    for (int s = 0; s < sectors; s++) {
                        float x = (float) (Math.cos(2 * PI * s * S) * Math.sin(PI * r * R));
                        float y = (float) Math.sin(-PI / 2 + PI * r * R);
                        float z = (float) (Math.sin(2 * PI * s * S) * Math.sin(PI * r * R));
                        fb.put(x).put(y).put(z);
                        fb.put(1.0f - s * S).put(1.0f - r * R);
                    }
                }
                fb.flip();
                glBindBuffer(GL_ARRAY_BUFFER, vbo);
                glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
                MemoryUtil.memFree(fb);
                glVertexAttribPointer(sphereProgram_inputPosition, 3, GL_FLOAT, false, (3 + 2) * 4, 0L);
                glEnableVertexAttribArray(sphereProgram_inputPosition);
                glVertexAttribPointer(sphereProgram_inputTextureCoords, 2, GL_FLOAT, false, (3 + 2) * 4, 3 * 4L);
                glEnableVertexAttribArray(sphereProgram_inputTextureCoords);
                /* Generate index/element buffer */
                int ibo = glGenBuffers();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
                IntBuffer ib = MemoryUtil.memAllocInt((rings - 1) * (sectors - 1) * 6);
                for (int r = 0; r < rings - 1; r++) {
                    for (int s = 0; s < sectors - 1; s++) {
                        ib.put(r * sectors + s).put((r + 1) * sectors + s).put((r + 1) * sectors + s + 1);
                        ib.put((r + 1) * sectors + s + 1).put(r * sectors + s + 1).put(r * sectors + s);
                    }
                }
                ib.flip();
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
                glBindBuffer(GL_ARRAY_BUFFER, 0);
                MemoryUtil.memFree(ib);
                glBindVertexArray(0);
            }

            void createTexture() throws IOException {
                try (MemoryStack frame = MemoryStack.stackPush()) {
                    IntBuffer width = frame.mallocInt(1);
                    IntBuffer height = frame.mallocInt(1);
                    IntBuffer components = frame.mallocInt(1);
                    ByteBuffer data = stbi_load_from_memory(
                            ioResourceToByteBuffer("org/lwjgl/demo/earth.jpg", 1024), width, height, components,
                            4);
                    int id = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, id);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                    stbi_image_free(data);
                }
            }

            void update(float elapsedTime) {
                time += elapsedTime;
                m.setPerspective((float) Math.toRadians(45), (float) getWidth() / getWidth(), 0.1f, 10.0f)
                        .lookAt(0, 1, 3,
                                0, 0, 0,
                                0, 1, 0)
                        .rotateY(time * 0.2f);
            }


        }, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.transferFocus();

        Runnable renderLoop = new Runnable() {
            public void run() {
                if (!canvas.isValid())
                    return;
                canvas.render();
                SwingUtilities.invokeLater(this);
            }
        };
        SwingUtilities.invokeLater(renderLoop);
    }

    public static int createShader(String resource, int type) throws IOException {
        return createShader(resource, type, null);
    }

    public static int createShader(String resource, int type, String version) throws IOException {
        int shader = glCreateShader(type);

        ByteBuffer source = ioResourceToByteBuffer(resource, 8192);

        if ( version == null ) {
            PointerBuffer strings = BufferUtils.createPointerBuffer(1);
            IntBuffer lengths = BufferUtils.createIntBuffer(1);

            strings.put(0, source);
            lengths.put(0, source.remaining());

            glShaderSource(shader, strings, lengths);
        } else {
            PointerBuffer strings = BufferUtils.createPointerBuffer(2);
            IntBuffer lengths = BufferUtils.createIntBuffer(2);

            ByteBuffer preamble = memUTF8("#version " + version + "\n", false);

            strings.put(0, preamble);
            lengths.put(0, preamble.remaining());

            strings.put(1, source);
            lengths.put(1, source.remaining());

            glShaderSource(shader, strings, lengths);
        }

        glCompileShader(shader);
        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
        String shaderLog = glGetShaderInfoLog(shader);
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        if (compiled == 0) {
            throw new AssertionError("Could not compile shader");
        }
        return shader;
    }

    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null)
            throw new IOException("Classpath resource not found: " + resource);
        File file = new File(url.getFile());
        if (file.isFile()) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            fc.close();
            fis.close();
        } else {
            buffer = BufferUtils.createByteBuffer(bufferSize);
            InputStream source = url.openStream();
            if (source == null)
                throw new FileNotFoundException(resource);
            try {
                byte[] buf = new byte[8192];
                while (true) {
                    int bytes = source.read(buf, 0, buf.length);
                    if (bytes == -1)
                        break;
                    if (buffer.remaining() < bytes)
                        buffer = resizeBuffer(buffer, Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytes));
                    buffer.put(buf, 0, bytes);
                }
                buffer.flip();
            } finally {
                source.close();
            }
        }
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
