package com.zzhalex233.twilightforestaddons.client.gui;

import com.zzhalex233.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer;
import com.zzhalex233.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer.HoveredFeature;
import com.zzhalex233.twilightforestaddons.item.ModItems;
import com.zzhalex233.twilightforestaddons.map.BossFeatureRegistry;
import com.zzhalex233.twilightforestaddons.network.ModNetwork;
import com.zzhalex233.twilightforestaddons.network.packet.PacketTeleportToBossFeature;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import twilightforest.TFMagicMapData;

public class GuiAdvancedMagicMap extends GuiScreen {
    private static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final int MAP_FRAME_PADDING = 7;
    private static final int PANEL_BACKGROUND = 0x66000000;
    private static final int PANEL_BORDER = 0x8A2F2A24;

    private final int mapId;
    private AdvancedMagicMapItemRenderer renderer;
    private int mapSize;
    private int mapLeft;
    private int mapTop;

    public GuiAdvancedMagicMap(int mapId) {
        this.mapId = mapId;
    }

    @Override
    public void initGui() {
        if (this.renderer == null) {
            this.renderer = new AdvancedMagicMapItemRenderer();
        }
        this.allowUserInput = true;
        updateMapLayout();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!ensurePlayerStillHoldingMap()) {
            return;
        }

        drawDefaultBackground();
        updateMapLayout();

