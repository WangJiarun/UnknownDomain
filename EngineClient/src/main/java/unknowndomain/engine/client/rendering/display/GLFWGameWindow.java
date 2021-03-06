package unknowndomain.engine.client.rendering.display;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import unknowndomain.engine.Platform;
import unknowndomain.engine.util.RuntimeEnvironment;

import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GLFWGameWindow implements GameWindow {

    private long windowId;

    private int windowWidth;
    private int windowHeight;
    private int fboWidth;
    private int fboHeight;

    private float contentScaleX;
    private float contentScaleY;

    private boolean resized = false;
    private Matrix4f projection;

    private String title;

    private boolean closed = false;

    private Cursor cursor;

    private final List<KeyCallback> keyCallbacks = new LinkedList<>();
    private final List<MouseCallback> mouseCallbacks = new LinkedList<>();
    private final List<CursorCallback> cursorCallbacks = new LinkedList<>();
    private final List<ScrollCallback> scrollCallbacks = new LinkedList<>();
    private final List<CharCallback> charCallbacks = new LinkedList<>();

    public GLFWGameWindow(int width, int height, String title) {
        this.title = title;
        this.windowWidth = width;
        this.windowHeight = height;
    }

    @Override
    public int getWidth() {
        return fboWidth;
    }

    @Override
    public int getHeight() {
        return fboHeight;
    }

    @Override
    public float getContentScaleX() {
        return contentScaleX;
    }

    @Override
    public float getContentScaleY() {
        return contentScaleY;
    }

    @Override
    public void setSize(int width, int height) {
        this.fboWidth = width;
        this.fboHeight = height;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            FloatBuffer f1 = memoryStack.mallocFloat(1);
            FloatBuffer f2 = memoryStack.mallocFloat(1);

            glfwGetWindowContentScale(windowId, f1,f2);

            if(contentScaleX != f1.get(0) || contentScaleY != f2.get(0)){
                contentScaleX = f1.get(0);
                contentScaleY = f2.get(0);
            }
        }
        this.windowWidth = Math.round(width / contentScaleX);
        this.windowHeight = Math.round(height / contentScaleX);
        this.resized = true;
        glViewport(0, 0, width, height);
    }

    @Override
    public Matrix4f projection() {
        if (resized || projection == null) {
            projection = new Matrix4f().perspective((float) (Math.toRadians(Math.max(1.0, Math.min(90.0, 60.0f)))), windowWidth / (float) windowHeight, 0.01f, 1000f);
        }
        return projection;
    }

    @Override
    public boolean isResized() {
        return resized;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        glfwSetWindowTitle(windowId, title);
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void addKeyCallback(KeyCallback callback) {
        keyCallbacks.add(requireNonNull(callback));
    }

    @Override
    public void removeKeyCallback(KeyCallback callback) {
        keyCallbacks.remove(callback);
    }

    @Override
    public void addMouseCallback(MouseCallback callback) {
        mouseCallbacks.add(requireNonNull(callback));
    }

    @Override
    public void removeMouseCallback(MouseCallback callback) {
        mouseCallbacks.remove(callback);
    }

    @Override
    public void addCursorCallback(CursorCallback callback) {
        cursorCallbacks.add(requireNonNull(callback));
    }

    @Override
    public void removeCursorCallback(CursorCallback callback) {
        cursorCallbacks.remove(callback);
    }

    @Override
    public void addScrollCallback(ScrollCallback callback) {
        scrollCallbacks.add(requireNonNull(callback));
    }

    @Override
    public void removeScrollCallback(ScrollCallback callback) {
        scrollCallbacks.remove(callback);
    }

    @Override
    public void addCharCallback(CharCallback callback) {
        charCallbacks.add(requireNonNull(callback));
    }

    @Override
    public void removeCharCallback(CharCallback callback) {
        charCallbacks.remove(callback);
    }

    @Override
    public void beginRender() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void endRender() {
        glfwSwapBuffers(windowId);

        if (isResized()) {
            resized = false;
        }

        glfwPollEvents();

        updateFps();
    }

    private long lastUpdateFps = System.currentTimeMillis();
    private int frameCount = 0;
    private int fps = 0;

    @Override
    public int getFps() {
        return fps;
    }

    public void updateFps() {
        long time = System.currentTimeMillis();
        if (time - lastUpdateFps > 1000) {
            fps = frameCount;
            frameCount = 0; // reset the FPS counter
            lastUpdateFps += 1000; // add one second
        }
        frameCount++;
    }

    @Override
    public void close() {
        closed = true;
        glfwDestroyWindow(windowId);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public long getWindowId() {
        return windowId;
    }

    public void init() {
        initErrorCallback(System.err);
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        initWindowHint();
        windowId = glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
        if (!checkCreated())
            throw new RuntimeException("Failed to parse the GLFW window");
        long moniter = glfwGetPrimaryMonitor();
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            FloatBuffer f1 = memoryStack.mallocFloat(1);
            FloatBuffer f2 = memoryStack.mallocFloat(1);
            glfwGetMonitorContentScale(moniter,f1,f2);
            contentScaleX = f1.get(0);
            contentScaleY = f2.get(0);
        }
        initCallbacks();
        setWindowPosCenter();
        glfwMakeContextCurrent(windowId);
        GL.createCapabilities();
        enableVSync();
        cursor = new GLFWCursor(windowId);
        setupInput();
        showWindow();
    }

    private void setupInput() {
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> keyCallbacks.forEach(keyCallback -> keyCallback.invoke(key, scancode, action, mods)));
        glfwSetMouseButtonCallback(windowId, (window, button, action, mods) -> mouseCallbacks.forEach(mouseCallback -> mouseCallback.invoke(button, action, mods)));
        glfwSetCursorPosCallback(windowId, (window, xpos, ypos) -> cursorCallbacks.forEach(cursorCallback -> cursorCallback.invoke(xpos, ypos)));
        glfwSetScrollCallback(windowId, (window, xoffset, yoffset) -> scrollCallbacks.forEach(scrollCallback -> scrollCallback.invoke(xoffset, yoffset)));
        glfwSetCharCallback(windowId, (window, codepoint) -> charCallbacks.forEach(charCallback -> charCallback.invoke((char) codepoint)));

        // TODO: Remove it.
        addKeyCallback((key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F12 && action == GLFW_PRESS) {
                Platform.getEngine().terminate();
            }
        });
    }

    private boolean checkCreated() {
        return windowId != NULL;
    }

    private void initCallbacks() {
        glfwSetFramebufferSizeCallback(windowId, (window, width, height) -> setSize(width, height));
    }

    private void initWindowHint() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GL_TRUE);
        if(Platform.getEngineClient().getRuntimeEnvironment() != RuntimeEnvironment.DEPLOYMENT){
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }
        if(Platform.getRunningOsPlatform() == org.lwjgl.system.Platform.MACOSX){
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);
        }
    }

    private void initErrorCallback(PrintStream stream) {
        GLFWErrorCallback.createPrint(stream).set();
    }

    private void setWindowPosCenter() {
        GLFWVidMode vidmode = Objects.requireNonNull(glfwGetVideoMode(glfwGetPrimaryMonitor()));
        // Center our window
        glfwSetWindowPos(windowId, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
    }

    private void enableVSync() {
        glfwSwapInterval(1);
    }

    private void showWindow() {
        glfwShowWindow(windowId);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        // glViewport(0, 0, width, height);
        //getCursor().setCursorState(CursorState.DISABLED);
    }
}
