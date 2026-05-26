package net.fretux.mindmotion.client.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fretux.mindmotion.ConfigMM;
import net.fretux.mindmotion.client.ClientData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "mindmotion", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShadowFigureHandler {
    private static final int MAX_FIGURES = 3;
    private static final int MIN_SPAWN_DELAY = 120;
    private static final int RANDOM_SPAWN_DELAY = 180;
    private static final double MIN_SPAWN_DISTANCE = 11.0;
    private static final double MAX_SPAWN_DISTANCE = 22.0;
    private static final double APPROACH_DISTANCE_SQR = 5.0 * 5.0;
    private static final double LOOK_DOT_THRESHOLD = 0.94;
    private static final int FADE_TICKS = 24;

    private static final List<ShadowFigure> FIGURES = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int spawnCooldown = MIN_SPAWN_DELAY;

    private ShadowFigureHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || ConfigMM.CLIENT.DISABLE_SHADERS.get()) {
            FIGURES.clear();
            spawnCooldown = MIN_SPAWN_DELAY;
            return;
        }

        boolean insane = ClientData.SANITY <= 0f;
        if (!insane) {
            FIGURES.clear();
            spawnCooldown = MIN_SPAWN_DELAY;
            return;
        }

        tickFigures(player);

        if (--spawnCooldown <= 0) {
            if (FIGURES.size() < MAX_FIGURES) {
                trySpawnFigure(player);
            }
            spawnCooldown = MIN_SPAWN_DELAY + RANDOM.nextInt(RANDOM_SPAWN_DELAY + 1);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FIGURES.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || ConfigMM.CLIENT.DISABLE_SHADERS.get()) return;

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

        for (ShadowFigure figure : FIGURES) {
            renderFigure(figure, poseStack, consumer, cameraPos, event.getPartialTick());
        }

        bufferSource.endBatch(RenderType.debugQuads());
    }

    private static void tickFigures(LocalPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        Iterator<ShadowFigure> iterator = FIGURES.iterator();
        while (iterator.hasNext()) {
            ShadowFigure figure = iterator.next();
            figure.age++;

            double distanceSqr = player.distanceToSqr(figure.position);
            Vec3 toFigure = figure.position.add(0.0, 1.35, 0.0).subtract(eyePos).normalize();
            boolean lookedAt = look.dot(toFigure) > LOOK_DOT_THRESHOLD && distanceSqr < MAX_SPAWN_DISTANCE * MAX_SPAWN_DISTANCE;
            boolean approached = distanceSqr < APPROACH_DISTANCE_SQR;

            if (lookedAt || approached || figure.age > figure.lifeTicks) {
                figure.fading = true;
            }

            if (figure.fading) {
                figure.fadeTicks++;
                if (figure.fadeTicks >= FADE_TICKS) {
                    iterator.remove();
                }
            }
        }
    }

    private static void trySpawnFigure(LocalPlayer player) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double distance = Mth.lerp(RANDOM.nextDouble(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            Vec3 candidate = findStandingPosition(player, x, z);
            if (candidate == null || player.distanceToSqr(candidate) < MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE) {
                continue;
            }

            Vec3 toCandidate = candidate.subtract(player.getEyePosition()).normalize();
            if (player.getLookAngle().normalize().dot(toCandidate) > 0.55) {
                continue;
            }

            FIGURES.add(new ShadowFigure(candidate, 180 + RANDOM.nextInt(141), RANDOM.nextFloat() * 360f));
            return;
        }
    }

    private static Vec3 findStandingPosition(LocalPlayer player, double x, double z) {
        if (player.clientLevel == null) return null;

        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int startY = Mth.floor(player.getY()) + 4;
        int minY = Math.max(player.clientLevel.getMinBuildHeight() + 1, Mth.floor(player.getY()) - 8);

        for (int y = startY; y >= minY; y--) {
            BlockPos feet = new BlockPos(blockX, y, blockZ);
            BlockPos floor = feet.below();
            boolean floorSolid = player.clientLevel.getBlockState(floor).isFaceSturdy(player.clientLevel, floor, Direction.UP);
            boolean bodyClear = player.clientLevel.getBlockState(feet).isAir() && player.clientLevel.getBlockState(feet.above()).isAir();
            if (floorSolid && bodyClear) {
                return Vec3.atBottomCenterOf(feet);
            }
        }

        return null;
    }

    private static void renderFigure(ShadowFigure figure, PoseStack poseStack, VertexConsumer consumer, Vec3 cameraPos, float partialTick) {
        float appear = Mth.clamp((figure.age + partialTick) / 18f, 0f, 1f);
        float fade = figure.fading ? 1f - Mth.clamp((figure.fadeTicks + partialTick) / FADE_TICKS, 0f, 1f) : 1f;
        float flicker = 0.82f + 0.18f * Mth.sin((figure.age + partialTick) * 0.37f + figure.seedYaw);
        float alpha = 0.58f * appear * fade * flicker;
        if (alpha <= 0.01f) return;

        poseStack.pushPose();
        poseStack.translate(figure.position.x - cameraPos.x, figure.position.y - cameraPos.y, figure.position.z - cameraPos.z);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-figure.seedYaw));

        Matrix4f matrix = poseStack.last().pose();
        drawQuad(matrix, consumer, -0.34f, 0.15f, 0.0f, 0.34f, 1.62f, 0.0f, alpha);
        drawQuad(matrix, consumer, 0.0f, 0.15f, -0.34f, 0.0f, 1.62f, 0.34f, alpha * 0.78f);
        drawQuad(matrix, consumer, -0.22f, 1.58f, 0.0f, 0.22f, 2.05f, 0.0f, alpha * 0.9f);

        poseStack.popPose();
    }

    private static void drawQuad(Matrix4f matrix, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float alpha) {
        consumer.vertex(matrix, minX, minY, minZ).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        consumer.vertex(matrix, maxX, minY, maxZ).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        consumer.vertex(matrix, minX, maxY, minZ).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
    }

    private static final class ShadowFigure {
        private final Vec3 position;
        private final int lifeTicks;
        private final float seedYaw;
        private int age;
        private int fadeTicks;
        private boolean fading;

        private ShadowFigure(Vec3 position, int lifeTicks, float seedYaw) {
            this.position = position;
            this.lifeTicks = lifeTicks;
            this.seedYaw = seedYaw;
        }
    }
}
