package com.recursive_pineapple.nuclear_horizons.qa;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClickTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.gtnewhorizons.horizonqa.api.client.ClientTarget;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;
import com.recursive_pineapple.nuclear_horizons.reactors.blocks.BlockList;
import com.recursive_pineapple.nuclear_horizons.reactors.tile.simulator.SimulationConfig;

import gregtech.api.enums.ItemList;

@GameTestHolder(value = "nuclear_horizons", clientOnly = true, requiredMods = { "gregtech" })
public final class ClientSimulatorTests {

    private ClientSimulatorTests() {}

    @GameTest(template = "reactor_parity", timeoutTicks = 1200)
    public static void startButtonProducesVisibleResults(GameTestHelper helper) {
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(100)
            .server("place simulator and supply a fuel rod", () -> prepare(helper))
            .withinTicks(100)
            .awaitClient("crosshair points at the simulator", c -> {
                var hit = net.minecraft.client.Minecraft.getMinecraft().objectMouseOver;
                TestPos simulator = helper.absolute(2, 1, 1);
                if (c.screen() != null || hit == null
                    || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                    || hit.blockX != simulator.x()
                    || hit.blockY != simulator.y()
                    || hit.blockZ != simulator.z()) {
                    throw new AssertionError("The simulator is not yet under the crosshair: " + hit);
                }
            })
            .useHeldItem()
            .awaitClient("simulator GUI opens", c -> c.screen(GuiContainerWrapper.class))
            .click(
                widget(
                    "uranium rod in player inventory",
                    w -> w instanceof ItemSlot slot && !(w instanceof PhantomItemSlot)
                        && ItemStack.areItemStacksEqual(
                            slot.getSlot()
                                .getStack(),
                            ItemList.RodUranium.get(1))))
            .click(
                widget(
                    "first simulator slot",
                    w -> w instanceof PhantomItemSlot slot && slot.getSlot()
                        .getSlotIndex() == 0))
            .capture("simulator-grid-one-rod")
            .click(
                widget(
                    "Start button",
                    w -> w instanceof ButtonWidget<?>button && button.getOverlay() instanceof IKey key
                        && "Start".equals(key.get())))
            .click(widget("Results tab", w -> w instanceof PageButton page && page.getIndex() == 1))
            .withinTicks(120)
            .awaitClient("simulation result appears in the GUI", c -> {
                String text = resultText(c.screen(GuiContainerWrapper.class));
                if (!text.contains("Avg Power: 10 EU/t")) throw new AssertionError("Visible results: " + text);
            })
            .capture("simulator-start-result")
            .click(widget("Grid tab", w -> w instanceof PageButton page && page.getIndex() == 0))
            .click(
                widget(
                    "second simulator slot",
                    w -> w instanceof PhantomItemSlot slot && slot.getSlot()
                        .getSlotIndex() == 1))
            .capture("simulator-grid-two-rods")
            .click(widget("Results tab", w -> w instanceof PageButton page && page.getIndex() == 1))
            .awaitClient("changing the grid clears the previous result", c -> {
                String text = resultText(c.screen(GuiContainerWrapper.class));
                if (!text.contains("No simulation results available"))
                    throw new AssertionError("Previous result was not cleared: " + text);
            })
            .click(widget("Grid tab", w -> w instanceof PageButton page && page.getIndex() == 0))
            .click(
                widget(
                    "Start button",
                    w -> w instanceof ButtonWidget<?>button && button.getOverlay() instanceof IKey key
                        && "Start".equals(key.get())))
            .click(widget("Results tab", w -> w instanceof PageButton page && page.getIndex() == 1))
            .withinTicks(120)
            .awaitClient("updated simulation result appears in the GUI", c -> {
                String text = resultText(c.screen(GuiContainerWrapper.class));
                if (!text.contains("Avg Power: 40 EU/t")) throw new AssertionError("Visible results: " + text);
            })
            .capture("simulator-updated-result")
            .succeed();
    }

