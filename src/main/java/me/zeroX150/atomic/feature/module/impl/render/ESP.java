package me.zeroX150.atomic.feature.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.zeroX150.atomic.Atomic;
import me.zeroX150.atomic.feature.module.Module;
import me.zeroX150.atomic.feature.module.ModuleType;
import me.zeroX150.atomic.feature.module.config.BooleanValue;
import me.zeroX150.atomic.feature.module.config.MultiValue;
import me.zeroX150.atomic.feature.module.config.SliderValue;
import me.zeroX150.atomic.helper.Friends;
import me.zeroX150.atomic.helper.Utils;
import me.zeroX150.atomic.helper.render.Renderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ESP extends Module {
    public final MultiValue outlineMode = this.config.create("Outline mode", "Filled", "Filled", "2D", "Outline", "Shader");
    final BooleanValue entities = (BooleanValue) this.config.create("Show Entities", false).description("Whether or not to show entities");
    final BooleanValue players = (BooleanValue) this.config.create("Show Players", true).description("Whether or not to show players");
    final SliderValue range = this.config.create("Range", 64, 32, 128, 0);

    public ESP() {
        super("ESP", "shows where shit is but its the walmart version", ModuleType.RENDER);
    }

    @Override
    public void tick() {

    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public String getContext() {
        return null;
    }

    public boolean shouldRenderEntity(Entity e) {
        return ((e instanceof PlayerEntity && players.getValue()) || entities.getValue());
    }

    @Override
    public void onWorldRender(MatrixStack matrices) {
        if (Atomic.client.world == null || Atomic.client.player == null) return;
        for (Entity entity : Atomic.client.world.getEntities()) {
            if (entity.squaredDistanceTo(Atomic.client.player) > Math.pow(range.getValue(), 2)) continue;
            if (entity.getUuid().equals(Atomic.client.player.getUuid())) continue;
            if (shouldRenderEntity(entity)) {
                Color c = Utils.getCurrentRGB();
                if (entity instanceof PlayerEntity pe && Friends.isAFriend(pe)) c = new Color(100, 255, 20);
                Vec3d eSource = new Vec3d(MathHelper.lerp(Atomic.client.getTickDelta(), entity.prevX, entity.getX()),
                        MathHelper.lerp(Atomic.client.getTickDelta(), entity.prevY, entity.getY()),
                        MathHelper.lerp(Atomic.client.getTickDelta(), entity.prevZ, entity.getZ()));
                switch (outlineMode.getIndex()) {
                    case 0 -> Renderer.renderFilled(eSource.subtract(new Vec3d(entity.getWidth(), 0, entity.getWidth()).multiply(0.5)), new Vec3d(entity.getWidth(), entity.getHeight(), entity.getWidth()), Renderer.modify(c, -1, -1, -1, 130), matrices);
                    case 1 -> renderOutline(entity, c, matrices);
                    case 2 -> Renderer.renderOutline(eSource.subtract(new Vec3d(entity.getWidth(), 0, entity.getWidth()).multiply(0.5)), new Vec3d(entity.getWidth(), entity.getHeight(), entity.getWidth()), Renderer.modify(c, -1, -1, -1, 130), matrices);
                }
            }
        }
    }

    @Override
    public void onHudRender() {

    }

    void renderOutline(Entity e, Color color, MatrixStack stack) {
        Vec3d eSource = new Vec3d(MathHelper.lerp(Atomic.client.getTickDelta(), e.prevX, e.getX()),
                MathHelper.lerp(Atomic.client.getTickDelta(), e.prevY, e.getY()),
                MathHelper.lerp(Atomic.client.getTickDelta(), e.prevZ, e.getZ()));
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        Camera c = Atomic.client.gameRenderer.getCamera();
        Vec3d camPos = c.getPos();
        Vec3d start = eSource.subtract(camPos);
        float x = (float) start.x;
        float y = (float) start.y;
        float z = (float) start.z;

        double r = Math.toRadians(-c.getYaw() + 90);
        float sin = (float) (Math.sin(r) * (e.getWidth() / 1.7));
        float cos = (float) (Math.cos(r) * (e.getWidth() / 1.7));
        stack.push();

        Matrix4f matrix = stack.peek().getModel();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, x + sin, y, z + cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x - sin, y, z - cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x - sin, y, z - cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x - sin, y + e.getHeight(), z - cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x - sin, y + e.getHeight(), z - cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x + sin, y + e.getHeight(), z + cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x + sin, y + e.getHeight(), z + cos).color(red, green, blue, alpha).next();
        buffer.vertex(matrix, x + sin, y, z + cos).color(red, green, blue, alpha).next();

        buffer.end();

        BufferRenderer.draw(buffer);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        stack.pop();
    }
}

