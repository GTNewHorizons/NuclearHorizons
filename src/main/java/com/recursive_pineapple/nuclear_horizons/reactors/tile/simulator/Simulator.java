package com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.reactors.components.ComponentRegistry;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

/**
 * Owns one background simulation. Call start, cancel, pollFinished and close from the client thread.
 */
public class Simulator implements AutoCloseable {

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private Future<SimulationResult> simulation;

    public boolean isRunning() {
        return simulation != null && !simulation.isDone();
    }

    public void start(SimulationConfig config) {
        cancel();
        SimulationConfig snapshot = new SimulationConfig();
        snapshot.put(config);
        simulation = threadPool.submit(() -> runSimulation(snapshot));
    }

    public void cancel() {
        if (simulation != null) {
            simulation.cancel(true);
            simulation = null;
        }
    }

    @Override
    public void close() {
        cancel();
        threadPool.shutdownNow();
    }

    private SimulationResult runSimulation(SimulationConfig config) {
        SimulatedReactor reactor = new SimulatedReactor();
        // Components mutate their working stacks. Keep the original input for the result.
        reactor.config = new SimulationConfig();
        reactor.config.put(config);
        reactor.run();
        reactor.result.config = config;
        return reactor.result;
    }

    @Nullable
    public SimulationResult pollFinished() throws InterruptedException, ExecutionException {
        if (simulation == null || !simulation.isDone()) {
            return null;
        }

        Future<SimulationResult> finished = simulation;
        simulation = null;
        try {
            return finished.get();
        } catch (CancellationException e) {
            return null;
        }
    }

    private static class SimulatedReactor implements IReactorGrid {

        private static final int COL_COUNT = TileReactorCore.COL_COUNT;
        private static final int ROW_COUNT = TileReactorCore.ROW_COUNT;

        public ItemStack[] contents = new ItemStack[COL_COUNT * ROW_COUNT];
        public IComponentAdapter[] components = new IComponentAdapter[COL_COUNT * ROW_COUNT];

        public int reactorTickCounter = 0, storedHeat, addedHU, pulseCounter;
        public boolean pulseActive, suspended;
        public double addedEU;
        public Integer nextUnpause;

        public SimulationConfig config;

        public SimulationResult result = new SimulationResult();
        public SimulationComponentResult[] componentResults = new SimulationComponentResult[COL_COUNT * ROW_COUNT];
        public SimulationComponentResult currentComponent;

        @Override
        public int getWidth() {
            return COL_COUNT;
        }

        @Override
        public int getHeight() {
            return ROW_COUNT;
        }

        @Override
        public @Nullable IComponentAdapter getComponent(int x, int y) {
            if (x < 0 || x >= COL_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for x: %d, must conform to x >= 0, x < %d", x, COL_COUNT));
            }

            if (y < 0 || y >= ROW_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for y: %d, must conform to y >= 0, y < %d", y, ROW_COUNT));
            }

            int index = y * COL_COUNT + x;

            var adapter = this.components[index];
            if (adapter != null) {
                return adapter;
            }

            var item = this.contents[index];
            if (item != null) {
                adapter = ComponentRegistry.getAdapter(item, this, x, y);
                this.components[index] = adapter;
            }

            return adapter;
        }

        @Override
        public @Nullable ItemStack getItem(int x, int y) {
            if (x < 0 || x >= COL_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for x: %d, must conform to x >= 0, x < %d", x, COL_COUNT));
            }

            if (y < 0 || y >= ROW_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for y: %d, must conform to y >= 0, y < %d", y, ROW_COUNT));
            }

            int index = y * COL_COUNT + x;

            return this.contents[index];
        }

        @Override
        public void setItem(int x, int y, @Nullable ItemStack item) {
            if (x < 0 || x >= COL_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for x: %d, must conform to x >= 0, x < %d", x, COL_COUNT));
            }

            if (y < 0 || y >= ROW_COUNT) {
                throw new IllegalArgumentException(
                    String.format("Illegal value for y: %d, must conform to y >= 0, y < %d", y, ROW_COUNT));
            }

            int index = y * COL_COUNT + x;

            this.contents[index] = item;
            this.components[index] = item != null && ComponentRegistry.isReactorItem(item)
                ? ComponentRegistry.getAdapter(item, this, x, y)
                : null;
        }

        @Override
        public boolean isActive() {
            if (suspended) {
                return false;
            }

            if (nextUnpause != null && reactorTickCounter < nextUnpause) {
                return false;
            }

            if (config.pulsed) {
                return pulseActive;
            }

            return true;
        }

        @Override
        public int getHullHeat() {
            return storedHeat;
        }

        @Override
        public int getMaxHullHeat() {
            int maxHeat = 5000;

            for (int row = 0; row < ROW_COUNT; row++) {
                for (int col = 0; col < COL_COUNT; col++) {
                    var component = getComponent(col, row);
                    if (component != null) {
                        maxHeat += component.getReactorMaxHeatIncrease();
                    }
                }
            }

            return maxHeat;
        }

        @Override
        public void setHullHeat(int newHeat) {
            this.storedHeat = newHeat;
        }

