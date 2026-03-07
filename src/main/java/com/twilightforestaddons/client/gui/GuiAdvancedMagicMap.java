package com.twilightforestaddons.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.twilightforestaddons.Config;
import com.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer;
import com.twilightforestaddons.client.renderer.AdvancedMagicMapItemRenderer.HoveredFeature;
import com.twilightforestaddons.item.ModItems;
import com.twilightforestaddons.map.BossFeatureRegistry;
import com.twilightforestaddons.network.ModNetwork;
import com.twilightforestaddons.network.packet.PacketRequestAdvancedMapRefresh;
import com.twilightforestaddons.network.packet.PacketTeleportToBossFeature;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import twilightforest.TFMagicMapData;

@SideOnly(Side.CLIENT)
public class GuiAdvancedMagicMap extends GuiScreen {

    private static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final int MAP_FRAME_PADDING = 7;
    private static final int INFO_PANEL_PADDING = 6;
    private static final int INFO_PANEL_GAP = 12;
    private static final int INFO_LINE_HEIGHT = 12;
    private static final int INFO_PANEL_BUTTON_GAP = 8;
    private static final int PANEL_BACKGROUND = 0x66000000;
    private static final int PANEL_BORDER = 0x8A2F2A24;
    private static final int BUTTON_BACKGROUND = 0x7A000000;
    private static final int BUTTON_BACKGROUND_HOVER = 0x90000000;
    private static final int BUTTON_BORDER_HOVER = 0xB67A6A57;

    private final int mapId;

