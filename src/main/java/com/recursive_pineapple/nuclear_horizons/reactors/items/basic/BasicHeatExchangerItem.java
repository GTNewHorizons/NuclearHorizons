package com.recursive_pineapple.nuclear_horizons.reactors.items.basic;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;
import com.recursive_pineapple.nuclear_horizons.reactors.components.InventoryDirection;
import com.recursive_pineapple.nuclear_horizons.reactors.components.adapters.HeatMoverAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces.IHeatMover;

public class BasicHeatExchangerItem extends ReactorItem implements IHeatMover {

    private final int maxHeatFromReactor;
    private final int maxHeatFromNeighbour;

    public BasicHeatExchangerItem(String name, String textureName, int maxHeatFromReactor, int maxHeatFromNeighbour,
        int maxHeat) {
        super(name, textureName, "heat", maxHeat);

        this.maxHeatFromReactor = maxHeatFromReactor;
        this.maxHeatFromNeighbour = maxHeatFromNeighbour;
    }

    @Override
    public int getTransferFromReactor(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor) {
        return getTransferFrom(itemStack, reactor.getHullHeat(), reactor.getMaxHullHeat(), maxHeatFromReactor, true);
    }

    @Override
    public int getTransferFromNeighbour(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor,
        @Nonnull IComponentAdapter neighbour) {
        return getTransferFrom(
            itemStack,
            neighbour.getStoredHeat(),
            neighbour.getMaxHeat(),
            maxHeatFromNeighbour,
            false);
    }

    private int getTransferFrom(ItemStack itemStack, int sourceHeat, int sourceCapacity, int limit,
        boolean roundAmount) {
        if (limit == 0 || sourceCapacity <= 0) {
            return 0;
        }

        double ownPercent = 100.0 * getStoredHeat(itemStack) / getMaxHeat(itemStack);
        double sourcePercent = 100.0 * sourceHeat / sourceCapacity;
        long ownRounded = Math.round(ownPercent * 10.0);
        long sourceRounded = Math.round(sourcePercent * 10.0);

        if (ownRounded == sourceRounded) {
            return 0;
        }

        // Low-temperature steps use the component limit even for hull exchange.
        double transferPercent = sourcePercent + ownPercent / 2.0;
        int amount;
        if (transferPercent < 0.25) {
            amount = 1;
        } else if (transferPercent < 0.5) {
            amount = maxHeatFromNeighbour / 8;
        } else if (transferPercent < 0.75) {
            amount = maxHeatFromNeighbour / 4;
        } else if (transferPercent < 1.0) {
            amount = maxHeatFromNeighbour / 2;
        } else {
            double scaledAmount = sourceCapacity * transferPercent / 100.0;
            amount = Math.min(limit, roundAmount ? (int) Math.round(scaledAmount) : (int) scaledAmount);
        }

        return sourceRounded > ownRounded ? amount : -amount;
    }

    @Override
    public int getTransferNeighbourToAir(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor,
        @Nonnull IComponentAdapter neighbour) {
        return 0;
    }

    @Override
    public int getTransferToAir(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor) {
        return 0;
    }

    @Override
    public @Nonnull IComponentAdapter getAdapter(@Nonnull ItemStack itemStack, @Nonnull IReactorGrid reactor, int x,
        int y) {
        return new HeatExchangerAdapter(reactor, x, y, itemStack, this);
    }

    private static class HeatExchangerAdapter extends HeatMoverAdapter {

        private static final InventoryDirection[] DIRECTIONS = { InventoryDirection.Left, InventoryDirection.Right,
            InventoryDirection.Up, InventoryDirection.Down };

        HeatExchangerAdapter(IReactorGrid reactor, int x, int y, ItemStack itemStack, IHeatMover heatMover) {
            super(reactor, x, y, itemStack, heatMover);
        }

        @Override
        public void onHeatTick() {
            // Keep our stored heat unchanged until every transfer has used the same input percentage.
            int heatChange = 0;
            for (InventoryDirection direction : DIRECTIONS) {
                int neighbourX = direction.offsetX(x);
                int neighbourY = direction.offsetY(y);
                if (neighbourX < 0 || neighbourY < 0
                    || neighbourX >= reactor.getWidth()
                    || neighbourY >= reactor.getHeight()) {
                    continue;
                }

                IComponentAdapter neighbour = reactor.getComponent(neighbourX, neighbourY);
                if (neighbour != null && neighbour.containsHeat()) {
                    int fromNeighbour = getTransferFromNeighbour(neighbour);
                    if (fromNeighbour != 0) {
                        int rejected = neighbour.addHeat(-fromNeighbour);
                        // A destroyed receiver dissipates the overflow plus the transferred heat, less one unit.
                        boolean destroyed = reactor.getItem(neighbourX, neighbourY) != neighbour.getItemStack();
                        heatChange += fromNeighbour + (destroyed ? 1 - rejected : rejected);
                    }
                }
            }

            int fromReactor = getTransferFromReactor();
            if (fromReactor != 0) {
                reactor.addHullHeat(-fromReactor);
                heatChange += fromReactor;
            }
            addHeat(heatChange);
        }
    }

    @Override
    public void addReactorItemInfo(ItemStack itemStack, EntityPlayer player, List<String> chunks) {
        chunks.add(I18n.format("nh_tooltip.prelude"));

        if (this.maxHeatFromReactor > 0) {
            chunks.add(I18n.format("nh_tooltip.mover.reactor_xfer", this.maxHeatFromReactor));
        }

        if (this.maxHeatFromNeighbour > 0) {
            chunks.add(I18n.format("nh_tooltip.exchanger.comp_xfer", this.maxHeatFromNeighbour));
        }
    }

    @Override
    protected String getDurabilityTooltip(ItemStack itemStack) {
        return I18n.format("nh_tooltip.stored_heat", itemStack.getItemDamage(), itemStack.getMaxDamage());
    }
}
