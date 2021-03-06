package unknowndomain.engine.client.rendering;

import org.joml.FrustumIntersection;
import unknowndomain.engine.client.EngineClient;
import unknowndomain.engine.client.gui.GuiManager;
import unknowndomain.engine.client.rendering.camera.Camera;
import unknowndomain.engine.client.rendering.display.GameWindow;
import unknowndomain.engine.client.rendering.texture.TextureManager;

import javax.annotation.Nonnull;

public interface RenderContext {

    EngineClient getEngine();

    Thread getRenderThread();

    boolean isRenderThread();

    GameWindow getWindow();

    @Nonnull
    Camera getCamera();

    void setCamera(@Nonnull Camera camera);

    FrustumIntersection getFrustumIntersection();

    TextureManager getTextureManager();

    GuiManager getGuiManager();
}
