package com.signalvfx.plugin;

import com.signalvfx.plugin.cast.CastService;
import com.signalvfx.plugin.command.SkillCommand;
import com.signalvfx.plugin.damage.DamageEngine;
import com.signalvfx.plugin.vfx.VfxService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SignalVFX Paper plugin entry point. Loads editor-authored skill JSON and wires
 * together the services that execute them: target resolution, VFX rendering
 * (BetterModel when present, otherwise display entities / resource-pack items)
 * and the damage engine.
 */
public final class SignalVfxPlugin extends JavaPlugin {

    private SkillRegistry registry;
    private VfxService vfxService;
    private DamageEngine damageEngine;
    private CastService castService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        registry = new SkillRegistry(this);
        registry.loadAll();

        vfxService = new VfxService(this);
        damageEngine = new DamageEngine(this);
        castService = new CastService(this, vfxService, damageEngine);

        SkillCommand command = new SkillCommand(this, registry, castService);
        getCommand("svfx").setExecutor(command);
        getCommand("svfx").setTabCompleter(command);

        getLogger().info("SignalVFX enabled with " + registry.count() + " skill(s). "
                + "BetterModel integration: " + (vfxService.isBetterModelAvailable() ? "active" : "not installed"));
    }

    public SkillRegistry registry() {
        return registry;
    }

    public CastService castService() {
        return castService;
    }
}
