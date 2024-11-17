package com.direwolf20.buildinggadgets.common.registry.objects;

import com.direwolf20.buildinggadgets.common.items.Template;
import com.direwolf20.buildinggadgets.common.items.gadgets.*;
import com.direwolf20.buildinggadgets.common.items.pastes.*;
import com.direwolf20.buildinggadgets.common.registry.RegistryContainer;
import com.direwolf20.buildinggadgets.common.registry.RegistryObjectBuilder;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.direwolf20.buildinggadgets.common.util.ref.Reference.ItemReference;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ObjectHolder;

import java.util.function.Function;

@ObjectHolder(Reference.MODID)
@EventBusSubscriber(modid = Reference.MODID, bus = Bus.MOD)
public final class BGItems {

    private BGItems() {
    }

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
    public static ConstructionPasteContainer constructionPasteContainerT1;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_T2)
    public static ConstructionPasteContainer constructionPasteContainerT2;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_T3)
    public static ConstructionPasteContainer constructionPasteContainerT3;
    @ObjectHolder(ItemReference.PASTE_CONTAINER_CREATIVE)
    public static ConstructionPasteContainerCreative creativeConstructionPasteContainer;

    static void init() {
        addItemBuilder(ItemReference.GADGET_EXCHANGING_RL, unstackableItemProperties(), GadgetExchanger::new);
        addItemBuilder(ItemReference.GADGET_BUILDING_RL, unstackableItemProperties(), GadgetBuilding::new);
        addItemBuilder(ItemReference.GADGET_DESTRUCTION_RL, unstackableItemProperties(), GadgetDestruction::new);
        addItemBuilder(ItemReference.GADGET_COPY_PASTE_RL, unstackableItemProperties(), GadgetCopyPaste::new);
        addItemBuilder(ItemReference.PASTE_CONTAINER_T1_RL, unstackableItemProperties(), RegularPasteContainerTypes.T1::create);
        addItemBuilder(ItemReference.PASTE_CONTAINER_T2_RL, unstackableItemProperties(), RegularPasteContainerTypes.T2::create);
        addItemBuilder(ItemReference.PASTE_CONTAINER_T3_RL, unstackableItemProperties(), RegularPasteContainerTypes.T3::create);
        addItemBuilder(ItemReference.PASTE_CONTAINER_CREATIVE_RL, unstackableItemProperties(), ConstructionPasteContainerCreative::new);
        addItemBuilder(ItemReference.CONSTRUCTION_PASTE_RL, itemPropertiesWithGroup(), ConstructionPaste::new);
        addItemBuilder(ItemReference.CONSTRUCTION_CHUNK_DENSE_RL, itemPropertiesWithGroup(), Item::new);
        addItemBuilder(ItemReference.TEMPLATE_RL, itemPropertiesWithGroup(), Template::new);
    }

    /**
     * DRY way to create and add {@code RegistryObjectBuilder<Item, Item.Properties>}.
     */
    private static void addItemBuilder(ResourceLocation registryName, Item.Properties properties, Function<Item.Properties, Item> factory) {
        container.add(new RegistryObjectBuilder<Item, Item.Properties>(registryName)
                .builder(properties)
                .factory(factory));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        container.register(event);
    }

    static Item.Properties itemProperties() {
        return new Item.Properties();
    }

    static Item.Properties itemPropertiesWithGroup() {
        return itemProperties().group(BuildingObjects.creativeTab);
    }

    static Item.Properties unstackableItemProperties() {
        return itemPropertiesWithGroup().maxStackSize(1);
    }

    static void cleanup() {
        container.clear();
    }

}
