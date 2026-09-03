package ru.euphoria.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.euphoria.tools.AutoFishSoundHandler;

@Mixin(ClientPacketListener.class)
public abstract class SoundMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        AutoFishSoundHandler.INSTANCE.handle(packet);
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"))
    private void onSoundEntity(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        AutoFishSoundHandler.INSTANCE.handleSoundEntity(packet);
    }
}
