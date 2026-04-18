package com.example.cosmetics.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Custom render types. Extends RenderType so we can access the protected
 * State constants (TRANSLUCENT_TRANSPARENCY, NO_CULL, etc.) in 1.16.5.
 */
public final class ModRenderTypes extends RenderType {

    // Required dummy constructor — RenderType has no no-arg constructor.
    private ModRenderTypes(String n, net.minecraft.client.renderer.vertex.VertexFormat fmt,
                           int mode, int bufSize, boolean delegate, boolean sorted,
                           Runnable on, Runnable off) {
        super(n, fmt, mode, bufSize, delegate, sorted, on, off);
    }

    /** Untextured translucent colored quads (for the hat, wings, trails). */
    public static final RenderType COLOR_QUADS = create(
            "cosmeticsmod_color_quads",
            DefaultVertexFormats.POSITION_COLOR,
            GL11.GL_QUADS, 1024, false, true,
            RenderType.State.builder()
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    /** Additive-blend translucent colored quads (for glowing trails/wings). */
    public static final RenderType GLOW_QUADS = create(
            "cosmeticsmod_glow_quads",
            DefaultVertexFormats.POSITION_COLOR,
            GL11.GL_QUADS, 1024, false, true,
            RenderType.State.builder()
                    .setTransparencyState(LIGHTNING_TRANSPARENCY) // additive
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );
}
