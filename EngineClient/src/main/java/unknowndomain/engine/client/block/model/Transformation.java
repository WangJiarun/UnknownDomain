package unknowndomain.engine.client.block.model;
import org.joml.Matrix4d;
/** 
* @author byxiaobai
* 来自 教程
*/
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Transformation {

    private final Matrix4d projectionMatrix;

    private final Matrix4d modelViewMatrix;
    
    private final Matrix4d viewMatrix;

    public Transformation() {
        projectionMatrix = new Matrix4d();
        modelViewMatrix = new Matrix4d();
        viewMatrix = new Matrix4d();
    }

    public final Matrix4d getProjectionMatrix(float fov, float width, float height, float zNear, float zFar) {
        float aspectRatio = width / height;        
        projectionMatrix.identity();
        projectionMatrix.perspective(fov, aspectRatio, zNear, zFar);
        return projectionMatrix;
    }
    
    public Matrix4d getViewMatrix(Camera camera) {
        Vector3d cameraPos = camera.getPosition();
        Vector3d rotation = camera.getRotation();
        
        viewMatrix.identity();
        // First do the rotation so camera rotates over its position
        viewMatrix.rotate((float)Math.toRadians(rotation.x), new Vector3d(1, 0, 0))
                .rotate((float)Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        // Then do the translation
        viewMatrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        return viewMatrix;
    }

    public Matrix4d getModelViewMatrix(GameItem gameItem, Matrix4d viewMatrix) {
        Vector3d rotation = gameItem.getRotation();
        modelViewMatrix.identity().translate(gameItem.getPosition()).
                rotateX((float)Math.toRadians(-rotation.x)).
                rotateY((float)Math.toRadians(-rotation.y)).
                rotateZ((float)Math.toRadians(-rotation.z)).
                scale(gameItem.getScale());
        Matrix4d viewCurr = new Matrix4d(viewMatrix);
        return viewCurr.mul(modelViewMatrix);
    }
}