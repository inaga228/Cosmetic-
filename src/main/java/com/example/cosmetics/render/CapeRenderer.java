package com.example.cosmetics.render;

import com.example.cosmetics.client.CosmeticsState;
import com.example.cosmetics.feature.FeatureSettings;
import com.example.cosmetics.feature.FeatureType;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/**
 * Cape cosmetic — a cloth cape on the player's back with real physics.
 *
 * Physics model: a 5×7 grid of verlet-integration nodes.
 * The top row is pinned to the player's shoulder blade positions.
 * Every tick gravity and drag are applied; nodes are constrained to
 * their rest-length from each neighbour.  The result is a cape that
 * swings, billows when running, and settles when standing still.
 *
 * Styles:
 *   0 = FLAT    — solid colour, smooth gradient at bottom
 *   1 = SPLIT   — two-colour left/right split (accent + darker)
 *   2 = GRADIENT— top-to-bottom gradient from user colour to black
 *   3 = CROSS   — decorative cross emblem on the back
 *   4 = WINGS   — wide ornate cape with side flares
 */
public final class CapeRenderer {

    public static final int STYLE_COUNT = 5;
    public static final String[] STYLE_NAMES = { "Flat", "Split", "Gradient", "Cross", "Wings" };

    // Grid dimensions: COLS x ROWS (cols = horizontal, rows = vertical down)
    private static final int COLS = 6;
    private static final int ROWS = 8;
    private static final int N    = COLS * ROWS;

    // Physics nodes: position (x,y,z) + previous position
    private final float[] nx  = new float[N];
    private final float[] ny  = new float[N];
    private final float[] nz  = new float[N];
    private final float[] pnx = new float[N];
    private final float[] pny = new float[N];
    private final float[] pnz = new float[N];

    private boolean initialised = false;

    // Rest lengths (computed once)
    private float restH; // horizontal rest length between adjacent column nodes
    private float restV; // vertical rest length between adjacent row nodes

    private static final CapeRenderer INSTANCE = new CapeRenderer();
    public static CapeRenderer get() { return INSTANCE; }

    // Cape world anchor from last tick (for initialisation)
    private double lastAnchorX, lastAnchorY, lastAnchorZ;

    private CapeRenderer() {}

