package com.maz.client.mixin;

import com.maz.client.performance.ParticleFilterState;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void maz$filterParticles(
            Particle particle,
            CallbackInfo ci
    ) {
        if (ParticleFilterState.noParticles()) {
            ci.cancel();
            return;
        }

        if (!ParticleFilterState.boosterEnabled()) {
            ParticleFilterState.resetCounter();
            return;
        }

        int keepEvery = ParticleFilterState.keepEvery();
        if (ParticleFilterState.shouldDrop(keepEvery)) {
            ci.cancel();
        }
        }

        if (maz$particlesUntilKeep > 0) {
            maz$particlesUntilKeep--;
            ci.cancel();
            return;
        }

        maz$particlesUntilKeep = keepEvery - 1;
    }

}
