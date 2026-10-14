package com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.recursive_pineapple.nuclear_horizons.Config;
import com.recursive_pineapple.nuclear_horizons.reactors.components.ComponentRegistry;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IComponentAdapter;
import com.recursive_pineapple.nuclear_horizons.reactors.components.IReactorGrid;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore;

/**
 * Owns one background simulation. Call start, cancel, pollFinished and close from the owning game thread.
 */
public class Simulator implements AutoCloseable {

    public static int maxComponentInitialHeat(ItemStack stack) {
        SimulatedReactor reactor = new SimulatedReactor();
        reactor.contents[0] = ItemStack.copyItemStack(stack);
        IComponentAdapter component = reactor.getComponent(0, 0);
        return component == null ? 0 : component.getMaxHeat();
    }

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
        reactor.result.config = config;
        reactor.run();
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

        public int reactorTickCounter = 0, storedHeat, addedHU;
        public boolean suspended;
        public int addedEU;
        public Integer nextUnpause;
        private double fractionalEU;
        private int heatTickHullCapacity = -1;

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
                if (config.offPulse <= 0) return true;
                if (config.onPulse <= 0) return false;
                long period = (long) config.onPulse + config.offPulse;
                return reactorTickCounter % period < config.onPulse;
            }

            return true;
        }

        @Override
        public int getHullHeat() {
            return storedHeat;
        }

        @Override
        public int getMaxHullHeat() {
            if (heatTickHullCapacity >= 0) {
                return heatTickHullCapacity;
            }

            int maxHeat = Config.REACTOR_BASE_HEAT;

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
                result.totalHullHeating += delta;
                currentComponent.totalHullHeating += delta;
            } else {
                result.totalHullCooling += -delta;
                currentComponent.totalHullCooling += -delta;
            }
        }

        @Override
        public int addAirHeat(int delta) {
            if (isFluid()) {
                delta *= Config.FLUID_NUKE_HU_MULTIPLIER;
            }
            result.totalHU += delta;
            addedHU += delta;
            currentComponent.totalAirHeating += delta;

            return 0;
        }

        @Override
        public void addEU(double eu) {
            if (!isFluid()) {
                double generated = eu * Config.REACTOR_EU_MULTIPLIER;

                double reactorOutput = generated + fractionalEU;
                int wholeReactorEU = (int) reactorOutput;
                fractionalEU = reactorOutput - wholeReactorEU;
                result.totalEU += wholeReactorEU;
                addedEU += wholeReactorEU;

                double componentOutput = generated + currentComponent.fractionalEUOutput;
                int wholeComponentEU = (int) componentOutput;
                currentComponent.fractionalEUOutput = componentOutput - wholeComponentEU;
                currentComponent.totalEUOutput += wholeComponentEU;
            }
        }

        @Override
        public boolean isFluid() {
            return config.fluid;
        }

        public void run() {
            this.reactorTickCounter = 0;
            this.fractionalEU = 0;

            Arrays.fill(components, null);
            Arrays.fill(contents, null);

            for (int i = 0; i < components.length; i++) {
                contents[i] = config.components[i] == null ? null : config.components[i].stack;
                componentResults[i] = new SimulationComponentResult();

                var comp = getComponent(i % COL_COUNT, i / COL_COUNT);
                if (comp != null) {
                    comp.setUsedHeatCapacity(config.components[i].initialHeat);
                }
            }

            result.componentResults = componentResults;

            this.storedHeat = config.initialHeat;
            result.minTemp = storedHeat;
            result.maxTemp = storedHeat;
            recordTemperatureThresholds(0, false);
            for (int i = 0; i < componentResults.length; i++) {
                var component = getComponent(i % COL_COUNT, i / COL_COUNT);
                int heat = component == null ? 0 : component.getStoredHeat();
                componentResults[i].minTemp = heat;
                componentResults[i].maxTemp = heat;
            }

            for (int i = 0; i < componentResults.length; i++) {
                var component = getComponent(i % COL_COUNT, i / COL_COUNT);
                if (component != null) {
                    component.onSimulationStarted(result, i);
                }
            }

            while (reactorTickCounter < config.maxSimulationTicks) {
                if (Thread.currentThread()
                    .isInterrupted()) {
                    throw new CancellationException();
                }

                this.addedEU = 0;
                this.addedHU = 0;
                if (config.pulsed) {
                    suspended = suspended ? storedHeat > config.resumeTemp : storedHeat >= config.suspendTemp;
                }
                boolean active = isActive();

                tickComponents(true);
                tickComponents(false);
                sampleComponents();

                reactorTickCounter++;
                result.simTime++;
                if (active) {
                    result.activeTime++;
                } else {
                    result.pausedTime++;
                }

                if (sampleReactor()) {
                    break;
                }
                replaceComponents();

            }

            if (result.simTime == 0) {
                result.minEUpT = 0;
                result.minHUpS = 0;
            }
            result.end = System.nanoTime();
        }

        private void tickComponents(boolean heatPhase) {
            if (heatPhase) {
                heatTickHullCapacity = Config.REACTOR_BASE_HEAT;
            }
            try {
                for (int row = 0; row < ROW_COUNT; row++) {
                    for (int col = 0; col < COL_COUNT; col++) {
                        var component = getComponent(col, row);
                        if (component == null) continue;
                        int index = row * COL_COUNT + col;
                        currentComponent = componentResults[index];
                        component.setSimulationComponent(result, index);
                        if (heatPhase) {
                            heatTickHullCapacity += component.getReactorMaxHeatIncrease();
                            component.onHeatTick();
                        } else {
                            component.onEnergyTick();
                        }
                    }
                }
            } finally {
                heatTickHullCapacity = -1;
            }
        }

        private void sampleComponents() {
            for (int index = 0; index < componentResults.length; index++) {
                var component = getComponent(index % COL_COUNT, index / COL_COUNT);
                var stats = componentResults[index];
                int heat = component == null ? 0 : component.getStoredHeat();
                stats.totalTempSecs += heat;
                stats.minTemp = Math.min(stats.minTemp, heat);
                stats.maxTemp = Math.max(stats.maxTemp, heat);

            }
        }

        private void replaceComponents() {
            if (!config.automated) return;
            for (int index = 0; index < components.length; index++) {
                var compConfig = config.components[index];
                if (compConfig == null) continue;
                var component = getComponent(index % COL_COUNT, index / COL_COUNT);
                boolean overThreshold = compConfig.hasAutomation && component != null
                    && component.getUsedHeatCapacity() > compConfig.replacementThreshold;
                if (!overThreshold && contents[index] == compConfig.stack) continue;

                componentResults[index].replaceCount++;
                compConfig.stack = ItemStack.copyItemStack(result.config.components[index].stack);
                setItem(index % COL_COUNT, index / COL_COUNT, compConfig.stack);
                var replacement = getComponent(index % COL_COUNT, index / COL_COUNT);
                if (replacement != null) {
                    currentComponent = componentResults[index];
                    replacement.setUsedHeatCapacity(compConfig.initialHeat);
                }
                if (compConfig.hasAutomation && compConfig.reactorPause > 0) {
                    int unpause = reactorTickCounter + compConfig.reactorPause;
                    nextUnpause = nextUnpause == null ? unpause : Math.max(nextUnpause, unpause);
                }
            }
        }

        private boolean sampleReactor() {
            int eupt = addedEU / 20;
            result.minEUpT = Math.min(result.minEUpT, eupt);
            result.maxEUpT = Math.max(result.maxEUpT, eupt);
            result.minHUpS = Math.min(result.minHUpS, addedHU);
            result.maxHUpS = Math.max(result.maxHUpS, addedHU);
            result.minTemp = Math.min(result.minTemp, storedHeat);
            result.maxTemp = Math.max(result.maxTemp, storedHeat);
            result.totalTempSecs += storedHeat;

            return recordTemperatureThresholds(result.simTime, true);
        }

        private boolean recordTemperatureThresholds(int time, boolean checkExplosion) {
            double ratio = storedHeat / (double) getMaxHullHeat();
            if (result.timeToEvaporate != null && result.timeToNormal == null && ratio < 0.5) {
                result.timeToNormal = time;
            }
            if (result.timeToBurn == null && ratio >= 0.4) {
                result.timeToBurn = time;
            }
            if (result.timeToEvaporate == null && ratio >= 0.5) {
                result.timeToEvaporate = time;
            }
            if (result.timeToHurt == null && ratio >= 0.7) {
                result.timeToHurt = time;
            }
            if (result.timeToLava == null && ratio >= 0.85) {
                result.timeToLava = time;
            }
            if (checkExplosion && ratio >= 1) {
                result.timeToExplode = time;
                return true;
            }
            return false;
        }
    }
}
