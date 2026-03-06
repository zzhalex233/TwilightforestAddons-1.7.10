package com.twilightforestaddons.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.storage.MapData.MapCoord;

import org.lwjgl.opengl.GL11;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer;
import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.map.AdvancedMagicMapDataUtils;
import com.twilightforestaddons.map.BossFeatureRegistry;
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;
import twilightforest.client.renderer.TFMagicMapRenderer;

@SideOnly(Side.CLIENT)
public class GuiAdvancedMagicMap extends GuiScreen {

    private static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final int MAP_FRAME_PADDING = 7;

    private final int mapId;
    private TFMagicMapRenderer renderer;

    private int mapSize;
    private int mapLeft;
    private int mapTop;

    private double displayCenterX;
    private double displayCenterZ;
    private double mapCenterX;
    private double mapCenterZ;
    private float renderMapLeft;
    private float renderMapTop;

    public GuiAdvancedMagicMap(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public void initGui() {
        if (this.renderer == null) {
            this.renderer = new AdvancedMagicMapItemRenderer(this.mc.gameSettings, this.mc.getTextureManager());
        }
        this.allowUserInput = true;
        this.updateMapLayout();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!this.ensurePlayerStillHoldingMap()) {
            return;
        }

        TFMagicMapData mapData = this.getMapData();
        this.drawDefaultBackground();
        this.updateMapLayout();

        if (mapData == null) {
            drawCenteredString(
                this.fontRendererObj,
                StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.loading"),
                this.width / 2,
                this.height / 2,
                0xFFFFFF);
            return;
        }

        this.updateRenderCenters(mapData);
        List<MapCoord> displayedFeatures = AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData);

        this.drawMapBackground(
            this.mapLeft - MAP_FRAME_PADDING,
            this.mapTop - MAP_FRAME_PADDING,
            this.mapSize + MAP_FRAME_PADDING * 2);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        GL11.glTranslatef(this.renderMapLeft, this.renderMapTop, 0.0F);
        float scale = this.mapSize / 128.0F;
        GL11.glScalef(scale, scale, 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderer.renderMap(this.mc.thePlayer, this.mc.getTextureManager(), mapData);
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.title"),
            this.width / 2,
            this.mapTop - 18,
            0xFFFFFF);
        drawCenteredString(
            this.fontRendererObj,
            StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.hint"),
            this.width / 2,
            this.mapTop + this.mapSize + 10,
            0xCCCCCC);

        int infoX = this.mapLeft;
        int infoY = this.mapTop - 46;
        drawString(
            this.fontRendererObj,
            StatCollector.translateToLocalFormatted(
                "gui.twilightforestaddons.advanced_map.center",
                MathHelper.floor_double(this.displayCenterX),
                MathHelper.floor_double(this.displayCenterZ)),
            infoX,
            infoY,
            0xEAEAEA);
        drawString(
            this.fontRendererObj,
            StatCollector
                .translateToLocalFormatted("gui.twilightforestaddons.advanced_map.facing", this.getFacingLabel()),
            infoX,
            infoY + 12,
            0xEAEAEA);

        if (this.isInsideMap(mouseX, mouseY)) {
            int hoverWorldX = this.mapPixelToWorldX(mouseX, mapData);
            int hoverWorldZ = this.mapPixelToWorldZ(mouseY, mapData);
            drawString(
                this.fontRendererObj,
                StatCollector.translateToLocalFormatted(
                    "gui.twilightforestaddons.advanced_map.cursor",
                    hoverWorldX,
                    hoverWorldZ),
                infoX,
                infoY + 24,
                0x8FE0FF);
        }

