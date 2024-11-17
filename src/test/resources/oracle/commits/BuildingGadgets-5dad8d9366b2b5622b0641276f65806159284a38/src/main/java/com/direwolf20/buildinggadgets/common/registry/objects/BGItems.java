package com.direwolf20.buildinggadgets.common.registry.objects;

import com.direwolf20.buildinggadgets.common.items.Template;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.items.gadgets.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.items.pastes.ConstructionPaste;
import com.direwolf20.buildinggadgets.common.items.pastes.ConstructionPasteContainer;
import com.direwolf20.buildinggadgets.common.items.pastes.ConstructionPasteContainerCreative;
import com.direwolf20.buildinggadgets.common.items.pastes.RegularPasteContainerTypes;
import com.direwolf20.buildinggadgets.common.registry.RegistryContainer;
import com.direwolf20.buildinggadgets.common.registry.RegistryObjectBuilder;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.ItemReference;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(Reference.MODID)
@EventBusSubscriber(modid = Reference.MODID, bus = Bus.MOD)
public final class BGItems {
    private BGItems() {}

    private static final RegistryContainer<Item, RegistryObjectBuilder<Item, Item.Properties>> container = new RegistryContainer<>();
    // Gadgets
    @ObjectHolder(ItemReference.GADGET_BUILDING)
    public static GadgetBuilding gadgetBuilding;
    @ObjectHolder(ItemReference.GADGET_COPY_PASTE)
    public static GadgetCopyPaste gadgetCopyPaste;
    @ObjectHolder(ItemReference.GADGET_EXCHANGING)
    public static GadgetExchanger gadgetExchanger;
    @ObjectHolder(ItemReference.GADGET_DESTRUCTION)
    public static GadgetDestruction gadgetDestruction;

    // Building Items
    @ObjectHolder(ItemReference.CONSTRUCTION_PASTE)
    public static ConstructionPaste constructionPaste;
    @ObjectHolder(ItemReference.CONSTRUCTION_CHUNK_DENSE)
    public static Item constructionChunkDense;
    @ObjectHolder(ItemReference.TEMPLATE)
    public static Template template;

    // Construction Paste Containers
    @ObjectHolder(ItemReference.PASTE_CONTAINER_T1)
    public static ConstructionPasteContainer ConstructionPasteContainer;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_T2)
    public static ConstructionPasteContainer ConstructionPasteContainer2;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_T3)
    public static ConstructionPasteContainer ConstructionPasteContainer3;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_CREATIVE)
    public static ConstructionPasteContainerCreative CreativeConstructionPasteContainer;

    static void init() {
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.GADGET_EXCHANGING_RL).builder(itemProperties()).factory(GadgetExchanger::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.GADGET_BUILDING_RL).builder(itemProperties()).factory(GadgetBuilding::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.GADGET_DESTRUCTION_RL).builder(itemProperties()).factory(GadgetDestruction::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.GADGET_COPY_PASTE_RL).builder(itemProperties()).factory(GadgetCopyPaste::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.PASTE_CONTAINER_T1_RL).builder(itemProperties()).factory(RegularPasteContainerTypes.T1::create));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.PASTE_CONTAINER_T2_RL).builder(itemProperties()).factory(RegularPasteContainerTypes.T2::create));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.PASTE_CONTAINER_T3_RL).builder(itemProperties()).factory(RegularPasteContainerTypes.T3::create));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.PASTE_CONTAINER_CREATIVE_RL).builder(itemProperties()).factory(ConstructionPasteContainerCreative::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.CONSTRUCTION_PASTE_RL).builder(itemProperties()).factory(ConstructionPaste::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.CONSTRUCTION_CHUNK_DENSE_RL).builder(itemProperties()).factory(Item::new));
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(ItemReference.TEMPLATE_RL).builder(itemProperties()).factory(Template::new));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        container.register(event);
    }

    static Item.Properties itemProperties() {
        return new Item.Properties().group(BuildingObjects.creativeTab);
    }

    static Item.Properties itemPropertiesWithoutGroup() {
        return new Item.Properties();
    }

    static void cleanup() {
        container.clear();
    }
}
