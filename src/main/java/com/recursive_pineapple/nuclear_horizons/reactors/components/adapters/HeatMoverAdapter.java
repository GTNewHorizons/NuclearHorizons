package com.recursive_pineapple.nuclear_horizons.reactors.components.adapters;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;
import com.recursive_pineapple.nuclear_horizons.reactors.components.InventoryDirection;
import com.recursive_pineapple.nuclear_horizons.reactors.items.interfaces.IHeatMover;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.SimulationResult;

public class HeatMoverAdapter implements IComponentAdapter {

    protected final IReactorGrid reactor;
    protected final int x, y;
    protected final ItemStack itemStack;
    protected final IHeatMover heatMover;

    public HeatMoverAdapter(IReactorGrid reactor, int x, int y, ItemStack itemStack, IHeatMover heatMover) {
        this.reactor = reactor;
        this.x = x;
        this.y = y;
        this.itemStack = itemStack;
        this.heatMover = heatMover;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean containsHeat() {
        return this.heatMover.getMaxHeat(itemStack) > 0;
    }

    @Override
    public int getStoredHeat() {
        return this.heatMover.getStoredHeat(itemStack);
    }

    @Override
    public int getMaxHeat() {
        return this.heatMover.getMaxHeat(itemStack);
    }

    @Override
    public int addHeat(int delta) {
        int rejected = this.heatMover.addHeat(itemStack, delta);

        if (rejected > 0 && this.heatMover.getRemainingHealth(itemStack) <= 0) {
            this.reactor.setItem(this.x, this.y, null);
        }

        return rejected;
    }

    @Override
    public void onSimulationStarted(SimulationResult result, int componentIndex) {
        int ventableNeighbours = 0;
        for (var direction : InventoryDirection.values()) {
            int neighbourX = direction.offsetX(x);
            int neighbourY = direction.offsetY(y);
            if (neighbourX < 0 || neighbourY < 0
                || neighbourX >= reactor.getWidth()
                || neighbourY >= reactor.getHeight()) {
                continue;
            }

            var neighbour = reactor.getComponent(neighbourX, neighbourY);
            if (neighbour != null && neighbour.canVentHeat()) {
                ventableNeighbours++;
            }
        }

        result.componentResults[componentIndex].maxAirHeatingCapacity = heatMover
            .getMaxAirCooling(itemStack, reactor, ventableNeighbours);
    }

    protected int getTransferFromReactor() {
        return this.heatMover.getTransferFromReactor(itemStack, reactor);
    }

    protected int getTransferToAir() {
        return this.heatMover.getTransferToAir(itemStack, reactor);
    }

    protected int getTransferNeighbourToAir(IComponentAdapter neighbour) {
        return this.heatMover.getTransferNeighbourToAir(itemStack, reactor, neighbour);
    }

    protected int getTransferFromNeighbour(IComponentAdapter neighbour) {
        return this.heatMover.getTransferFromNeighbour(itemStack, reactor, neighbour);
    }

    @Override
    public void onHeatTick() {
        int fromReactor = getTransferFromReactor();

        if (fromReactor != 0) {
            int rejected = this.addHeat(fromReactor);
            this.reactor.addHullHeat(-fromReactor + rejected);

            if (this.reactor.getItem(x, y) != itemStack) {
                return;
            }
        }

        int toAir = getTransferToAir();

        if (toAir != 0) {
            this.addHeat(-toAir);
            int rejected = this.reactor.addAirHeat(toAir);

            if (rejected != 0) {
                this.addHeat(rejected);
            }
        }

        for (var dir : InventoryDirection.values()) {
            int x2 = dir.offsetX(x);
            int y2 = dir.offsetY(y);

            if (x2 < 0 || y2 < 0 || x2 >= reactor.getWidth() || y2 >= reactor.getHeight()) {
                continue;
            }

            var neighbour = reactor.getComponent(x2, y2);

            if (neighbour != null && neighbour.containsHeat()) {
                int fromNeighbourToAir = getTransferNeighbourToAir(neighbour);

                if (fromNeighbourToAir != 0) {
                    neighbour.addHeat(-fromNeighbourToAir);
                    int rejected = this.reactor.addAirHeat(fromNeighbourToAir);

                    if (rejected != 0) {
                        neighbour.addHeat(rejected);
                    }
                }

                int fromNeighbour = getTransferFromNeighbour(neighbour);

                if (fromNeighbour > 0) {
                    int rejected = this.addHeat(fromNeighbour);
                    neighbour.addHeat(-fromNeighbour + rejected);
                } else if (fromNeighbour < 0) {
                    int rejected = neighbour.addHeat(-fromNeighbour);
                    this.addHeat(fromNeighbour + rejected);
                }
            }
        }
    }
}
