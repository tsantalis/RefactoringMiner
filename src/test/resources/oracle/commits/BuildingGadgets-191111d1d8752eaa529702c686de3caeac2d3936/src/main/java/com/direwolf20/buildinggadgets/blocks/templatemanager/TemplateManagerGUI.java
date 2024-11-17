/**
 * Parts of this class were adapted from code written by TTerrag for the Chisel mod: https://github.com/Chisel-Team/Chisel
 * Chisel is Open Source and distributed under GNU GPL v2
 */

package com.direwolf20.buildinggadgets.blocks.templatemanager;

import com.direwolf20.buildinggadgets.BuildingGadgets;
import com.direwolf20.buildinggadgets.items.CopyPasteTool;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import com.direwolf20.buildinggadgets.network.PacketTemplateManagerLoad;
import com.direwolf20.buildinggadgets.network.PacketTemplateManagerPaste;
import com.direwolf20.buildinggadgets.network.PacketTemplateManagerSave;
import com.direwolf20.buildinggadgets.tools.PasteToolBufferBuilder;
import com.direwolf20.buildinggadgets.tools.ToolDireBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;
import org.lwjgl.util.glu.Project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class TemplateManagerGUI extends GuiContainer {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 256;

    private boolean panelClicked;
    private int clickButton;
    private long lastDragTime;
    private int clickX, clickY;
    private float initRotX, initRotY, initZoom, initPanX, initPanY;
    private float prevRotX, prevRotY, prevPanX, prevPanY;
    private float momentumX, momentumY;
    private float momentumDampening = 0.98f;
    private float rotX = 0, rotY = 0, zoom = 1;
    private float panX = 0, panY = 0;
    Rectangle panel = new Rectangle(10, 18, 60, 60);

    private int scrollAcc;

    private GuiTextField nameField;

    private TemplateManagerTileEntity te;
    private TemplateManagerContainer container;

    private static final ResourceLocation background = new ResourceLocation(BuildingGadgets.MODID, "textures/gui/testcontainer.png");

    public TemplateManagerGUI(TemplateManagerTileEntity tileEntity, TemplateManagerContainer container) {
        super(container);
        this.te = tileEntity;
        this.container = container;
        //xSize = WIDTH;
        //ySize = HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    public void initGui() {
        super.initGui();
        //The parameters of GuiButton are(id, x, y, width, height, text);
        this.buttonList.add(new GuiButton(1, this.guiLeft + 87, this.guiTop + 11, 30, 20, "Save"));
        this.buttonList.add(new GuiButton(2, this.guiLeft + 136, this.guiTop + 11, 30, 20, "Load"));
        this.buttonList.add(new GuiButton(3, this.guiLeft + 87, this.guiTop + 55, 30, 20, "Copy"));
        this.buttonList.add(new GuiButton(4, this.guiLeft + 134, this.guiTop + 55, 35, 20, "Paste"));
        this.nameField = new GuiTextField(0, this.fontRenderer, this.guiLeft + 5, this.guiTop + 6, 80, this.fontRenderer.FONT_HEIGHT);
        this.nameField.setMaxStringLength(50);
        this.nameField.setVisible(true);
        //NOTE: the id always has to be different or else it might get called twice or never!
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        this.nameField.drawTextBox();
        drawStructure();
    }

    private void drawStructure() {
        int scale = new ScaledResolution(mc).getScaleFactor();
        drawRect(guiLeft + panel.getX() - 1, guiTop + panel.getY() - 1, guiLeft + panel.getX() + panel.getWidth() + 1, guiTop + panel.getY() + panel.getHeight() + 1, 0xFF8A8A8A);
        ItemStack itemstack = this.container.getSlot(0).getStack();
        BlockRendererDispatcher dispatcher = this.mc.getBlockRendererDispatcher();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        //float rotX = 165, rotY = 0, zoom = 1;
        if (!itemstack.isEmpty()) {
            String UUID = CopyPasteTool.getUUID(itemstack);
            ToolDireBuffer bufferBuilder = PasteToolBufferBuilder.getBufferFromMap(UUID);
            if (bufferBuilder != null) {
                BlockPos startPos = CopyPasteTool.getStartPos(itemstack);
                BlockPos endPos = CopyPasteTool.getEndPos(itemstack);
                double lengthX = Math.abs(startPos.getX() - endPos.getX());
                double lengthY = Math.abs(startPos.getY() - endPos.getY());
                double lengthZ = Math.abs(startPos.getZ() - endPos.getZ());

                final double maxW = 6 * 16;
                final double maxH = 11 * 16;

                double overW = Math.max(lengthX * 16 - maxW, lengthZ * 16 - maxW);
                double overH = lengthY * 16 - maxH;

                double sc = 1;
                double zoomScale = 1;

                if (overW > 0 && overW >= overH) {
                    sc = maxW / (overW + maxW);
                    zoomScale = overW / 40;
                } else if (overH > 0 && overH >= overW) {
                    sc = maxH / (overH + maxH);
                    zoomScale = overH / 40;
                }

                //System.out.println(distance);
                GlStateManager.pushMatrix();
                //GlStateManager.translate(panel.getX() + (panel.getWidth() / 2), panel.getY() + (panel.getHeight() / 2), 100);

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                GlStateManager.loadIdentity();
                //int scale = new ScaledResolution(mc).getScaleFactor();
                Project.gluPerspective(60, (float) panel.getWidth() / panel.getHeight(), 0.01F, 4000);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                //GlStateManager.translate(-panel.getX() - panel.getWidth() / 2, -panel.getY() - panel.getHeight() / 2, 0);
                GlStateManager.viewport((guiLeft + panel.getX()) * scale, mc.displayHeight - (guiTop + panel.getY() + panel.getHeight()) * scale, panel.getWidth() * scale, panel.getHeight() * scale);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);

                //double sc = 300 + 8 * 0.0125 * (Math.sqrt(zoom + 99) - 9);
                sc = (293 * sc) + zoom / zoomScale;
                GlStateManager.scale(sc, sc, sc);
                int moveX = startPos.getX() - endPos.getX();

                //GlStateManager.rotate(30, 0, 1, 0);
                if (startPos.getX() >= endPos.getX()) {
                    moveX--;
                    //GlStateManager.rotate(90, 0, -1, 0);
                }

                GlStateManager.translate((moveX) / 1.75, -Math.abs(startPos.getY() - endPos.getY()) / 1.75, 0);
                GlStateManager.translate(panX, panY, 0);
//System.out.println(((startPos.getX() - endPos.getX()) / 2) * -1 + ":" + ((startPos.getY() - endPos.getY()) / 2) * -1 + ":" + ((startPos.getZ() - endPos.getZ()) / 2) * -1);
                GlStateManager.translate(((startPos.getX() - endPos.getX()) / 2) * -1, ((startPos.getY() - endPos.getY()) / 2) * -1, ((startPos.getZ() - endPos.getZ()) / 2) * -1);
                GlStateManager.rotate(rotX, 1, 0, 0);
                GlStateManager.rotate(rotY, 0, 1, 0);
                GlStateManager.translate(((startPos.getX() - endPos.getX()) / 2), ((startPos.getY() - endPos.getY()) / 2), ((startPos.getZ() - endPos.getZ()) / 2));

                mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                if ((startPos.getX() - endPos.getX()) == 0) {
                    //GlStateManager.rotate(270, 0, 1, 0);
                }
                //Tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                //dispatcher.renderBlockBrightness(Blocks.GLASS.getDefaultState(), 1f);
                //Tessellator.getInstance().draw();

                if (bufferBuilder.getVertexCount() > 0) {
                    VertexFormat vertexformat = bufferBuilder.getVertexFormat();
                    int i = vertexformat.getNextOffset();
                    ByteBuffer bytebuffer = bufferBuilder.getByteBuffer();
                    List<VertexFormatElement> list = vertexformat.getElements();

                    for (int j = 0; j < list.size(); ++j) {
                        VertexFormatElement vertexformatelement = list.get(j);
                        VertexFormatElement.EnumUsage vertexformatelement$enumusage = vertexformatelement.getUsage();
                        int k = vertexformatelement.getType().getGlConstant();
                        int l = vertexformatelement.getIndex();
                        bytebuffer.position(vertexformat.getOffset(j));

                        // moved to VertexFormatElement.preDraw
                        vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
                    }

                    GlStateManager.glDrawArrays(bufferBuilder.getDrawMode(), 0, bufferBuilder.getVertexCount());
                    int i1 = 0;

                    for (int j1 = list.size(); i1 < j1; ++i1) {
                        VertexFormatElement vertexformatelement1 = list.get(i1);
                        VertexFormatElement.EnumUsage vertexformatelement$enumusage1 = vertexformatelement1.getUsage();
                        int k1 = vertexformatelement1.getIndex();

                        // moved to VertexFormatElement.postDraw
                        vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
                    }
                }

                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);

            }
        } else {
            rotX = 0;
            rotY = 0;
            zoom = 1;
            momentumX = 0;
            momentumY = 0;
            panX = 0;
            panY = 0;
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == 1) {
            PacketHandler.INSTANCE.sendToServer(new PacketTemplateManagerSave(te.getPos(), nameField.getText()));
        } else if (b.id == 2) {
            PacketHandler.INSTANCE.sendToServer(new PacketTemplateManagerLoad(te.getPos()));
        } else if (b.id == 3) {
            TemplateManagerCommands.CopyTemplate(container);
        } else if (b.id == 4) {
            String CBString = getClipboardString();
            //System.out.println("CBString Length: " + CBString.length());
            //System.out.println(CBString);
            try {
                NBTTagCompound tagCompound = JsonToNBT.getTagFromJson(CBString);
                //BlockMapIntState MapIntState = CopyPasteTool.getBlockMapIntState(tagCompound);
                int[] stateArray = tagCompound.getIntArray("stateIntArray");
                //int[] posArray = tagCompound.getIntArray("posIntArray");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed(tagCompound, baos);
                //ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                //NBTTagCompound newTag = CompressedStreamTools.readCompressed(bais);
                //System.out.println("BAOS Size: " + baos.size());

                //Anything larger than below is likely to overflow the max packet size, crashing your client.
                if (stateArray.length <= 12000 && baos.size() < 31000) {
                    PacketHandler.INSTANCE.sendToServer(new PacketTemplateManagerPaste(tagCompound, te.getPos(), nameField.getText()));
                } else {
                    Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.pastetoobig").getUnformattedComponentText()), false);
                }

            } catch (Throwable t) {
                System.out.println(t);
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + new TextComponentTranslation("message.gadget.pastefailed").getUnformattedComponentText()), false);
            }

        }
    }

    /*public static void sendSplitArrays(int[] stateArray, int[] posArray, Map<Short, IBlockState> stateMap) {

        System.out.println("PosArray Length: " + posArray.length);
    }*/

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.nameField.textboxKeyTyped(typedChar, keyCode)) {

        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.nameField.mouseClicked(mouseX, mouseY, mouseButton)) {
            nameField.setFocused(true);
        } else {
            nameField.setFocused(false);
            if (panel.contains(mouseX - guiLeft, mouseY - guiTop)) {
                clickButton = mouseButton;
                panelClicked = true;
                clickX = Mouse.getX();
                clickY = Mouse.getY();
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (panelClicked) {
            lastDragTime = System.currentTimeMillis();
        }
        panelClicked = false;
        initRotX = rotX;
        initRotY = rotY;
        initPanX = panX;
        initPanY = panY;
        initZoom = zoom;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void drawGuiContainerForegroundLayer(int j, int i) {
        boolean doMomentum = false;
        if (panelClicked) {
            if (clickButton == 0) {
                prevRotX = rotX;
                prevRotY = rotY;
                rotX = initRotX - (Mouse.getY() - clickY);
                rotY = initRotY + (Mouse.getX() - clickX);
                momentumX = rotX - prevRotX;
                momentumY = rotY - prevRotY;
                doMomentum = false;
            } else if (clickButton == 1) {
                prevPanX = panX;
                prevPanY = panY;
                panX = initPanX + (Mouse.getX() - clickX) / 8;
                panY = initPanY + (Mouse.getY() - clickY) / 8;
            }
        }

        if (doMomentum) {
            rotX += momentumX;
            rotY += momentumY;
            momentumX *= momentumDampening;
            momentumY *= momentumDampening;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        //System.out.println(Mouse.getEventDWheel());
        zoom = initZoom + Mouse.getEventDWheel() / 2;
        if (zoom < -200) zoom = -200;
        if (zoom > 1000) zoom = 1000;
        //System.out.println(zoom);

    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (!panelClicked) {
            initRotX = rotX;
            initRotY = rotY;
            initZoom = zoom;
            initPanX = panX;
            initPanY = panY;
        }

    }
}
