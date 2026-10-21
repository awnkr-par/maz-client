package com.maz.client.mixin;

import com.maz.client.MazClient;
import com.maz.client.module.FpsBoosterModule;
import com.maz.client.module.Module;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleGroup.class)
public class ParticleGroupMixin {

    private static boolean maz$modulesResolved;
    private static int maz$particlesUntilKeep;
    private static int maz$lastKeepEvery = -1;
    private static int maz$cachedKeepEvery = 1;
    private static boolean maz$cachedBoosterEnabled;
    private static boolean maz$cachedNoParticles;

    /** Resolve module state once per client tick rather than for each particle. */
    public static void maz$refreshParticleSettings() {
        maz$resolveModules();
        maz$cachedNoParticles = maz$noParticles != null && maz$noParticles.isEnabled();
        maz$cachedBoosterEnabled = maz$fpsBooster != null && maz$fpsBooster.isEnabled();
        maz$cachedKeepEvery = maz$cachedBoosterEnabled ? maz$fpsBooster.getParticleKeepEvery() : 1;
    }
    private static Module maz$noParticles;
    private static FpsBoosterModule maz$fpsBooster;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void maz$filterParticles(
            Particle particle,
            CallbackInfo ci
    ) {
        if (maz$cachedNoParticles) {
            ci.cancel();
            return;
        }

        if (!maz$cachedBoosterEnabled) {
            maz$lastKeepEvery = -1;
            maz$particlesUntilKeep = 0;
            return;
        }

        int keepEvery = maz$cachedKeepEvery;
        if (keepEvery <= 1) {
            maz$lastKeepEvery = keepEvery;
            maz$particlesUntilKeep = 0;
            return;
        }

        if (keepEvery != maz$lastKeepEvery) {
            maz$lastKeepEvery = keepEvery;
            maz$particlesUntilKeep = keepEvery - 1;
        }

        if (maz$particlesUntilKeep > 0) {
            maz$particlesUntilKeep--;
            ci.cancel();
            return;
        }

        maz$particlesUntilKeep = keepEvery - 1;
    }

    private static void maz$resolveModules() {
        if (maz$modulesResolved) {
            return;
        }

        maz$noParticles = MazClient.MODULE_MANAGER.getModule("NoParticles");
        Module module = MazClient.MODULE_MANAGER.getModule("FPS Booster");
        if (module instanceof FpsBoosterModule fpsBooster) {
            maz$fpsBooster = fpsBooster;
        }
        maz$modulesResolved = true;
    }
}
