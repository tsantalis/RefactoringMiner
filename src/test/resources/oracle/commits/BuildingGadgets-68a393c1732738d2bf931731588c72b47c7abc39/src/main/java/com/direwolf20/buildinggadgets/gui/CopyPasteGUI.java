/**
 * Parts of this class were adapted from code written by TTerrag for the Chisel mod: https://github.com/Chisel-Team/Chisel
 * Chisel is Open Source and distributed under GNU GPL v2
 */

package com.direwolf20.buildinggadgets.gui;

import com.direwolf20.buildinggadgets.BuildingGadgets;
import com.direwolf20.buildinggadgets.Config;
import com.direwolf20.buildinggadgets.ModItems;
import com.direwolf20.buildinggadgets.network.PacketCopyCoords;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;

public class CopyPasteGUI extends GuiScreen {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 256;

    private GuiTextField startX;
    private GuiTextField startY;
    private GuiTextField startZ;
    private GuiTextField endX;
    private GuiTextField endY;
    private GuiTextField endZ;

    private boolean absoluteCoords = Config.absoluteCoordDefault;

    int guiLeft = 15;
    int guiTop = 15;

    ItemStack copyPasteTool;
    BlockPos startPos;
    BlockPos endPos;

    private static final ResourceLocation background = new ResourceLocation(BuildingGadgets.MODID, "textures/gui/testcontainer.png");

    public CopyPasteGUI(ItemStack tool) {
        super();
        this.copyPasteTool = tool;
    }

    /*@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }*/

    @Override
    public void initGui() {
        super.initGui();
        startPos = ModItems.copyPasteTool.getStartPos(copyPasteTool);
        endPos = ModItems.copyPasteTool.getEndPos(copyPasteTool);
        if (startPos == null) startPos = new BlockPos(0, 0, 0);
        if (endPos == null) endPos = new BlockPos(0, 0, 0);

        startX = new GuiTextField(0, this.fontRenderer, this.guiLeft + 65, this.guiTop + 15, 40, this.fontRenderer.FONT_HEIGHT);
        startX.setMaxStringLength(50);
        startX.setVisible(true);

        startY = new GuiTextField(1, this.fontRenderer, this.guiLeft + 165, this.guiTop + 15, 40, this.fontRenderer.FONT_HEIGHT);
        startY.setMaxStringLength(50);
        startY.setVisible(true);

        startZ = new GuiTextField(2, this.fontRenderer, this.guiLeft + 265, this.guiTop + 15, 40, this.fontRenderer.FONT_HEIGHT);
        startZ.setMaxStringLength(50);
        startZ.setVisible(true);


        endX = new GuiTextField(3, this.fontRenderer, this.guiLeft + 65, this.guiTop + 35, 40, this.fontRenderer.FONT_HEIGHT);
        endX.setMaxStringLength(50);
        endX.setVisible(true);

        endY = new GuiTextField(4, this.fontRenderer, this.guiLeft + 165, this.guiTop + 35, 40, this.fontRenderer.FONT_HEIGHT);
        endY.setMaxStringLength(50);
        endY.setVisible(true);

        endZ = new GuiTextField(5, this.fontRenderer, this.guiLeft + 265, this.guiTop + 35, 40, this.fontRenderer.FONT_HEIGHT);
        endZ.setMaxStringLength(50);
        endZ.setVisible(true);

        updateTextFields();
        //NOTE: the id always has to be different or else it might get called twice or never!
        this.buttonList.add(new GuiButton(1, this.guiLeft + 45, this.guiTop + 60, 40, 20, "Ok"));
        this.buttonList.add(new GuiButton(2, this.guiLeft + 145, this.guiTop + 60, 40, 20, "Cancel"));
        this.buttonList.add(new GuiButton(3, this.guiLeft + 245, this.guiTop + 60, 40, 20, "Clear"));
        this.buttonList.add(new GuiButton(4, this.guiLeft + 325, this.guiTop + 60, 80, 20, "CoordsMode"));
        this.buttonList.add(new DireButton(5, this.guiLeft + 50, this.guiTop + 14, 10, 10, "-"));
        this.buttonList.add(new DireButton(6, this.guiLeft + 110, this.guiTop + 14, 10, 10, "+"));
        this.buttonList.add(new DireButton(7, this.guiLeft + 150, this.guiTop + 14, 10, 10, "-"));
        this.buttonList.add(new DireButton(8, this.guiLeft + 210, this.guiTop + 14, 10, 10, "+"));
        this.buttonList.add(new DireButton(9, this.guiLeft + 250, this.guiTop + 14, 10, 10, "-"));
        this.buttonList.add(new DireButton(10, this.guiLeft + 310, this.guiTop + 14, 10, 10, "+"));
        this.buttonList.add(new DireButton(11, this.guiLeft + 50, this.guiTop + 34, 10, 10, "-"));
        this.buttonList.add(new DireButton(12, this.guiLeft + 110, this.guiTop + 34, 10, 10, "+"));
        this.buttonList.add(new DireButton(13, this.guiLeft + 150, this.guiTop + 34, 10, 10, "-"));
        this.buttonList.add(new DireButton(14, this.guiLeft + 210, this.guiTop + 34, 10, 10, "+"));
        this.buttonList.add(new DireButton(15, this.guiLeft + 250, this.guiTop + 34, 10, 10, "-"));
        this.buttonList.add(new DireButton(16, this.guiLeft + 310, this.guiTop + 34, 10, 10, "+"));
    }

