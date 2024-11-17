package com.direwolf20.buildinggadgets.eventhandlers;

import com.direwolf20.buildinggadgets.items.CopyPasteTool;
import com.direwolf20.buildinggadgets.items.Template;
import com.direwolf20.buildinggadgets.network.PacketHandler;
import com.direwolf20.buildinggadgets.network.PacketRequestBlockMap;
import com.direwolf20.buildinggadgets.network.PacketRequestTemplateBlockMap;
import com.direwolf20.buildinggadgets.tools.PasteToolBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientTickEvent {

    private static int counter = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        counter++;
        if (counter > 600) {
            counter = 0;
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) return;

            for (int i = 0; i < 36; ++i) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (stack.getItem() instanceof CopyPasteTool) {
                    String UUID = CopyPasteTool.getUUID(stack);
                    if (UUID != null) {
                        if (PasteToolBufferBuilder.isUpdateNeeded(UUID, stack)) {
                            //System.out.println("BlockMap Update Needed for UUID: " + UUID + " in slot " + i);
                            PacketHandler.INSTANCE.sendToServer(new PacketRequestBlockMap(CopyPasteTool.getUUID(stack)));
                        }
                    }
                } else if (stack.getItem() instanceof Template) {
                    String UUID = Template.getUUID(stack);
                    if (UUID != null) {
                        if (PasteToolBufferBuilder.isUpdateNeeded(UUID, stack)) {
                            //System.out.println("BlockMap Update Needed for UUID: " + UUID + " in slot " + i);
                            PacketHandler.INSTANCE.sendToServer(new PacketRequestTemplateBlockMap(Template.getUUID(stack)));
                        }
                    }
                }
            }
        }
    }
}
