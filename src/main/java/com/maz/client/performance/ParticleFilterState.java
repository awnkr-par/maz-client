package com.maz.client.performance;

import com.maz.client.MazClient;
import com.maz.client.module.FpsBoosterModule;
import com.maz.client.module.Module;

/** Particle admission decisions without module-manager lookups in the particle hot path. */
public final class ParticleFilterState {
    private static Module noParticlesModule;
    private static FpsBoosterModule boosterModule;
    private static boolean initialized;
    private static boolean noParticles;
    private static boolean boosterEnabled;
    private static int keepEvery = 1;
    private static int counter;

    private ParticleFilterState() {}

    /** Called on the client thread once per client tick. */
    public static void refresh() {
        if (!initialized) {
            noParticlesModule = MazClient.MODULE_MANAGER.getModule("NoParticles");
            Module module = MazClient.MODULE_MANAGER.getModule("FPS Booster");
            if (module instanceof FpsBoosterModule booster) boosterModule = booster;
            initialized = true;
        }
        noParticles = noParticlesModule != null && noParticlesModule.isEnabled();
        boosterEnabled = boosterModule != null && boosterModule.isEnabled();
        int nextKeepEvery = boosterEnabled ? boosterModule.getParticleKeepEvery() : 1;
        if (nextKeepEvery != keepEvery || !boosterEnabled) counter = 0;
        keepEvery = nextKeepEvery;
    }

    public static boolean noParticles() { return noParticles; }

    /** Called only on the render/client thread; avoids allocations and lookups per particle. */
    public static boolean shouldDrop() {
        if (!boosterEnabled || keepEvery <= 1) return false;
        if (++counter >= keepEvery) {
            counter = 0;
            return false;
        }
        return true;
    }
}
