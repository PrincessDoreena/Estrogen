package dev.mayaqq.estrogen.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.mayaqq.estrogen.Estrogen;
import dev.mayaqq.estrogen.datagen.tags.EstrogenTags;
import dev.mayaqq.estrogen.networking.EstrogenC2S;
import dev.mayaqq.estrogen.registry.common.EstrogenEffects;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import static dev.mayaqq.estrogen.registry.client.EstrogenKeybinds.dashKey;

public class Dash {

    public static boolean uwufy = false;
    public static boolean uwufyHotbar = false;
    private static int tick = 0;

    private static final Identifier DASH_OVERLAY = new Identifier("textures/misc/nausea.png");
    public static int dashCooldown = 0;
    public static int groundCooldown = 0;
    public static boolean onCooldown = false;
    public static short maxDashes = 0;
    public static short currentDashes = 0;
    private static boolean shouldWaveDash = false;
    private static BlockPos lastPos = null;
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            // UwU
            tick++;
            if (tick == 20) {
                tick = 0;
                uwufy = player.getInventory().contains(EstrogenTags.ItemTags.UWUFYING);
                boolean turnoff = true;
                for (int i = 0; i < 9; i++) {
                    if (player.getInventory().getStack(i).getItem().getDefaultStack().isIn(EstrogenTags.ItemTags.UWUFYING)) {
                        uwufyHotbar = true;
                        turnoff = false;
                        break;
                    }
                }
                if (turnoff) uwufyHotbar = false;
            }

            // Dash
            dashCooldown--;
            groundCooldown--;
            if (dashCooldown < 0) dashCooldown = 0;
            if (groundCooldown < 0) groundCooldown = 0;
            if (!player.hasStatusEffect(EstrogenEffects.ESTROGEN_EFFECT)) {
                maxDashes = 0;
                currentDashes = 0;
                onCooldown = false;
                dashCooldown = 0;
                return;
            }

            if (dashCooldown > 0 && dashCooldown % 2 == 0 && player.getBlockPos() != lastPos) {
                ClientPlayNetworking.send(EstrogenC2S.DASH_PARTICLES, PacketByteBufs.empty());
            }
            lastPos = player.getBlockPos();

            if (dashCooldown > 0 && shouldWaveDash && client.options.jumpKey.isPressed()) {
                player.setVelocity(player.getRotationVector().x * 3, 1, player.getRotationVector().z * 3);
                shouldWaveDash = false;
            }

            if (shouldRefreshDash(player) && groundCooldown == 0) {
                groundCooldown = 4;
                currentDashes = maxDashes;
            }
            onCooldown = dashCooldown > 0 || currentDashes == 0;
            if (dashKey.wasPressed() && player.hasStatusEffect(EstrogenEffects.ESTROGEN_EFFECT) && !onCooldown) {
                if (player.getPitch() > 50 && player.getPitch() < 90) {
                    shouldWaveDash = true;
                }
                dashCooldown = 10;
                currentDashes--;
                player.setVelocity(player.getRotationVector().x * 2, player.getRotationVector().y * 2, player.getRotationVector().z * 2);
                ClientPlayNetworking.send(Estrogen.id("dash"), PacketByteBufs.empty());
            }
        });
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            if (onCooldown) {
                renderOverLayer(0.3F, 0.5F, 0.8F);
            }
            if (uwufyHotbar) {
                renderOverLayer(0.9F, 0.6F, 0.7F);
            }
        });
    }

    private static void renderOverLayer(float f, float g, float h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float distortionStrength = 0.5F;
        int i = mc.getWindow().getScaledWidth();
        int j = mc.getWindow().getScaledHeight();
        double d = MathHelper.lerp(distortionStrength, 2.0, 1.0);
        double e = (double)i * d;
        double k = (double)j * d;
        double l = ((double)i - e) / 2.0;
        double m = ((double)j - k) / 2.0;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderColor(f, g, h, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, DASH_OVERLAY);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(l, m + k, -90.0).texture(0.0F, 1.0F).next();
        bufferBuilder.vertex(l + e, m + k, -90.0).texture(1.0F, 1.0F).next();
        bufferBuilder.vertex(l + e, m, -90.0).texture(1.0F, 0.0F).next();
        bufferBuilder.vertex(l, m, -90.0).texture(0.0F, 0.0F).next();
        tessellator.draw();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static Boolean shouldRefreshDash(ClientPlayerEntity player) {
        return player.isOnGround() || player.getWorld().getBlockState(player.getBlockPos()).getBlock() instanceof FluidBlock;
    }
}