        MapCoord hovered = this.findHoveredBossFeature(mouseX, mouseY, displayedFeatures);
        if (hovered != null) {
            String featureName = BossFeatureRegistry.getFeatureName(hovered.iconSize);
            drawString(
                this.fontRendererObj,
                StatCollector.translateToLocalFormatted("gui.twilightforestaddons.advanced_map.target", featureName),
                infoX,
                infoY + 36,
                0xFFD35A);

            if (Config.advancedMapEnforceProgression
                && !BossFeatureRegistry.isFeatureUnlocked(hovered.iconSize, this.mc.thePlayer)) {
                drawString(
                    this.fontRendererObj,
                    StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.locked"),
                    infoX,
                    infoY + 48,
                    0xFF6666);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1) {
            this.mc.displayGuiScreen(null);
            return;
        }

        TFMagicMapData mapData = this.getMapData();
        if (mapData != null && mouseButton == 0) {
            MapCoord hovered = this.findHoveredBossFeature(
                mouseX,
                mouseY,
                AdvancedMagicMapDataUtils.createDedupedFeatureSnapshot(mapData));
            if (hovered != null) {
                ModNetwork.CHANNEL.sendToServer(
                    new PacketTeleportToBossFeature(
                        this.mapId,
                        hovered.iconSize,
                        hovered.centerX,
                        hovered.centerZ,
                        mapData.xCenter,
                        mapData.zCenter,
                        mapData.scale));
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private TFMagicMapData getMapData() {
        if (this.mc.theWorld == null || this.mc.thePlayer == null) {
            return null;
        }
        ItemStack held = this.mc.thePlayer.getCurrentEquippedItem();
        if (held == null || held.getItem() != ModItems.advancedMagicMap || held.getItemDamage() != this.mapId) {
            return null;
        }
        return ModItems.advancedMagicMap.getMapData(held, this.mc.theWorld);
    }

    private void updateMapLayout() {
        int suggestedSize = Math.min(this.width - 120, this.height - 90);
        this.mapSize = Math.max(260, Math.min(430, suggestedSize));
        this.mapLeft = (this.width - this.mapSize) / 2;
        this.mapTop = Math.max(62, (this.height - this.mapSize) / 2 - 6);

        int maxBottom = this.height - 70;
        if (this.mapTop + this.mapSize > maxBottom) {
            this.mapTop = Math.max(62, maxBottom - this.mapSize);
        }
    }

    private boolean ensurePlayerStillHoldingMap() {
        if (this.mc.thePlayer == null) {
            this.mc.displayGuiScreen(null);
            return false;
        }

        ItemStack heldStack = this.mc.thePlayer.getCurrentEquippedItem();
        if (heldStack == null || heldStack.getItem() != ModItems.advancedMagicMap
            || heldStack.getItemDamage() != this.mapId) {
            this.mc.displayGuiScreen(null);
            return false;
        }
        return true;
    }

    private MapCoord findHoveredBossFeature(int mouseX, int mouseY, List<MapCoord> features) {
        float mapScale = this.mapSize / 128.0F;
        float iconHalfSize = 4.0F * mapScale;

        for (MapCoord coord : features) {
            if (!BossFeatureRegistry.isBossFeature(coord.iconSize)) {
                continue;
            }

            float iconCenterX = this.renderMapLeft + (coord.centerX / 2.0F + 64.0F) * mapScale;
            float iconCenterY = this.renderMapTop + (coord.centerZ / 2.0F + 64.0F) * mapScale;

            if (Math.abs(mouseX - iconCenterX) <= iconHalfSize && Math.abs(mouseY - iconCenterY) <= iconHalfSize) {
                return coord;
            }
        }

        return null;
    }

    private boolean isInsideMap(int mouseX, int mouseY) {
        return mouseX >= this.mapLeft && mouseX <= this.mapLeft + this.mapSize
            && mouseY >= this.mapTop
            && mouseY <= this.mapTop + this.mapSize;
    }

    private int mapPixelToWorldX(int mouseX, TFMagicMapData mapData) {
        float mapScale = this.mapSize / 128.0F;
        float pixelX = (mouseX - this.renderMapLeft) / mapScale;
        int offset = MathHelper.floor_float(pixelX - 64.0F);
        int blocksPerPixel = 1 << mapData.scale;
        return MathHelper.floor_double(this.mapCenterX + offset * blocksPerPixel);
    }

    private int mapPixelToWorldZ(int mouseY, TFMagicMapData mapData) {
        float mapScale = this.mapSize / 128.0F;
        float pixelZ = (mouseY - this.renderMapTop) / mapScale;
        int offset = MathHelper.floor_float(pixelZ - 64.0F);
        int blocksPerPixel = 1 << mapData.scale;
        return MathHelper.floor_double(this.mapCenterZ + offset * blocksPerPixel);
    }

    private String getFacingLabel() {
        if (this.mc.thePlayer == null) {
            return "";
        }

        int dir = MathHelper.floor_double(this.mc.thePlayer.rotationYaw * 4.0D / 360.0D + 0.5D) & 3;
        switch (dir) {
            case 0:
                return StatCollector.translateToLocal("gui.twilightforestaddons.direction.south");
            case 1:
                return StatCollector.translateToLocal("gui.twilightforestaddons.direction.west");
            case 2:
                return StatCollector.translateToLocal("gui.twilightforestaddons.direction.north");
            default:
                return StatCollector.translateToLocal("gui.twilightforestaddons.direction.east");
        }
    }

    private void drawMapBackground(int left, int top, int size) {
        this.mc.getTextureManager()
            .bindTexture(MAP_BACKGROUND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(left, top + size, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(left + size, top + size, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(left + size, top, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(left, top, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    private void updateRenderCenters(TFMagicMapData mapData) {
        if (this.mc.thePlayer != null) {
            this.displayCenterX = this.mc.thePlayer.posX;
            this.displayCenterZ = this.mc.thePlayer.posZ;
        } else {
            this.displayCenterX = mapData.xCenter;
            this.displayCenterZ = mapData.zCenter;
        }

        this.mapCenterX = mapData.xCenter;
        this.mapCenterZ = mapData.zCenter;

        float mapScale = this.mapSize / 128.0F;
        double blocksPerPixel = 1 << mapData.scale;
        float pixelOffsetX = (float) ((this.displayCenterX - this.mapCenterX) / blocksPerPixel);
        float pixelOffsetZ = (float) ((this.displayCenterZ - this.mapCenterZ) / blocksPerPixel);
        pixelOffsetX = Math.max(-63.5F, Math.min(63.5F, pixelOffsetX));
        pixelOffsetZ = Math.max(-63.5F, Math.min(63.5F, pixelOffsetZ));
        this.renderMapLeft = this.mapLeft - pixelOffsetX * mapScale;
        this.renderMapTop = this.mapTop - pixelOffsetZ * mapScale;
    }

}