    private AdvancedMagicMapItemRenderer renderer;
    private GuiButton refreshButton;
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
        this.buttonList.clear();
        String refreshLabel = StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.refresh");
        this.refreshButton = new PanelButton(
            100,
            0,
            0,
            Math.max(76, this.fontRendererObj.getStringWidth(refreshLabel) + 16),
            20,
            refreshLabel);
        this.buttonList.add(this.refreshButton);
        this.updateMapLayout();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!this.ensurePlayerStillHoldingMap()) {
            return;
        }

        this.drawDefaultBackground();
        this.updateMapLayout();

        TFMagicMapData mapData = this.renderer.getClientMapData(this.mapId, this.mc.theWorld);
        if (mapData == null) {
            drawCenteredString(
                this.fontRendererObj,
                StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.loading"),
                this.width / 2,
                this.height / 2,
                0xFFFFFF);
            return;
        }

        this.drawMapBackground(
            this.mapLeft - MAP_FRAME_PADDING,
            this.mapTop - MAP_FRAME_PADDING,
            this.mapSize + MAP_FRAME_PADDING * 2);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        this.renderer.renderGuiMap(
            this.mc.thePlayer,
            this.mc.getTextureManager(),
            mapData,
            this.mapLeft,
            this.mapTop,
            this.mapSize);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        HoveredFeature hovered = this.renderer
            .findHoveredFeature(mapData, mouseX, mouseY, this.mapLeft, this.mapTop, this.mapSize);

        List<InfoLine> infoLines = new ArrayList<InfoLine>();
        infoLines.add(
            new InfoLine(
                StatCollector.translateToLocalFormatted(
                    "gui.twilightforestaddons.advanced_map.center",
                    mapData.xCenter,
                    mapData.zCenter),
                0xEAEAEA));
        infoLines.add(
            new InfoLine(
                StatCollector
                    .translateToLocalFormatted("gui.twilightforestaddons.advanced_map.facing", this.getFacingLabel()),
                0xEAEAEA));

        if (this.isInsideMap(mouseX, mouseY)) {
            int hoverWorldX = this.renderer.mapPixelToWorldX(mapData, (mouseX - this.mapLeft) * 128.0F / this.mapSize);
            int hoverWorldZ = this.renderer.mapPixelToWorldZ(mapData, (mouseY - this.mapTop) * 128.0F / this.mapSize);
            infoLines.add(
                new InfoLine(
                    StatCollector.translateToLocalFormatted(
                        "gui.twilightforestaddons.advanced_map.cursor",
                        hoverWorldX,
                        hoverWorldZ),
                    0x8FE0FF));
        }

        if (hovered != null) {
            infoLines.add(
                new InfoLine(
                    StatCollector.translateToLocalFormatted(
                        "gui.twilightforestaddons.advanced_map.target",
                        BossFeatureRegistry.getFeatureName(hovered.featureId)),
                    0xFFD35A));

            if (Config.advancedMapEnforceProgression
                && !BossFeatureRegistry.isFeatureUnlocked(hovered.featureId, this.mc.thePlayer)) {
                infoLines.add(
                    new InfoLine(
                        StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.locked"),
                        0xFF6666));
            }
        }

        this.updateRefreshButtonLayout(infoLines);
        this.drawTitlePanel();
        this.drawHintPanel();
        this.drawInfoPanel(infoLines);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 1) {
            this.mc.displayGuiScreen(null);
            return;
        }

        if (mouseButton == 0) {
            TFMagicMapData mapData = this.renderer.getClientMapData(this.mapId, this.mc.theWorld);
            HoveredFeature hovered = this.renderer
                .findHoveredFeature(mapData, mouseX, mouseY, this.mapLeft, this.mapTop, this.mapSize);
            if (hovered != null) {
                ModNetwork.CHANNEL.sendToServer(
                    new PacketTeleportToBossFeature(
                        this.mapId,
                        hovered.featureId,
                        hovered.mapCoordX,
                        hovered.mapCoordZ));
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.refreshButton) {
            this.requestRefresh();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_R) {
            this.requestRefresh();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void updateMapLayout() {
        int suggestedSize = Math.min(this.width - 120, this.height - 90);
        this.mapSize = Math.max(260, Math.min(430, suggestedSize));
        this.mapLeft = (this.width - this.mapSize) / 2;
        this.mapTop = Math.max(50, (this.height - this.mapSize) / 2 - 18);

        int maxBottom = this.height - 70;
        if (this.mapTop + this.mapSize > maxBottom) {
            this.mapTop = Math.max(50, maxBottom - this.mapSize);
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

    private boolean isInsideMap(int mouseX, int mouseY) {
        return mouseX >= this.mapLeft && mouseX <= this.mapLeft + this.mapSize
            && mouseY >= this.mapTop
            && mouseY <= this.mapTop + this.mapSize;
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

    private void drawTitlePanel() {
        String title = StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.title");
        int panelWidth = Math.max(120, this.fontRendererObj.getStringWidth(title) + 16);
        int panelLeft = this.width / 2 - panelWidth / 2;
        int panelTop = this.mapTop - 28;
        this.drawPanel(panelLeft, panelTop, panelWidth, 16);
        this.fontRendererObj.drawStringWithShadow(
            title,
            this.width / 2 - this.fontRendererObj.getStringWidth(title) / 2,
            panelTop + 4,
            0xFFFFFF);
    }

    private void drawHintPanel() {
        String hint = StatCollector.translateToLocal("gui.twilightforestaddons.advanced_map.hint");
        int panelWidth = this.fontRendererObj.getStringWidth(hint) + 18;
        int panelLeft = this.width / 2 - panelWidth / 2;
        int panelTop = this.mapTop + this.mapSize + 10;
        this.drawPanel(panelLeft, panelTop, panelWidth, 16);
        this.fontRendererObj.drawStringWithShadow(
            hint,
            this.width / 2 - this.fontRendererObj.getStringWidth(hint) / 2,
            panelTop + 4,
            0xD8D8D8);
    }

    private void drawInfoPanel(List<InfoLine> infoLines) {
        InfoPanelLayout layout = this.computeInfoPanelLayout(infoLines);
        if (layout == null) {
            return;
        }

        this.drawPanel(layout.left, layout.top, layout.width, layout.height);

        int textX = layout.left + INFO_PANEL_PADDING;
        int textY = layout.top + INFO_PANEL_PADDING;
        for (InfoLine line : infoLines) {
            this.fontRendererObj.drawStringWithShadow(line.text, textX, textY, line.color);
            textY += INFO_LINE_HEIGHT;
        }
    }

    private void drawPanel(int left, int top, int width, int height) {
        drawRect(left, top, left + width, top + height, PANEL_BACKGROUND);
        drawRect(left, top, left + width, top + 1, PANEL_BORDER);
        drawRect(left, top + height - 1, left + width, top + height, PANEL_BORDER);
        drawRect(left, top, left + 1, top + height, PANEL_BORDER);
        drawRect(left + width - 1, top, left + width, top + height, PANEL_BORDER);
    }

    private void updateRefreshButtonLayout(List<InfoLine> infoLines) {
        if (this.refreshButton == null) {
            return;
        }

        InfoPanelLayout layout = this.computeInfoPanelLayout(infoLines);
        if (layout != null) {
            this.refreshButton.width = Math.max(76, layout.width);
            this.refreshButton.xPosition = layout.left;
            this.refreshButton.yPosition = layout.top + layout.height + INFO_PANEL_BUTTON_GAP;
            return;
        }

        this.refreshButton.width = Math
            .max(76, this.fontRendererObj.getStringWidth(this.refreshButton.displayString) + 16);
        this.refreshButton.xPosition = this.mapLeft + this.mapSize - this.refreshButton.width;
        this.refreshButton.yPosition = this.mapTop - 30;
    }

    private InfoPanelLayout computeInfoPanelLayout(List<InfoLine> infoLines) {
        if (infoLines == null || infoLines.isEmpty()) {
            return null;
        }

        int maxWidth = 0;
        for (InfoLine line : infoLines) {
            maxWidth = Math.max(maxWidth, this.fontRendererObj.getStringWidth(line.text));
        }

        int panelWidth = maxWidth + INFO_PANEL_PADDING * 2;
        int panelHeight = infoLines.size() * INFO_LINE_HEIGHT + INFO_PANEL_PADDING * 2;
        int panelLeft = this.mapLeft - panelWidth - INFO_PANEL_GAP;
        if (panelLeft < 8) {
            int rightLeft = this.mapLeft + this.mapSize + INFO_PANEL_GAP;
            panelLeft = rightLeft + panelWidth <= this.width - 8 ? rightLeft : this.mapLeft + 10;
        }

        int panelTop = this.mapTop + 10;
        return new InfoPanelLayout(panelLeft, panelTop, panelWidth, panelHeight);
    }

    private void requestRefresh() {
        ModNetwork.CHANNEL.sendToServer(new PacketRequestAdvancedMapRefresh(this.mapId));
    }

    private static final class InfoLine {

        private final String text;
        private final int color;

        private InfoLine(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    private static final class InfoPanelLayout {

        private final int left;
        private final int top;
        private final int width;
        private final int height;

        private InfoPanelLayout(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    private static final class PanelButton extends GuiButton {

        private PanelButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }

            this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;

            int background = this.field_146123_n ? BUTTON_BACKGROUND_HOVER : BUTTON_BACKGROUND;
            int border = this.field_146123_n ? BUTTON_BORDER_HOVER : PANEL_BORDER;
            int textColor = !this.enabled ? 0x888888 : this.field_146123_n ? 0xFFFFFF : 0xE6E6E6;

            drawRect(
                this.xPosition,
                this.yPosition,
                this.xPosition + this.width,
                this.yPosition + this.height,
                background);
            drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + 1, border);
            drawRect(
                this.xPosition,
                this.yPosition + this.height - 1,
                this.xPosition + this.width,
                this.yPosition + this.height,
                border);
            drawRect(this.xPosition, this.yPosition, this.xPosition + 1, this.yPosition + this.height, border);
            drawRect(
                this.xPosition + this.width - 1,
                this.yPosition,
                this.xPosition + this.width,
                this.yPosition + this.height,
                border);

            this.mouseDragged(mc, mouseX, mouseY);
            this.drawCenteredString(
                mc.fontRenderer,
                this.displayString,
                this.xPosition + this.width / 2,
                this.yPosition + (this.height - 8) / 2,
                textColor);
        }
    }
}
