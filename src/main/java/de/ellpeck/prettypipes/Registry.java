package de.ellpeck.prettypipes;

import de.ellpeck.prettypipes.entities.PipeFrameEntity;
import de.ellpeck.prettypipes.entities.PipeFrameRenderer;
import de.ellpeck.prettypipes.items.*;
import de.ellpeck.prettypipes.pipe.modules.LowPriorityModuleItem;
import de.ellpeck.prettypipes.pipe.modules.SpeedModuleItem;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleGui;
import de.ellpeck.prettypipes.pipe.modules.extraction.ExtractionModuleItem;
import de.ellpeck.prettypipes.network.PipeNetwork;
import de.ellpeck.prettypipes.packets.PacketHandler;
import de.ellpeck.prettypipes.pipe.*;
import de.ellpeck.prettypipes.pipe.modules.containers.*;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleGui;
import de.ellpeck.prettypipes.pipe.modules.insertion.FilterModuleItem;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleGui;
import de.ellpeck.prettypipes.pipe.modules.retrieval.RetrievalModuleItem;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleContainer;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleGui;
import de.ellpeck.prettypipes.pipe.modules.stacksize.StackSizeModuleItem;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(bus = Bus.MOD)
public final class Registry {

    public static final ItemGroup GROUP = new ItemGroup(PrettyPipes.ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(wrenchItem);
        }
    };

    @CapabilityInject(PipeNetwork.class)
    public static Capability<PipeNetwork> pipeNetworkCapability;

    public static Item wrenchItem;

    public static Block pipeBlock;
    public static TileEntityType<PipeTileEntity> pipeTileEntity;

    public static EntityType<PipeFrameEntity> pipeFrameEntity;

    public static ContainerType<MainPipeContainer> pipeContainer;
    public static ContainerType<ExtractionModuleContainer> extractionModuleContainer;
    public static ContainerType<FilterModuleContainer> filterModuleContainer;
    public static ContainerType<RetrievalModuleContainer> retrievalModuleContainer;
    public static ContainerType<StackSizeModuleContainer> stackSizeModuleContainer;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                pipeBlock = new PipeBlock().setRegistryName("pipe")
        );
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.registerAll(
                wrenchItem = new WrenchItem().setRegistryName("wrench"),
                new Item(new Item.Properties().group(GROUP)).setRegistryName("blank_module"),
                new PipeFrameItem().setRegistryName("pipe_frame")
        );
        registry.registerAll(createTieredModule("extraction_module", ExtractionModuleItem::new));
        registry.registerAll(createTieredModule("filter_module", FilterModuleItem::new));
        registry.registerAll(createTieredModule("speed_module", SpeedModuleItem::new));
        registry.registerAll(createTieredModule("low_priority_module", LowPriorityModuleItem::new));
        registry.registerAll(createTieredModule("retrieval_module", RetrievalModuleItem::new));
        registry.register(new StackSizeModuleItem("stack_size_module"));

        ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b.getRegistryName().getNamespace().equals(PrettyPipes.ID))
                .forEach(b -> registry.register(new BlockItem(b, new Item.Properties().group(GROUP)).setRegistryName(b.getRegistryName())));
    }

    @SubscribeEvent
    public static void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().registerAll(
                pipeTileEntity = (TileEntityType<PipeTileEntity>) TileEntityType.Builder.create(PipeTileEntity::new, pipeBlock).build(null).setRegistryName("pipe")
        );
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        event.getRegistry().registerAll(
                pipeFrameEntity = (EntityType<PipeFrameEntity>) EntityType.Builder.<PipeFrameEntity>create(PipeFrameEntity::new, EntityClassification.MISC).build("pipe_frame").setRegistryName("pipe_frame")
        );
    }

    @SubscribeEvent
    public static void registerContainers(RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().registerAll(
                // this needs to be registered manually since it doesn't send the module slot
                pipeContainer = (ContainerType<MainPipeContainer>) IForgeContainerType.create((windowId, inv, data) -> new MainPipeContainer(pipeContainer, windowId, inv.player, data.readBlockPos())).setRegistryName("pipe"),
                extractionModuleContainer = createPipeContainer("extraction_module"),
                filterModuleContainer = createPipeContainer("filter_module"),
                retrievalModuleContainer = createPipeContainer("retrieval_module"),
                stackSizeModuleContainer = createPipeContainer("stack_size_module")
        );
    }

    private static <T extends AbstractPipeContainer<?>> ContainerType<T> createPipeContainer(String name) {
        return (ContainerType<T>) IForgeContainerType.create((windowId, inv, data) -> {
            PipeTileEntity tile = Utility.getTileEntity(PipeTileEntity.class, inv.player.world, data.readBlockPos());
            int moduleIndex = data.readInt();
            ItemStack moduleStack = tile.modules.getStackInSlot(moduleIndex);
            return ((IModule) moduleStack.getItem()).getContainer(moduleStack, tile, windowId, inv, inv.player, moduleIndex);
        }).setRegistryName(name);
    }

    private static Item[] createTieredModule(String name, BiFunction<String, ModuleTier, ModuleItem> item) {
        List<Item> items = new ArrayList<>();
        for (ModuleTier tier : ModuleTier.values())
            items.add(item.apply(name, tier).setRegistryName(tier.name().toLowerCase(Locale.ROOT) + "_" + name));
        return items.toArray(new Item[0]);
    }

    public static void setup(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(PipeNetwork.class, new Capability.IStorage<PipeNetwork>() {
            @Nullable
            @Override
            public INBT writeNBT(Capability<PipeNetwork> capability, PipeNetwork instance, Direction side) {
                return null;
            }

            @Override
            public void readNBT(Capability<PipeNetwork> capability, PipeNetwork instance, Direction side, INBT nbt) {

            }
        }, () -> null);
        PacketHandler.setup();
    }

    public static final class Client {
        public static void setup(FMLClientSetupEvent event) {
            RenderTypeLookup.setRenderLayer(pipeBlock, RenderType.cutout());
            ClientRegistry.bindTileEntityRenderer(pipeTileEntity, PipeRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(pipeFrameEntity, PipeFrameRenderer::new);

            ScreenManager.registerFactory(pipeContainer, MainPipeGui::new);
            ScreenManager.registerFactory(extractionModuleContainer, ExtractionModuleGui::new);
            ScreenManager.registerFactory(filterModuleContainer, FilterModuleGui::new);
            ScreenManager.registerFactory(retrievalModuleContainer, RetrievalModuleGui::new);
            ScreenManager.registerFactory(stackSizeModuleContainer, StackSizeModuleGui::new);
        }
    }
}