    @GameTest(template = "reactor_parity", timeoutTicks = 1200)
    public static void componentGridSelectsAndPersistsAutomation(GameTestHelper helper) {
        ClientScenario scenario = ClientTest.scenario(helper)
            .defaultTimeoutTicks(100)
            .server("place simulator and supply a fuel rod", () -> prepare(helper))
            .awaitClient("crosshair points at the simulator", c -> {
                var hit = net.minecraft.client.Minecraft.getMinecraft().objectMouseOver;
                TestPos simulator = helper.absolute(2, 1, 1);
                if (c.screen() != null || hit == null
                    || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                    || hit.blockX != simulator.x()
                    || hit.blockY != simulator.y()
                    || hit.blockZ != simulator.z()) {
                    throw new AssertionError("The simulator is not yet under the crosshair: " + hit);
                }
            })
            .useHeldItem()
            .awaitScreen(GuiContainerWrapper.class)
            .click(
                widget(
                    "uranium rod in inventory",
                    w -> w instanceof ItemSlot slot && !(w instanceof PhantomItemSlot)
                        && ItemStack.areItemStacksEqual(
                            slot.getSlot()
                                .getStack(),
                            ItemList.RodUranium.get(1))))
            .click(
                widget(
                    "first reactor slot",
                    w -> w instanceof PhantomItemSlot slot && slot.getSlot()
                        .getSlotIndex() == 0))
            .click(
                widget(
                    "last reactor slot",
                    w -> w instanceof PhantomItemSlot slot && slot.getSlot()
                        .getSlotIndex() == 53))
            .click(widget("Settings tab", w -> w instanceof PageButton page && page.getIndex() == 2))
            .click(settingControl("nh_gui.sim.settings.automated"))
            .click(widget("Component settings tab", w -> w instanceof PageButton page && page.getIndex() == 3))
            .click(componentSlot(0))
            .click(settingControl("nh_gui.sim.comp_settings.automated"));
        enterNumber(scenario, "nh_gui.sim.comp_settings.threshold", "111");
        enterNumber(scenario, "nh_gui.sim.comp_settings.pause", "7");
        scenario.capture("component-first-automated")
            .click(componentSlot(53))
            .awaitClient(
                "last slot settings start independently",
                c -> assertField(c, "nh_gui.sim.comp_settings.threshold", "0"))
            .click(settingControl("nh_gui.sim.comp_settings.automated"));
        enterNumber(scenario, "nh_gui.sim.comp_settings.threshold", "222");
        enterNumber(scenario, "nh_gui.sim.comp_settings.pause", "13");
        scenario.capture("component-two-automation-markers")
            .click(componentSlot(1))
            .awaitClient("empty slot is selected without inserting the held rod", c -> {
                if (!resultText(c.screen(GuiContainerWrapper.class)).contains("Select an occupied slot"))
                    throw new AssertionError("Expected the empty-slot message");
            })
            .capture("component-empty-selection")
            .click(componentSlot(0))
            .awaitClient(
                "first slot retains its settings",
                c -> assertField(c, "nh_gui.sim.comp_settings.threshold", "111"))
            .click(settingControl("nh_gui.sim.comp_settings.automated"))
            .capture("component-first-marker-disabled")
            .awaitServer("slot settings synchronize independently", () -> assertComponentConfig(helper))
            .escape()
            .awaitClient(
                "simulator screen closes",
                c -> { if (c.screen() != null) throw new AssertionError("Screen still open"); })
            .useHeldItem()
            .awaitScreen(GuiContainerWrapper.class)
            .click(widget("Component settings tab", w -> w instanceof PageButton page && page.getIndex() == 3))
            .click(componentSlot(53))
            .awaitClient("reopening preserves last slot settings", c -> {
                assertField(c, "nh_gui.sim.comp_settings.threshold", "222");
                assertField(c, "nh_gui.sim.comp_settings.pause", "13");
            })
            .capture("component-settings-reopened")
            .awaitServer("serialized settings preserve both components", () -> assertComponentConfig(helper))
            .succeed();
    }

    private static ClientTarget componentSlot(int index) {
        return widget("component settings slot " + (index + 1), w -> {
            if (!(w instanceof ButtonWidget<?>)) return false;
            IWidget row = w.getParent();
            IWidget grid = row.getParent();
            return row.getChildren()
                .size() == 9
                && grid.getChildren()
                    .size() == 6
                && row.getChildren()
                    .stream()
                    .allMatch(child -> child instanceof ButtonWidget<?>)
                && grid.getChildren()
                    .indexOf(row) == index / 9
                && row.getChildren()
                    .indexOf(w) == index % 9;
        });
    }

    private static boolean isSettingControl(IWidget widget, String key) {
        return (widget instanceof ButtonWidget<?> || widget instanceof TextFieldWidget) && widget.getParent()
            .getChildren()
            .stream()
            .anyMatch(
                sibling -> sibling instanceof TextWidget<?>text && I18n.format(key)
                    .equals(
                        text.getKey()
                            .get()));
    }