    public void fieldChange(GuiTextField textField, int amount) {
        nullCheckTextBoxes();
        if (GuiScreen.isShiftKeyDown()) amount = amount * 10;
        try {
            int i = Integer.valueOf(textField.getText());
            i = i + amount;
            textField.setText(String.valueOf(i));
        } catch (Throwable t) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mc.getTextureManager().bindTexture(background);
        //drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        this.startX.drawTextBox();
        this.startY.drawTextBox();
        this.startZ.drawTextBox();
        this.endX.drawTextBox();
        this.endY.drawTextBox();
        this.endZ.drawTextBox();
        fontRenderer.drawStringWithShadow("Start X", this.guiLeft, this.guiTop + 15, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("Y", this.guiLeft + 131, this.guiTop + 15, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("Z", this.guiLeft + 231, this.guiTop + 15, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("End X", this.guiLeft + 8, this.guiTop + 35, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("Y", this.guiLeft + 131, this.guiTop + 35, 0xFFFFFF);
        fontRenderer.drawStringWithShadow("Z", this.guiLeft + 231, this.guiTop + 35, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void nullCheckTextBoxes() {
        if (absoluteCoords) {
            if (startX.getText() == "") {
                startX.setText(String.valueOf(startPos.getX()));
            }
            if (startY.getText() == "") {
                startY.setText(String.valueOf(startPos.getY()));
            }
            if (startZ.getText() == "") {
                startZ.setText(String.valueOf(startPos.getZ()));
            }
            if (endX.getText() == "") {
                endX.setText(String.valueOf(endPos.getX()));
            }
            if (endY.getText() == "") {
                endY.setText(String.valueOf(endPos.getY()));
            }
            if (endZ.getText() == "") {
                endZ.setText(String.valueOf(endPos.getZ()));
            }
        } else {
            if (startX.getText() == "") {
                startX.setText("0");
            }
            if (startY.getText() == "") {
                startY.setText("0");
            }
            if (startZ.getText() == "") {
                startZ.setText("0");
            }
            if (endX.getText() == "") {
                endX.setText("0");
            }
            if (endY.getText() == "") {
                endY.setText("0");
            }
            if (endZ.getText() == "") {
                endZ.setText("0");
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 1) {
            nullCheckTextBoxes();
            try {
                if (absoluteCoords) {
                    startPos = new BlockPos(Integer.parseInt(startX.getText()), Integer.parseInt(startY.getText()), Integer.parseInt(startZ.getText()));
                    endPos = new BlockPos(Integer.parseInt(endX.getText()), Integer.parseInt(endY.getText()), Integer.parseInt(endZ.getText()));
                } else {
                    startPos = new BlockPos(startPos.getX() + Integer.parseInt(startX.getText()), startPos.getY() + Integer.parseInt(startY.getText()), startPos.getZ() + Integer.parseInt(startZ.getText()));
                    endPos = new BlockPos(startPos.getX() + Integer.parseInt(endX.getText()), startPos.getY() + Integer.parseInt(endY.getText()), startPos.getZ() + Integer.parseInt(endZ.getText()));
                }
                PacketHandler.INSTANCE.sendToServer(new PacketCopyCoords(startPos, endPos));
            } catch (Throwable t) {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.copyguierror").getUnformattedComponentText()), true);
            }
            this.mc.displayGuiScreen(null);
        } else if (b.id == 2) {
            this.mc.displayGuiScreen(null);
        } else if (b.id == 3) {
            PacketHandler.INSTANCE.sendToServer(new PacketCopyCoords(BlockPos.ORIGIN, BlockPos.ORIGIN));
            this.mc.displayGuiScreen(null);
        } else if (b.id == 4) {
            coordsModeSwitch();
            updateTextFields();
        } else if (b.id == 5) {
            fieldChange(startX, -1);
        } else if (b.id == 6) {
            fieldChange(startX, 1);
        } else if (b.id == 7) {
            fieldChange(startY, -1);
        } else if (b.id == 8) {
            fieldChange(startY, 1);
        } else if (b.id == 9) {
            fieldChange(startZ, -1);
        } else if (b.id == 10) {
            fieldChange(startZ, 1);
        } else if (b.id == 11) {
            fieldChange(endX, -1);
        } else if (b.id == 12) {
            fieldChange(endX, 1);
        } else if (b.id == 13) {
            fieldChange(endY, -1);
        } else if (b.id == 14) {
            fieldChange(endY, 1);
        } else if (b.id == 15) {
            fieldChange(endZ, -1);
        } else if (b.id == 16) {
            fieldChange(endZ, 1);
        }

    }

    protected void coordsModeSwitch() {
        absoluteCoords = !absoluteCoords;
    }

    protected void updateTextFields() {
        String x, y, z;
        if (absoluteCoords) {
            BlockPos start = startX.getText() != "" ? new BlockPos(startPos.getX() + Integer.parseInt(startX.getText()), startPos.getY() + Integer.parseInt(startY.getText()), startPos.getZ() + Integer.parseInt(startZ.getText())) : startPos;
            BlockPos end = endX.getText() != "" ? new BlockPos(startPos.getX() + Integer.parseInt(endX.getText()), startPos.getY() + Integer.parseInt(endY.getText()), startPos.getZ() + Integer.parseInt(endZ.getText())) : endPos;
            startX.setText(String.valueOf(start.getX()));
            startY.setText(String.valueOf(start.getY()));
            startZ.setText(String.valueOf(start.getZ()));
            endX.setText(String.valueOf(end.getX()));
            endY.setText(String.valueOf(end.getY()));
            endZ.setText(String.valueOf(end.getZ()));
        } else {
            x = startX.getText() != "" ? String.valueOf(Integer.parseInt(startX.getText()) - startPos.getX()) : "0";
            startX.setText(x);
            y = startY.getText() != "" ? String.valueOf(Integer.parseInt(startY.getText()) - startPos.getY()) : "0";
            startY.setText(y);
            z = startZ.getText() != "" ? String.valueOf(Integer.parseInt(startZ.getText()) - startPos.getZ()) : "0";
            startZ.setText(z);
            x = endX.getText() != "" ? String.valueOf(Integer.parseInt(endX.getText()) - startPos.getX()) : String.valueOf(endPos.getX() - startPos.getX());
            endX.setText(x);
            y = endY.getText() != "" ? String.valueOf(Integer.parseInt(endY.getText()) - startPos.getY()) : String.valueOf(endPos.getY() - startPos.getY());
            endY.setText(y);
            z = endZ.getText() != "" ? String.valueOf(Integer.parseInt(endZ.getText()) - startPos.getZ()) : String.valueOf(endPos.getZ() - startPos.getZ());
            endZ.setText(z);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.startX.textboxKeyTyped(typedChar, keyCode) || this.startY.textboxKeyTyped(typedChar, keyCode) || this.startZ.textboxKeyTyped(typedChar, keyCode) || this.endX.textboxKeyTyped(typedChar, keyCode) || this.endY.textboxKeyTyped(typedChar, keyCode) || this.endZ.textboxKeyTyped(typedChar, keyCode)) {

        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) {
            if (this.startX.mouseClicked(mouseX, mouseY, 0)) {
                startX.setText("");
            } else if (this.startY.mouseClicked(mouseX, mouseY, 0)) {
                startY.setText("");
            } else if (this.startZ.mouseClicked(mouseX, mouseY, 0)) {
                startZ.setText("");
            } else if (this.endX.mouseClicked(mouseX, mouseY, 0)) {
                endX.setText("");
            } else if (this.endY.mouseClicked(mouseX, mouseY, 0)) {
                endY.setText("");
            } else if (this.endZ.mouseClicked(mouseX, mouseY, 0)) {
                endZ.setText("");
            } else {
                //startX.setFocused(false);
                super.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } else {
            if (this.startX.mouseClicked(mouseX, mouseY, mouseButton)) {
                startX.setFocused(true);
            } else if (this.startY.mouseClicked(mouseX, mouseY, mouseButton)) {
                startY.setFocused(true);
            } else if (this.startZ.mouseClicked(mouseX, mouseY, mouseButton)) {
                startZ.setFocused(true);
            } else if (this.endX.mouseClicked(mouseX, mouseY, mouseButton)) {
                endX.setFocused(true);
            } else if (this.endY.mouseClicked(mouseX, mouseY, mouseButton)) {
                endY.setFocused(true);
            } else if (this.endZ.mouseClicked(mouseX, mouseY, mouseButton)) {
                endZ.setFocused(true);
            } else {
                //startX.setFocused(false);
                super.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }


    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        //System.out.println(Mouse.getEventDWheel());
        //System.out.println(zoom);

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
