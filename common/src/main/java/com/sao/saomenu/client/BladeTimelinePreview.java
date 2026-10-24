package com.sao.saomenu.client;

import com.sao.saomenu.SAOMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Development-only original blade stage preview; the parent activates it only on explicit request. */
final class BladeTimelinePreview {
    private static int age;
    private BladeTimelinePreview() { }

    static boolean tick(Minecraft client, String out) {
        if (client.player == null || client.level == null) return false;
        if (client.screen instanceof PauseScreen) client.setScreen(null);
        if (client.screen != null) return false;
        age++;
        if (age == 1) {
            SAOWelcome.dismiss();
            client.options.guiScale().set(2);
            client.resizeDisplay();
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            client.getToasts().clear();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            client.player.connection.sendCommand("fill ~-20 119 ~-20 ~20 119 ~20 minecraft:gray_concrete");
            client.player.connection.sendCommand("tp @s ~ 120 ~ 0 12");
            client.player.connection.sendCommand("time set 1000");
        }
        if (age == 10) cast(client, "elucidator", false, "elucidator", 4);
        if (age == 26) {
            requireCharge(client, 28);
            SAOMenuPreview.grab(client, out, "blade_black_charge_after_eight.png");
        }
        if (age == 34) SAOMenuPreview.grab(client, out, "blade_black_charge_late.png");
        if (age == 50) SAOMenuPreview.grab(client, out, "blade_black_finish_aura.png");
        if (age == 60 && !client.player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            throw new IllegalStateException("Original black finisher strength did not reach the client");
        }
        if (age == 100) cast(client, "dark_repulser", false, "dark_repulser", 4);
        if (age == 116) {
            requireCharge(client, 28);
            SAOMenuPreview.grab(client, out, "blade_white_charge_after_eight.png");
        }
        if (age == 145) SAOMenuPreview.grab(client, out, "blade_white_last_release.png");
        if (age == 158) SAOMenuPreview.grab(client, out, "blade_white_finish_aura.png");
        if (age == 170 && !client.player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            throw new IllegalStateException("Original white finisher speed did not reach the client");
        }
        if (age == 200) cast(client, "elucidator", true, "dual_blades", 3);
        if (age == 230) SAOMenuPreview.grab(client, out, "blade_dual_sixteen_release.png");
        if (age == 284) SAOMenuPreview.grab(client, out, "blade_dual_sixteen_aura.png");
        if (age == 340) cast(client, "elucidator", true, "dual_blades", 4);
        if (age == 356) {
            requireCharge(client, 28);
            SAOMenuPreview.grab(client, out, "blade_dual_charge_after_eight.png");
        }
        if (age == 484) SAOMenuPreview.grab(client, out, "blade_dual_finish_aura.png");
        if (age == 540) {
            SAOMenu.LOGGER.info("[SAOMenu] original blade charge lifetime, buff sync and stage preview complete");
            client.stop();
            return true;
        }
        return false;
    }

    private static void cast(Minecraft client, String item, boolean dual, String style, int slot) {
        var server = client.getSingleplayerServer();
        var id = client.player.getUUID();
        server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(id);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(
                        new ResourceLocation("saomod_rewrite", item))));
                player.setItemInHand(InteractionHand.OFF_HAND, dual ? new ItemStack(BuiltInRegistries.ITEM.get(
                        new ResourceLocation("saomod_rewrite", "dark_repulser"))) : ItemStack.EMPTY);
                var data = (net.minecraft.nbt.CompoundTag) player.getClass().getMethod("getPersistentData").invoke(player);
                data.putInt("SaoWeaponMastery_saomod_rewrite:weapon/" + style, 1000);
                data.putInt("SaoWeaponCombo", 3);
                var holder = Class.forName("cn.saomod.rewrite.platform.forge.weapon.WeaponSkillCasterHolder");
                var caster = ((java.util.Optional<?>) holder.getMethod("current").invoke(null)).orElseThrow();
                var skills = caster.getClass().getDeclaredField("skills"); skills.setAccessible(true);
                var applicationHolder = skills.get(caster);
                var application = ((java.util.Optional<?>) applicationHolder.getClass().getMethod("current")
                        .invoke(applicationHolder)).orElseThrow();
                var states = application.getClass().getDeclaredField("states"); states.setAccessible(true);
                var repository = states.get(application);
                var repositoryType = Class.forName("cn.saomod.rewrite.domain.skill.ProfessionStateRepository");
                var stateType = Class.forName("cn.saomod.rewrite.domain.skill.ProfessionState");
                var loaded = (java.util.Optional<?>) repositoryType.getMethod("load", java.util.UUID.class).invoke(repository, id);
                var state = loaded.isPresent() ? loaded.get() : stateType.getMethod("empty").invoke(null);
                int maximum = (Integer) stateType.getMethod("maximumResource").invoke(state);
                var staminaType = Class.forName("cn.saomod.rewrite.domain.player.Stamina");
                var stamina = staminaType.getConstructor(int.class, int.class, int.class).newInstance(maximum, maximum, 0);
                var refilled = stateType.getMethod("withStamina", staminaType).invoke(state, stamina);
                repositoryType.getMethod("save", java.util.UUID.class, stateType).invoke(repository, id, refilled);
                int result = server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2),
                        "saomod weapon cast saomod_rewrite:weapon/" + style + " " + slot);
                if (result != 1) throw new IllegalStateException("Actual original blade preview cast rejected: " + style);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot prepare isolated original blade preview", failure);
            }
        });
    }

    private static void requireCharge(Minecraft client, int duration) {
        try {
            var renderer = Class.forName("cn.saomod.rewrite.platform.client.WeaponEffectRenderer");
            var active = renderer.getDeclaredField("ACTIVE"); active.setAccessible(true);
            for (Object effect : (java.util.List<?>) active.get(null)) {
                var accessor = effect.getClass().getDeclaredMethod("message"); accessor.setAccessible(true);
                Object message = accessor.invoke(effect);
                var type = message.getClass();
                if ((Integer) type.getMethod("owner").invoke(message) == client.player.getId()
                        && (Integer) type.getMethod("phase").invoke(message) == 0
                        && (Integer) type.getMethod("durationTicks").invoke(message) == duration) return;
            }
            throw new IllegalStateException("Original blade charge was truncated before its full windup");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot verify original blade client lifetime", failure);
        }
    }
}