    private static ClientTarget settingControl(String key) {
        return widget(key, w -> isSettingControl(w, key));
    }

    private static void enterNumber(ClientScenario scenario, String key, String value) {
        scenario.click(settingControl(key))
            .key(Keyboard.KEY_HOME, '\0')
            .key(Keyboard.KEY_DELETE, '\0');
        for (char digit : value.toCharArray()) scenario.key(Keyboard.KEY_0, digit);
        scenario.key(Keyboard.KEY_RETURN, '\r');
    }

    private static void assertField(ClientTest client, String key, String value) {
        IWidget found = WidgetTree.foreachChildWithResult(
            client.screen(GuiContainerWrapper.class)
                .getScreen()
                .getMainPanel(),
            w -> isSettingControl(w, key) ? w : null,
            true);
        if (!(found instanceof TextFieldWidget field) || !value.equals(field.getText()))
            throw new AssertionError("Expected " + key + " = " + value + ", found " + found);
    }

    private static void assertComponentConfig(GameTestHelper helper) {
        NBTTagCompound saved = new NBTTagCompound();
        TestPos position = helper.absolute(2, 1, 1);
        helper.getWorld()
            .getTileEntity(position.x(), position.y(), position.z())
            .writeToNBT(saved);
        SimulationConfig config = SimulationConfig.fromCode(saved.getString("config"));
        if (config.automated) throw new AssertionError("Per-slot automation changed global Automated Replacement");
        for (int slot = 0; slot < config.components.length; slot++) {
            if (slot == 0 || slot == 53) continue;
            if (config.components[slot] != null)
                throw new AssertionError("Selection inserted a component in slot " + slot);
        }
        var first = config.components[0];
        var last = config.components[53];
        if (first == null || last == null
            || first.hasAutomation
            || !last.hasAutomation
            || first.replacementThreshold != 111
            || first.reactorPause != 7
            || last.replacementThreshold != 222
            || last.reactorPause != 13
            || !ItemStack.areItemStacksEqual(first.stack, ItemList.RodUranium.get(1))
            || !ItemStack.areItemStacksEqual(last.stack, ItemList.RodUranium.get(1)))
            throw new AssertionError("Component settings or reactor layout were not preserved");
    }

    private static void prepare(GameTestHelper helper) {
        helper.setBlock(2, 1, 1, BlockList.REACTOR_SIMULATOR);
        if (helper.getWorld().playerEntities.size() != 1) throw new AssertionError("Expected one client test player");
        EntityPlayerMP player = (EntityPlayerMP) helper.getWorld().playerEntities.get(0);
        int heldSlot = player.inventory.currentItem;
        ItemStack previousHeld = player.inventory.getStackInSlot(heldSlot);
        ItemStack previousRod = player.inventory.getStackInSlot(9);
        helper.afterTest(() -> {
            player.inventory.setInventorySlotContents(heldSlot, previousHeld);
            player.inventory.setInventorySlotContents(9, previousRod);
            player.inventoryContainer.detectAndSendChanges();
        });
        player.inventory.setInventorySlotContents(heldSlot, new ItemStack(Items.stick));
        player.inventory.setInventorySlotContents(9, ItemList.RodUranium.get(1));
        player.inventoryContainer.detectAndSendChanges();
        TestPos position = helper.absolute(2, 1, 4);
        player.playerNetServerHandler
            .setPlayerLocation(position.x() + 0.5, position.y(), position.z() + 0.5, 180.0f, 20.0f);
    }

    private static ClientTarget widget(String description, Predicate<IWidget> matches) {
        return ClientTarget.of(description, c -> {
            GuiContainerWrapper wrapper = c.screen(GuiContainerWrapper.class);
            IWidget found = WidgetTree.foreachChildWithResult(
                wrapper.getScreen()
                    .getMainPanel(),
                w -> matches.test(w) ? w : null,
                true);
            if (found == null || !found.isEnabled()) return null;
            return new ClickTarget(found, new Rectangle(found.getArea()), found::isHovering);
        });
    }

    private static String resultText(GuiContainerWrapper wrapper) {
        List<String> texts = new ArrayList<>();
        WidgetTree.foreachChildWithResult(
            wrapper.getScreen()
                .getMainPanel(),
            w -> {
                if (w instanceof TextWidget<?>text) texts.add(
                    text.getKey()
                        .get());
                return null;
            },
            true);
        return String.join(" | ", texts);
    }
}