        TFMagicMapData mapData = this.renderer.getClientMapData(this.mapId, this.mc.world);
        if (mapData == null) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.twilightforestaddons.advanced_map.loading"), this.width / 2, this.height / 2, 0xFFFFFF);
            return;
        }

        drawMapBackground(this.mapLeft - MAP_FRAME_PADDING, this.mapTop - MAP_FRAME_PADDING, this.mapSize + MAP_FRAME_PADDING * 2);

        GlStateManager.disableDepth();
        this.renderer.renderGuiMap(this.mc.player, this.mc.getTextureManager(), mapData, this.mapLeft, this.mapTop, this.mapSize);
        GlStateManager.enableDepth();

        HoveredFeature hovered = this.renderer.findHoveredFeature(this.mc.player, mapData, mouseX, mouseY, this.mapLeft, this.mapTop, this.mapSize);
        drawTitle();
        drawHint();
        drawInfoPanel(mapData, hovered, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (mouseButton == 1) {
            this.mc.displayGuiScreen(null);
            return;
        }

        if (mouseButton == 0) {
            TFMagicMapData mapData = this.renderer.getClientMapData(this.mapId, this.mc.world);
            HoveredFeature hovered = mapData == null ? null : this.renderer.findHoveredFeature(this.mc.player, mapData, mouseX, mouseY, this.mapLeft, this.mapTop, this.mapSize);
            if (hovered != null) {
                int approxWorldX = mapData.xCenter + Math.round(hovered.mapCoordX * (1 << mapData.scale) / 2.0F);
                int approxWorldZ = mapData.zCenter + Math.round(hovered.mapCoordZ * (1 << mapData.scale) / 2.0F);
                ModNetwork.CHANNEL.sendToServer(new PacketTeleportToBossFeature(this.mapId, hovered.featureId, hovered.mapCoordX, hovered.mapCoordZ, approxWorldX, approxWorldZ));
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateMapLayout() {
        int suggestedSize = Math.min(this.width - 180, this.height - 130);
        this.mapSize = Math.max(220, Math.min(360, suggestedSize));
        this.mapLeft = (this.width - this.mapSize) / 2;
        this.mapTop = Math.max(38, (this.height - this.mapSize) / 2 - 30);

        int maxBottom = this.height - 78;
        if (this.mapTop + this.mapSize > maxBottom) {
            this.mapTop = Math.max(38, maxBottom - this.mapSize);
        }
    }

    private boolean ensurePlayerStillHoldingMap() {
        if (this.mc.player == null) {
            this.mc.displayGuiScreen(null);
            return false;
        }

        ItemStack mainHand = this.mc.player.getHeldItemMainhand();
        ItemStack offHand = this.mc.player.getHeldItemOffhand();
        if (isTrackedMap(mainHand) || isTrackedMap(offHand)) {
            return true;
        }

        this.mc.displayGuiScreen(null);
        return false;
    }

    private boolean isTrackedMap(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.ADVANCED_MAGIC_MAP && stack.getMetadata() == this.mapId;
    }

    private void drawMapBackground(int left, int top, int size) {
        this.mc.getTextureManager().bindTexture(MAP_BACKGROUND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawModalRectWithCustomSizedTexture(left, top, 0.0F, 0.0F, size, size, size, size);
    }

    private void drawTitle() {
        String title = I18n.format("gui.twilightforestaddons.advanced_map.title");
        int panelWidth = Math.max(120, this.fontRenderer.getStringWidth(title) + 16);
        int panelLeft = this.width / 2 - panelWidth / 2;
        int panelTop = this.mapTop - 32;
        drawPanel(panelLeft, panelTop, panelWidth, 16);
        this.fontRenderer.drawStringWithShadow(title, this.width / 2.0F - this.fontRenderer.getStringWidth(title) / 2.0F, panelTop + 4, 0xFFFFFF);
    }

    private void drawHint() {
        String hint = I18n.format("gui.twilightforestaddons.advanced_map.hint");
        int panelWidth = this.fontRenderer.getStringWidth(hint) + 18;
        int panelLeft = this.width / 2 - panelWidth / 2;
        int panelTop = this.mapTop + this.mapSize + 10;
        drawPanel(panelLeft, panelTop, panelWidth, 16);
        this.fontRenderer.drawStringWithShadow(hint, this.width / 2.0F - this.fontRenderer.getStringWidth(hint) / 2.0F, panelTop + 4, 0xD8D8D8);
    }

    private void drawInfoPanel(TFMagicMapData mapData, HoveredFeature hovered, int mouseX, int mouseY) {
        int hoverWorldX = 0;
        int hoverWorldZ = 0;
        boolean showCursor = isInsideMap(mouseX, mouseY);
        if (showCursor) {
            hoverWorldX = this.renderer.mapPixelToWorldX(this.mc.player, mapData, (mouseX - this.mapLeft) * 128.0F / this.mapSize);
            hoverWorldZ = this.renderer.mapPixelToWorldZ(this.mc.player, mapData, (mouseY - this.mapTop) * 128.0F / this.mapSize);
        }

        int contentWidth = this.fontRenderer.getStringWidth(I18n.format("gui.twilightforestaddons.advanced_map.center", mapData.xCenter, mapData.zCenter));
        contentWidth = Math.max(contentWidth, this.fontRenderer.getStringWidth(I18n.format("gui.twilightforestaddons.advanced_map.facing", getFacingLabel())));
        if (showCursor) {
            contentWidth = Math.max(contentWidth, this.fontRenderer.getStringWidth(I18n.format("gui.twilightforestaddons.advanced_map.cursor", hoverWorldX, hoverWorldZ)));
        }
        if (hovered != null) {
            contentWidth = Math.max(contentWidth, this.fontRenderer.getStringWidth(I18n.format("gui.twilightforestaddons.advanced_map.target", BossFeatureRegistry.getFeatureName(hovered.featureId))));
        }

        int lineCount = 2 + (showCursor ? 1 : 0) + (hovered != null ? 1 : 0);
        int panelWidth = contentWidth + 12;
        int panelLeft = Math.max(12, this.mapLeft - (panelWidth + 12));
        int panelTop = this.mapTop;
        int panelHeight = 12 + lineCount * 12;
        if (panelLeft + panelWidth > this.width - 12) {
            panelLeft = this.mapLeft + this.mapSize + 12;
        }
        drawPanel(panelLeft, panelTop, panelWidth, panelHeight);

        int textX = panelLeft + 6;
        int textY = panelTop + 6;
        this.fontRenderer.drawStringWithShadow(I18n.format("gui.twilightforestaddons.advanced_map.center", mapData.xCenter, mapData.zCenter), textX, textY, 0xEAEAEA);
        textY += 12;
        this.fontRenderer.drawStringWithShadow(I18n.format("gui.twilightforestaddons.advanced_map.facing", getFacingLabel()), textX, textY, 0xEAEAEA);
        textY += 12;

        if (showCursor) {
            this.fontRenderer.drawStringWithShadow(I18n.format("gui.twilightforestaddons.advanced_map.cursor", hoverWorldX, hoverWorldZ), textX, textY, 0x8FE0FF);
            textY += 12;
        }

        if (hovered != null) {
            this.fontRenderer.drawStringWithShadow(I18n.format("gui.twilightforestaddons.advanced_map.target", BossFeatureRegistry.getFeatureName(hovered.featureId)), textX, textY, 0xFFD35A);
        }
    }

    private boolean isInsideMap(int mouseX, int mouseY) {
        return mouseX >= this.mapLeft && mouseX <= this.mapLeft + this.mapSize && mouseY >= this.mapTop && mouseY <= this.mapTop + this.mapSize;
    }

    private String getFacingLabel() {
        if (this.mc.player == null) {
            return "";
        }

        int dir = MathHelper.floor(this.mc.player.rotationYaw * 4.0D / 360.0D + 0.5D) & 3;
        switch (dir) {
            case 0:
                return I18n.format("gui.twilightforestaddons.direction.south");
            case 1:
                return I18n.format("gui.twilightforestaddons.direction.west");
            case 2:
                return I18n.format("gui.twilightforestaddons.direction.north");
            default:
                return I18n.format("gui.twilightforestaddons.direction.east");
        }
    }

    private void drawPanel(int left, int top, int width, int height) {
        drawRect(left, top, left + width, top + height, PANEL_BACKGROUND);
        drawHorizontalLine(left, left + width - 1, top, PANEL_BORDER);
        drawHorizontalLine(left, left + width - 1, top + height - 1, PANEL_BORDER);
        drawVerticalLine(left, top, top + height - 1, PANEL_BORDER);
        drawVerticalLine(left + width - 1, top, top + height - 1, PANEL_BORDER);
    }
}