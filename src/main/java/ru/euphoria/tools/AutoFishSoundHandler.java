package ru.euphoria.tools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public final class AutoFishSoundHandler {
    public static final AutoFishSoundHandler INSTANCE = new AutoFishSoundHandler();
    private long lastBiteTime = 0;

    private AutoFishSoundHandler() {
    }

    private boolean isFishBiteSound(String soundPath) {
        if (soundPath == null) return false;
        String lower = soundPath.toLowerCase();
        if (lower.contains("fishing_bobber.splash") || lower.contains("bobber.splash")) {
            return true;
        }
        if (lower.contains("note_block") || lower.contains("note.") || lower.contains("pling")) {
            return true;
        }
        return false;
    }

    private boolean isNoteBlockSound(String soundPath) {
        if (soundPath == null) return false;
        String lower = soundPath.toLowerCase();
        return lower.contains("note_block") || lower.contains("note.") || lower.contains("pling");
    }

    public void handle(ClientboundSoundPacket packet) {
        if (packet == null) return;
        if (MythicalFishHandler.INSTANCE.isFighting()) return;
        long now = System.currentTimeMillis();
        if (now - lastBiteTime < 1000) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null) return;
        FishingHook bobber = player.fishing;
        if (bobber == null) return;

        SoundEvent soundEvent = (SoundEvent) packet.getSound().value();
        if (soundEvent == null || soundEvent.location() == null) return;
        String soundPath = soundEvent.location().getPath();
        if (!isFishBiteSound(soundPath)) return;

        Vec3 soundPos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        if (isNoteBlockSound(soundPath)) {
            double distBobber = bobber.position().distanceTo(soundPos);
            double distPlayer = player.position().distanceTo(soundPos);
            if (distBobber <= 24.0 || distPlayer <= 24.0) {
                lastBiteTime = now;
                AutoFish.INSTANCE.onFishBite();
            }
        } else {
            if (bobber.position().distanceTo(soundPos) <= 2.5) {
                lastBiteTime = now;
                AutoFish.INSTANCE.onFishBite();
            }
        }
    }

    public void handleSoundEntity(ClientboundSoundEntityPacket packet) {
        if (packet == null) return;
        if (MythicalFishHandler.INSTANCE.isFighting()) return;
        long now = System.currentTimeMillis();
        if (now - lastBiteTime < 1000) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null) return;
        FishingHook bobber = player.fishing;
        if (bobber == null) return;

        SoundEvent soundEvent = (SoundEvent) packet.getSound().value();
        if (soundEvent == null || soundEvent.location() == null) return;
        String soundPath = soundEvent.location().getPath();
        if (!isFishBiteSound(soundPath)) return;

        int entityId = packet.getId();
        if (entityId == player.getId() || entityId == bobber.getId() || isNoteBlockSound(soundPath)) {
            lastBiteTime = now;
            AutoFish.INSTANCE.onFishBite();
        }
    }

    public void handleSoundInstance(SoundInstance sound) {
        if (sound == null || sound.getIdentifier() == null) return;
        if (MythicalFishHandler.INSTANCE.isFighting()) return;
        long now = System.currentTimeMillis();
        if (now - lastBiteTime < 1000) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        LocalPlayer player = client.player;
        if (player == null) return;
        FishingHook bobber = player.fishing;
        if (bobber == null) return;

        String soundPath = sound.getIdentifier().getPath();
        if (!isFishBiteSound(soundPath)) return;

        if (isNoteBlockSound(soundPath)) {
            if (sound.isRelative()) {
                lastBiteTime = now;
                AutoFish.INSTANCE.onFishBite();
            } else {
                Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
                double distBobber = bobber.position().distanceTo(soundPos);
                double distPlayer = player.position().distanceTo(soundPos);
                if (distBobber <= 24.0 || distPlayer <= 24.0) {
                    lastBiteTime = now;
                    AutoFish.INSTANCE.onFishBite();
                }
            }
        } else {
            Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            if (bobber.position().distanceTo(soundPos) <= 2.5) {
                lastBiteTime = now;
                AutoFish.INSTANCE.onFishBite();
            }
        }
    }
}