        @Override
        public void addHullHeat(int delta) {
            this.storedHeat += delta;

            if (delta > 0) {
                currentComponent.totalHullHeating += delta;
            } else {
                currentComponent.totalHullCooling += -delta;
            }
        }

        @Override
        public int addAirHeat(int delta) {
            result.totalHU += delta;
            addedHU += delta;
            currentComponent.totalAirHeating += delta;

            return 0;
        }

        @Override
        public void addEU(double eu) {
            result.totalEU += eu;
            currentComponent.totalEUOutput += eu;
            addedEU += eu;
        }

        @Override
        public boolean isFluid() {
            return config.fluid;
        }

        public void run() {
            this.reactorTickCounter = 0;

            Arrays.fill(components, null);
            Arrays.fill(contents, null);

            for (int i = 0; i < components.length; i++) {
                contents[i] = config.components[i] == null ? null : config.components[i].stack;
                componentResults[i] = new SimulationComponentResult();

                var comp = getComponent(i % COL_COUNT, i / COL_COUNT);
                if (comp != null) {
                    comp.addHeat(config.components[i].initialHeat);
                }
            }

            result.componentResults = componentResults;

            this.pulseActive = true;
            this.storedHeat = config.initialHeat;

            while (reactorTickCounter < config.maxSimulationTicks) {
                if (Thread.currentThread()
                    .isInterrupted()) {
                    throw new CancellationException();
                }

                if (reactorTickCounter % 2 == 0) {
                    this.addedEU = 0;
                } else {
                    this.addedHU = 0;
                }

                for (int row = 0; row < ROW_COUNT; row++) {
                    for (int col = 0; col < COL_COUNT; col++) {
                        int index = row * COL_COUNT + col;

                        var component = getComponent(col, row);

                        if (component != null) {
                            component.setSimulationComponent(result, index);
                            currentComponent = componentResults[index];

                            if (reactorTickCounter % 2 == 0) {
                                component.onEnergyTick();
                            } else {
                                component.onHeatTick();
                            }

                            var heat = component.getStoredHeat();

                            currentComponent.totalTempSecs += heat;

                            if (heat < currentComponent.minTemp) {
                                currentComponent.minTemp = heat;
                            }

                            if (heat > currentComponent.maxTemp) {
                                currentComponent.maxTemp = heat;
                            }

                            var compConfig = config.components[index];

                            if (compConfig.hasAutomation
                                && component.getStoredHeat() > compConfig.replacementThreshold) {
                                components[index] = null;
                                contents[index] = null;
                            }

                            // may be set to null by the component itself, or to e.g. a depleted fuel rod
                            if (contents[index] == null
                                || !ItemStack.areItemStacksEqual(contents[index], compConfig.stack)) {
                                currentComponent.replaceCount++;
                                contents[index] = compConfig.stack;
                            }
                        }
                    }
                }

                if (reactorTickCounter % 2 == 0) {
                    int eupt = (int) (this.addedEU / 20);

                    if (eupt < result.minEUpT) {
                        result.minEUpT = eupt;
                    }

                    if (eupt > result.maxEUpT) {
                        result.maxEUpT = eupt;
                    }
                } else {
                    int hups = this.addedHU;

                    if (hups < result.minHUpS) {
                        result.minHUpS = hups;
                    }

                    if (hups > result.maxHUpS) {
                        result.maxHUpS = hups;
                    }

                    if (storedHeat < result.minTemp) {
                        result.minTemp = storedHeat;
                    }

                    if (storedHeat > result.maxTemp) {
                        result.maxTemp = storedHeat;
                    }

                    result.totalTempSecs += storedHeat;

                    double ratio = storedHeat / (double) getMaxHullHeat();

                    if (result.timeToEvaporate != null && result.timeToNormal == null && ratio < 0.5) {
                        result.timeToNormal = reactorTickCounter;
                    }

                    if (result.timeToBurn == null && ratio >= 0.4) {
                        result.timeToBurn = reactorTickCounter;
                    }

                    if (result.timeToEvaporate == null && ratio >= 0.5) {
                        result.timeToEvaporate = reactorTickCounter;
                    }

                    if (result.timeToHurt == null && ratio >= 0.7) {
                        result.timeToHurt = reactorTickCounter;
                    }

                    if (result.timeToLava == null && ratio >= 0.85) {
                        result.timeToLava = reactorTickCounter;
                    }

                    if (result.timeToExplode == null && ratio >= 1) {
                        result.timeToExplode = reactorTickCounter;
                        break;
                    }
                }

                if (isActive()) {
                    result.activeTime++;
                } else {
                    result.pausedTime++;
                }

                if (config.pulsed) {
                    pulseCounter++;

                    if (pulseActive) {
                        if (pulseCounter >= config.onPulse) {
                            pulseActive = false;
                            pulseCounter -= config.onPulse;
                        }
                    } else {
                        if (pulseCounter >= config.offPulse) {
                            pulseActive = true;
                            pulseCounter -= config.offPulse;
                        }
                    }
                }

                result.simTime++;

                reactorTickCounter++;
            }

            result.end = System.nanoTime();
        }
    }
}