    // =========================================================================
    // Tick — called every client tick to advance the simulation
    // =========================================================================
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!CosmeticsState.get().isOn(FeatureType.CAPE)) return;

        ClientPlayerEntity player = mc.player;
        FeatureSettings fs = CosmeticsState.get().settings(FeatureType.CAPE);

        float size = Math.max(0.2F, fs.size);
        restH = 0.22F * size;
        restV = 0.22F * size;

        // Anchor: upper back of player (world coords)
        double ax = player.getX();
        double ay = player.getY() + 1.42; // shoulder blade height
        double az = player.getZ();

        float yawRad = (float) Math.toRadians(player.yBodyRot);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        if (!initialised) {
            initNodes(ax, ay, az, sinYaw, cosYaw, size);
            initialised = true;
        }

        // ---- Verlet integration ----
        float gravity  = 0.006F;
        float drag     = 0.98F;
        float wind     = 0F;

        // Speed-based wind: player velocity pushes cape backward
        double vx = player.getX() - player.xo;
        double vz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(vx * vx + vz * vz);
        // Wind pushes in player's forward direction (behind them = cape backward)
        float windX = sinYaw * speed * 1.8F * fs.speed;
        float windZ = -cosYaw * speed * 1.8F * fs.speed;

        for (int i = 0; i < N; i++) {
            float vvx = (nx[i] - pnx[i]) * drag;
            float vvy = (ny[i] - pny[i]) * drag;
            float vvz = (nz[i] - pnz[i]) * drag;

            pnx[i] = nx[i];
            pny[i] = ny[i];
            pnz[i] = nz[i];

            nx[i] += vvx + windX;
            ny[i] += vvy - gravity;
            nz[i] += vvz + windZ;
        }

        // ---- Pin top row to player back ----
        for (int col = 0; col < COLS; col++) {
            float t = col / (float)(COLS - 1); // 0..1 across width
            float offX = (t - 0.5F) * restH * (COLS - 1);
            // Rotate offset to player facing
            float wx = (float)(ax + offX * cosYaw);
            float wy = (float) ay;
            float wz = (float)(az - offX * sinYaw + 0.12F * size); // slight back offset
            int idx = col; // row 0
            // Move pinned nodes — also update previous to avoid velocity spike
            float dx = wx - nx[idx];
            float dz = wz - nz[idx];
            pnx[idx] += dx; nx[idx] = wx;
            pny[idx] += (wy - ny[idx]); ny[idx] = wy;
            pnz[idx] += dz; nz[idx] = wz;
        }

        // ---- Distance constraints (relaxation passes) ----
        int passes = 4;
        for (int pass = 0; pass < passes; pass++) {
            // Horizontal links
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS - 1; col++) {
                    constrain(row * COLS + col, row * COLS + col + 1, restH, row == 0);
                }
            }
            // Vertical links
            for (int row = 0; row < ROWS - 1; row++) {
                for (int col = 0; col < COLS; col++) {
                    constrain(row * COLS + col, (row + 1) * COLS + col, restV, row == 0);
                }
            }
        }

        lastAnchorX = ax; lastAnchorY = ay; lastAnchorZ = az;
    }

    private void initNodes(double ax, double ay, double az,
                            float sinYaw, float cosYaw, float size) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float t   = col / (float)(COLS - 1);
                float offX = (t - 0.5F) * restH * (COLS - 1);
                int idx   = row * COLS + col;
                nx[idx]  = pnx[idx] = (float)(ax + offX * cosYaw);
                ny[idx]  = pny[idx] = (float)(ay - row * restV);
                nz[idx]  = pnz[idx] = (float)(az - offX * sinYaw + 0.12F * size + row * 0.02F);
            }
        }
    }

    private void constrain(int a, int b, float rest, boolean aFixed) {
        float dx = nx[b] - nx[a];
        float dy = ny[b] - ny[a];
        float dz = nz[b] - nz[a];
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (dist < 1e-5F) return;
        float diff = (dist - rest) / dist * 0.5F;
        if (!aFixed) {
            nx[a] += dx * diff; ny[a] += dy * diff; nz[a] += dz * diff;
        }
        nx[b] -= dx * diff; ny[b] -= dy * diff; nz[b] -= dz * diff;
    }

    // =========================================================================
    // Render — called from RenderWorldLastEvent
    // =========================================================================
    public static void render(MatrixStack ms, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        CosmeticsState state = CosmeticsState.get();
        if (!state.isOn(FeatureType.CAPE)) return;
        if (HatRenderer.isFirstPerson(mc)) return;

        CapeRenderer cape = get();
        if (!cape.initialised) return;

        FeatureSettings fs = state.settings(FeatureType.CAPE);
        int style = Math.floorMod(fs.style, STYLE_COUNT);
        boolean glow = style == 3;

        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();
        IVertexBuilder vb = buf.getBuffer(glow ? ModRenderTypes.GLOW_QUADS : ModRenderTypes.COLOR_QUADS);

        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();

        int r = clamp255((int)(fs.colorR * 255));
        int g = clamp255((int)(fs.colorG * 255));
        int b = clamp255((int)(fs.colorB * 255));

        ms.pushPose();
        ms.translate(-cam.x, -cam.y, -cam.z);

        Matrix4f pose = ms.last().pose();

        // Draw quads between grid nodes
        for (int row = 0; row < ROWS - 1; row++) {
            for (int col = 0; col < COLS - 1; col++) {
                int i00 = row       * COLS + col;
                int i10 = row       * COLS + col + 1;
                int i01 = (row + 1) * COLS + col;
                int i11 = (row + 1) * COLS + col + 1;

                float u = col / (float)(COLS - 1);
                float v = row / (float)(ROWS - 1);

                int cr, cg, cb, ca;
                ca = 210;

                switch (style) {
                    case 1: { // SPLIT
                        boolean left = u < 0.5F;
                        cr = left ? r : clamp255(r / 2);
                        cg = left ? g : clamp255(g / 2);
                        cb = left ? b : clamp255(b / 2);
                        break;
                    }
                    case 2: { // GRADIENT top=user col, bottom=dark
                        float fade = 1F - v * 0.85F;
                        cr = clamp255((int)(r * fade));
                        cg = clamp255((int)(g * fade));
                        cb = clamp255((int)(b * fade));
                        ca = clamp255((int)(220 - v * 80));
                        break;
                    }
                    case 3: { // CROSS — base colour + bright cross overlay
                        boolean onCross = (Math.abs(u - 0.5F) < 0.13F && v > 0.1F && v < 0.9F)
                                       || (Math.abs(v - 0.45F) < 0.10F && u > 0.1F && u < 0.9F);
                        if (onCross) {
                            cr = Math.min(255, r + 80);
                            cg = Math.min(255, g + 80);
                            cb = Math.min(255, b + 80);
                            ca = 255;
                        } else {
                            cr = r; cg = g; cb = b;
                        }
                        break;
                    }
                    case 4: { // WINGS — wider with v-shaped side flares (via colour)
                        float distFromCenter = Math.abs(u - 0.5F) * 2F;
                        float vFade = 1F - v * 0.7F;
                        float eFade = 1F - distFromCenter * 0.6F;
                        cr = clamp255((int)(r * eFade));
                        cg = clamp255((int)(g * eFade));
                        cb = clamp255((int)(b * eFade));
                        ca = clamp255((int)(210 * vFade));
                        break;
                    }
                    default: { // FLAT
                        float fade = 1F - v * 0.3F;
                        cr = clamp255((int)(r * fade));
                        cg = clamp255((int)(g * fade));
                        cb = clamp255((int)(b * fade));
                        break;
                    }
                }

                quad(vb, pose,
                        cape.nx[i00], cape.ny[i00], cape.nz[i00],
                        cape.nx[i10], cape.ny[i10], cape.nz[i10],
                        cape.nx[i11], cape.ny[i11], cape.nz[i11],
                        cape.nx[i01], cape.ny[i01], cape.nz[i01],
                        cr, cg, cb, ca);
            }
        }

        ms.popPose();
        buf.endBatch(ModRenderTypes.COLOR_QUADS);
        buf.endBatch(ModRenderTypes.GLOW_QUADS);
    }

    private static void quad(IVertexBuilder vb, Matrix4f pose,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              int r, int g, int b, int a) {
        vb.vertex(pose, x0, y0, z0).color(r, g, b, a).endVertex();
        vb.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        vb.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
        // Back face (reverse winding)
        vb.vertex(pose, x3, y3, z3).color(r, g, b, a).endVertex();
        vb.vertex(pose, x2, y2, z2).color(r, g, b, a).endVertex();
        vb.vertex(pose, x1, y1, z1).color(r, g, b, a).endVertex();
        vb.vertex(pose, x0, y0, z0).color(r, g, b, a).endVertex();
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
}
