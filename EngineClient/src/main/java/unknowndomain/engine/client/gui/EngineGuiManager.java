package unknowndomain.engine.client.gui;

import unknowndomain.engine.Platform;
import unknowndomain.engine.client.rendering.RenderContext;
import unknowndomain.engine.util.UndoHistory;

import java.util.HashMap;
import java.util.Map;

public class EngineGuiManager implements GuiManager {

    //TODO: review on availability of customizing limit of history
    public static final int MAX_SCENE_HISTORY = 20;
    private final RenderContext context;

    private Map<String, Scene> huds;
    private Scene displayingScreen;
    private UndoHistory<Scene> sceneHistory;

    public EngineGuiManager(RenderContext context) {
        this.context = context;
        huds = new HashMap<>();
        sceneHistory = new UndoHistory<>(MAX_SCENE_HISTORY);
    }

    private boolean incognito = false;

    @Override
    public void showScreen(Scene scene) {
        showScreenInternal(scene);
        incognito = false;
    }

    private void showScreenInternal(Scene scene) {
        pushToHistory();
        displayingScreen = scene;
        context.getWindow().addCharCallback(displayingScreen.charCallback);
        context.getWindow().addCursorCallback(displayingScreen.cursorCallback);
        context.getWindow().addKeyCallback(displayingScreen.keyCallback);
        context.getWindow().addMouseCallback(displayingScreen.mouseCallback);
        context.getWindow().addScrollCallback(displayingScreen.scrollCallback);
        context.getWindow().getCursor().showCursor();
    }

    private void pushToHistory() {
        if (displayingScreen != null) {
            if (!incognito) {
                sceneHistory.pushHistory(displayingScreen);
            }
            context.getWindow().removeCharCallback(displayingScreen.charCallback);
            context.getWindow().removeCursorCallback(displayingScreen.cursorCallback);
            context.getWindow().removeKeyCallback(displayingScreen.keyCallback);
            context.getWindow().removeMouseCallback(displayingScreen.mouseCallback);
            context.getWindow().removeScrollCallback(displayingScreen.scrollCallback);
        }
    }

    public void showIncognitoScreen(Scene scene) {
        showScreenInternal(scene);
        incognito = true;
    }

    @Override
    public void showLastScreen() {
        var lastscreen = sceneHistory.undo();
        showIncognitoScreen(lastscreen);
    }

    @Override
    public void showHud(String id, Scene hud) {
        if (huds.containsKey(id)) {
            Platform.getLogger().warn(String.format("Conflicting HUD id!: %s", id));
        } else {
            huds.put(id, hud);
        }
    }

    @Override
    public void closeScreen() {
        pushToHistory();
        displayingScreen = null;
        context.getWindow().getCursor().disableCursor();
    }

    @Override
    public void hideHud(String id) {
        huds.remove(id);
    }

    @Override
    public Map<String, Scene> getHuds() {
        return huds;
    }

    @Override
    public Scene getDisplayingScreen() {
        return displayingScreen;
    }

    @Override
    public boolean isDisplayingScreen() {
        return displayingScreen != null;
    }
}
