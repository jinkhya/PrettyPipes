package de.ellpeck.prettypipes.pipe.modules.insertion;

import de.ellpeck.prettypipes.Registry;
import de.ellpeck.prettypipes.items.IModule;
import de.ellpeck.prettypipes.items.ModuleItem;
import de.ellpeck.prettypipes.items.ModuleTier;
import de.ellpeck.prettypipes.misc.DirectionSelector;
import de.ellpeck.prettypipes.misc.ItemFilter;
import de.ellpeck.prettypipes.pipe.PipeBlockEntity;
import de.ellpeck.prettypipes.pipe.containers.AbstractPipeContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class FilterModuleItem extends ModuleItem {

    public final int filterSlots;
    private final boolean canPopulateFromInventories;

    public FilterModuleItem(String name, ModuleTier tier) {
        super(name);
        this.filterSlots = tier.forTier(5, 9, 18);
        this.canPopulateFromInventories = tier.forTier(false, false, true);
    }

    @Override
    public boolean canAcceptItem(ItemStack module, PipeBlockEntity tile, ItemStack stack, Direction direction, IItemHandler destination) {
        return !this.getDirectionSelector(module, tile).has(direction) || this.getItemFilter(module, tile).isAllowed(stack);
    }

    @Override
    public boolean isCompatible(ItemStack module, PipeBlockEntity tile, IModule other) {
        return !(other instanceof FilterModuleItem);
    }

    @Override
    public boolean hasContainer(ItemStack module, PipeBlockEntity tile) {
        return true;
    }

    @Override
    public AbstractPipeContainer<?> getContainer(ItemStack module, PipeBlockEntity tile, int windowId, Inventory inv, Player player, int moduleIndex) {
        return new FilterModuleContainer(Registry.filterModuleContainer, windowId, player, tile.getBlockPos(), moduleIndex);
    }

    @Override
    public ItemFilter getItemFilter(ItemStack module, PipeBlockEntity tile) {
        var filter = new ItemFilter(this.filterSlots, module, tile);
        filter.canPopulateFromInventories = this.canPopulateFromInventories;
        return filter;
    }

    @Override
    public DirectionSelector getDirectionSelector(ItemStack module, PipeBlockEntity tile) {
        return new DirectionSelector(module, tile);
    }
}
