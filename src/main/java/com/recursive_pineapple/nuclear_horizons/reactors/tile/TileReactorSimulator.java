package com.recursive_pineapple.nuclear_horizons.reactors.tile;

import static com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore.COL_COUNT;
import static com.recursive_pineapple.nuclear_horizons.reactors.tile.TileReactorCore.ROW_COUNT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.TabTexture;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.google.protobuf.InvalidProtocolBufferException;
import com.recursive_pineapple.nuclear_horizons.NuclearHorizons;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.SimulationConfig;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.SimulationResult;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.Simulator;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.SimulatorProtos;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileReactorSimulator extends TileEntity implements IGuiHolder<PosGuiData> {

    @SideOnly(Side.CLIENT)
    private Simulator simulator;

    @NotNull
    private String configCode = "";

    /**
     * Must be final since the slots keep a reference to this specific config.
     */
    private final SimulationConfig config = new SimulationConfig();

    @Nullable
    private SimulationResult latestSimulation;

    public TileReactorSimulator() {

    }

    @Override
    public void updateEntity() {
        if (worldObj != null && worldObj.isRemote) {
            if (simulator == null) simulator = new Simulator(this);
            simulator.pollFinished();
        }
    }

    public void setSimulationResult(SimulationResult result) {
        latestSimulation = result;

        config.result = result;

        if (result != null) result.config = config;

        if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    public void setConfigCode(String code) {
        if (!Objects.equals(code, this.configCode)) {
            this.configCode = code;
            this.config.put(SimulationConfig.fromCode(code));

            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            markDirty();
        }
    }

    public void onConfigChanged() {
        var code = this.config.getCode();

        if (!Objects.equals(code, configCode)) {
            this.configCode = code;
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            markDirty();
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        var nbt = new NBTTagCompound();

        if (this.latestSimulation != null) {
            nbt.setByteArray(
                "history",
                this.latestSimulation.save()
                    .toByteArray());
        }

        nbt.setString("code", configCode);

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, blockMetadata, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        var nbt = pkt.func_148857_g();

        setConfigCode(nbt.getString("code"));

        var data = nbt.getByteArray("history");
        if (data.length > 0) {
            try {
                setSimulationResult(SimulationResult.load(SimulatorProtos.SimulationResult.parseFrom(data)));
            } catch (InvalidProtocolBufferException e) {
                NuclearHorizons.LOG.error("Received invalid simulation result", e);
                setSimulationResult(null);
            }
        } else {
            setSimulationResult(null);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        compound.setInteger("version", 1);
        compound.setString("config", configCode);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        switch (compound.getInteger("version")) {
            case 1: {
                setConfigCode(compound.getString("config"));
                break;
            }
        }
    }

    // #region UI

    // Placeholder tab icons
    private static final UITexture TAB_ICON_GRID = UITexture.fullImage("nuclear_horizons", "blocks/thermal_sensor");
    private static final UITexture TAB_ICON_RESULTS = UITexture.fullImage("nuclear_horizons", "blocks/redstone_port");
    private static final UITexture TAB_ICON_SETTINGS = UITexture
        .fullImage("nuclear_horizons", "blocks/access_hatch.png");

    private static final TabTexture TAB_TOP = TabTexture
        .of(UITexture.fullImage("nuclear_horizons", "gui/tabs_top", ColorType.DEFAULT), GuiAxis.Y, false, 28, 32, 4);

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        StringSyncValue codeSync = new StringSyncValue(() -> this.configCode, this::setConfigCode).allowC2S();
        syncManager.syncValue("code", codeSync);

        PagedWidget.Controller controller = new PagedWidget.Controller();

        ModularPanel panel = ModularPanel.defaultPanel("reactor_simulator", 222, 234);

        panel.child(
            Flow.row()
                .coverChildren()
                .pos(5, -28)
                .child(
                    new PageButton(0, controller).tab(TAB_TOP, -1)
                        .overlay(
                            TAB_ICON_GRID.asIcon()
                                .size(16))
                        .addTooltipLine(IKey.lang("nh_gui.sim.title")))
                .child(
                    new PageButton(1, controller).tab(TAB_TOP, 0)
                        .overlay(
                            TAB_ICON_RESULTS.asIcon()
                                .size(16))
                        .addTooltipLine(IKey.lang("nh_gui.sim.results.title")))
                .child(
                    new PageButton(2, controller).tab(TAB_TOP, 1)
                        .overlay(
                            TAB_ICON_SETTINGS.asIcon()
                                .size(16))
                        .addTooltipLine(IKey.lang("nh_gui.sim.settings.title"))));

        panel.child(
            new PagedWidget<>().controller(controller)
                .pos(0, 0)
                .sizeRel(1f)
                .addPage(gridPage())
                .addPage(resultsPage())
                .addPage(settingsPage(codeSync)));

        return panel;
    }

    private IWidget gridPage() {
        char[] rowChars = new char[COL_COUNT];
        Arrays.fill(rowChars, 'I');
        String[] matrix = new String[ROW_COUNT];
        Arrays.fill(matrix, new String(rowChars));

        return Flow.column()
            .sizeRel(1f)
            .padding(7)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .childPadding(2)
            .child(new TextWidget<>(IKey.lang("nh_gui.sim.title")))
            .child(
                SlotGroupWidget.builder()
                    .matrix(matrix)
                    .key(
                        'I',
                        index -> new PhantomItemSlot().slot(
                            new ModularSlot(this.config, index).changeListener(
                                (stack, onlyAmountChanged, client, init) -> {
                                    if (client && !init) onConfigChanged();
                                })))
                    .build())
            .child(
                Flow.row()
                    .coverChildren()
                    .childPadding(2)
                    .child(
                        new ButtonWidget<>().size(48, 16)
                            .overlay(IKey.lang("nh_gui.sim.actions.start"))
                            .onMousePressed(mb -> {
                                if (NetworkUtils.isClient()) {
                                    if (simulator == null) simulator = new Simulator(this);
                                    simulator.start(config);
                                }
                                return true;
                            }))
                    .child(
                        new ButtonWidget<>().size(48, 16)
                            .overlay(IKey.lang("nh_gui.sim.actions.cancel"))
                            .onMousePressed(mb -> {
                                if (NetworkUtils.isClient() && simulator != null) {
                                    simulator.cancel();
                                }
                                return true;
                            })))
            .child(SlotGroupWidget.playerInventory(false));
    }

    private static void addResultLine(ArrayList<String> lines, String name, String unit, Object value) {
        var text = I18n.format(
            "nh_gui.sim.results." + name,
            I18n.format(value == null ? "nh_gui.sim.results.none" : ("nh_gui.sim.results." + unit), value));

        lines.addAll(Arrays.asList(text.split("\\\\n")));
    }

    private IWidget resultsPage() {
        return Flow.column()
            .sizeRel(1f)
            .padding(7)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(new TextWidget<>(IKey.lang("nh_gui.sim.results.title")))
            .child(new TextWidget<>(IKey.dynamic(() -> {
                var r = latestSimulation;

                ArrayList<String> lines = new ArrayList<>();

                if (r == null) {
                    lines.add(I18n.format("nh_gui.sim.results.no_results"));
                } else if (r.timeToExplode == null) {
                    addResultLine(lines, "runtime", "seconds", r.simTime);
                }

                if (r != null) {
                    if (r.timeToNormal != null) {
                        addResultLine(lines, "time_to_normal", "seconds", r.timeToNormal);
                    }
                    if (r.timeToBurn != null) {
                        addResultLine(lines, "time_to_burn", "seconds", r.timeToBurn);
                    }
                    if (r.timeToEvaporate != null) {
                        addResultLine(lines, "time_to_evaporate", "seconds", r.timeToEvaporate);
                    }
                    if (r.timeToHurt != null) {
                        addResultLine(lines, "time_to_hurt", "seconds", r.timeToHurt);
                    }
                    if (r.timeToLava != null) {
                        addResultLine(lines, "time_to_lava", "seconds", r.timeToLava);
                    }
                    if (r.timeToExplode != null) {
                        addResultLine(lines, "time_to_explosion", "seconds", r.timeToExplode);
                    }

                    addResultLine(lines, "active_time", "seconds", r.activeTime);
                    addResultLine(lines, "inactive_time", "seconds", r.pausedTime);

                    addResultLine(lines, "avg_power", "eu_per_tick", r.totalEU / 20 / Math.max(1, r.simTime));
                    addResultLine(lines, "min_power", "eu_per_tick", r.minEUpT);
                    addResultLine(lines, "max_power", "eu_per_tick", r.maxEUpT);

                    addResultLine(lines, "avg_vent_cooling", "hu_per_sec", r.totalHU / Math.max(1, r.simTime));
                    addResultLine(lines, "min_vent_cooling", "hu_per_sec", r.minHUpS);
                    addResultLine(lines, "max_vent_cooling", "hu_per_sec", r.maxHUpS);

                    addResultLine(lines, "avg_hull_temp", "hu_total", r.totalTempSecs / Math.max(1, r.simTime));
                    addResultLine(lines, "min_hull_temp", "hu_total", r.minTemp);
                    addResultLine(lines, "max_hull_temp", "hu_total", r.maxTemp);
                }

                return String.join("\n", lines);
            })));
    }

    private IWidget settingsPage(StringSyncValue codeSync) {
        return Flow.column()
            .sizeRel(1f)
            .padding(7)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .childPadding(2)
            .child(new TextWidget<>(IKey.lang("nh_gui.sim.settings.title")))
            .child(textSetting("nh_gui.sim.settings.planner_code", codeSync))
            .child(
                numericSetting(
                    "nh_gui.sim.settings.max_simulation_ticks",
                    () -> this.config.maxSimulationTicks,
                    v -> this.config.maxSimulationTicks = v))
            .child(booleanSetting("nh_gui.sim.settings.fluid", () -> this.config.fluid, v -> this.config.fluid = v))
            .child(booleanSetting("nh_gui.sim.settings.pulsed", () -> this.config.pulsed, v -> this.config.pulsed = v))
            .child(
                numericSetting("nh_gui.sim.settings.on_pulse", () -> this.config.onPulse, v -> this.config.onPulse = v))
            .child(
                numericSetting(
                    "nh_gui.sim.settings.off_pulse",
                    () -> this.config.offPulse,
                    v -> this.config.offPulse = v))
            .child(
                numericSetting(
                    "nh_gui.sim.settings.suspend_temp",
                    () -> this.config.suspendTemp,
                    v -> this.config.suspendTemp = v))
            .child(
                numericSetting(
                    "nh_gui.sim.settings.resume_temp",
                    () -> this.config.resumeTemp,
                    v -> this.config.resumeTemp = v))
            .child(
                numericSetting(
                    "nh_gui.sim.settings.initial_reactor_heat",
                    () -> this.config.initialHeat,
                    v -> this.config.initialHeat = v));
    }

    private static final int SETTING_LABEL_WIDTH = 108;

    private IWidget labeledRow(String labelKey, IWidget control) {
        return Flow.row()
            .coverChildrenHeight()
            .widthRel(1f)
            .childPadding(4)
            .child(
                new TextWidget<>(IKey.lang(labelKey)).size(SETTING_LABEL_WIDTH, 14)
                    .textAlign(Alignment.CenterRight))
            .child(control);
    }

    private IWidget textSetting(String labelKey, StringSyncValue value) {
        return labeledRow(
            labelKey,
            new TextFieldWidget().value(value)
                .size(96, 14));
    }

    private IWidget numericSetting(String labelKey, java.util.function.IntSupplier getter,
        java.util.function.IntConsumer setter) {
        return labeledRow(
            labelKey,
            new TextFieldWidget().formatAsInteger(true)
                .numbersInt(0, Integer.MAX_VALUE)
                .value(new IntValue.Dynamic(getter, v -> {
                    setter.accept(v);
                    onConfigChanged();
                }))
                .size(96, 14));
    }

    private IWidget booleanSetting(String labelKey, java.util.function.BooleanSupplier getter,
        java.util.function.Consumer<Boolean> setter) {
        String trueText = I18n.format("nh_gui.sim.settings.true"), falseText = I18n.format("nh_gui.sim.settings.false");

        return labeledRow(
            labelKey,
            new ButtonWidget<>().size(40, 14)
                .overlay(IKey.dynamic(() -> getter.getAsBoolean() ? trueText : falseText))
                .onMousePressed(mb -> {
                    setter.accept(!getter.getAsBoolean());
                    onConfigChanged();
                    return true;
                }));
    }

    // #endregion
}
