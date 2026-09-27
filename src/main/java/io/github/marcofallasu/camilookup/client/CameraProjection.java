package io.github.marcofallasu.camilookup.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Converts between screen positions and world directions using the camera of the current frame, so the world can be
 * picked from the free cursor and pinned boxes can follow their targets.
 */
public final class CameraProjection {
    private static float fov = 70.0F;

    private final Vec3 cameraPos;
    private final Matrix4f viewProjection;
    private final Matrix4f inverse;

    private CameraProjection(Vec3 cameraPos, Matrix4f viewProjection) {
        this.cameraPos = cameraPos;
        this.viewProjection = viewProjection;
        this.inverse = new Matrix4f(viewProjection).invert();
    }

    /** Keeps the field of view the world was rendered with (after every modifier). */
    static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            fov = event.getFOV();
        }
    }

    public static @Nullable CameraProjection current() {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return null;
        }
        Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix(fov);
        Matrix4f view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        return new CameraProjection(camera.position(), projection.mul(view));
    }

    public Vec3 cameraPos() {
        return cameraPos;
    }

    /** World direction under a point of the window, in window pixels. */
    public Vec3 directionAt(double windowX, double windowY) {
        Window window = Minecraft.getInstance().getWindow();
        float ndcX = (float) (2.0 * windowX / window.getScreenWidth() - 1.0);
        float ndcY = (float) (1.0 - 2.0 * windowY / window.getScreenHeight());
        Vector3f near = inverse.transformProject(ndcX, ndcY, -1.0F, new Vector3f());
        Vector3f far = inverse.transformProject(ndcX, ndcY, 1.0F, new Vector3f());
        Vector3f direction = far.sub(near).normalize();
        return new Vec3(direction.x, direction.y, direction.z);
    }

    /** Projects a world point to GUI coordinates, or returns {@code null} if it is behind the camera. */
    public double @Nullable [] toGui(Vec3 point) {
        Vector4f clip = viewProjection.transform(new Vector4f(
                (float) (point.x - cameraPos.x), (float) (point.y - cameraPos.y), (float) (point.z - cameraPos.z), 1.0F));
        if (clip.w <= 0.05F) {
            return null;
        }
        Window window = Minecraft.getInstance().getWindow();
        double x = (clip.x / clip.w + 1.0) / 2.0 * window.getGuiScaledWidth();
        double y = (1.0 - clip.y / clip.w) / 2.0 * window.getGuiScaledHeight();
        return new double[]{x, y};
    }
}
