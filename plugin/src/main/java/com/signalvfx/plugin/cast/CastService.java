package com.signalvfx.plugin.cast;

import com.signalvfx.model.Cost;
import com.signalvfx.model.Origin;
import com.signalvfx.model.Skill;
import com.signalvfx.model.DamagePoint;
import com.signalvfx.plugin.damage.DamageEngine;
import com.signalvfx.plugin.util.Coords;
import com.signalvfx.plugin.util.Keys;
import com.signalvfx.plugin.vfx.VfxService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Runs a skill for a caster: checks cooldown and cost, resolves the target,
 * triggers the visual and schedules each damage point.
 */
public final class CastService {

    private final JavaPlugin plugin;
    private final VfxService vfx;
    private final DamageEngine damage;
    private final TargetResolver resolver = new TargetResolver();
    private final Cooldowns cooldowns = new Cooldowns();

    public CastService(JavaPlugin plugin, VfxService vfx, DamageEngine damage) {
        this.plugin = plugin;
        this.vfx = vfx;
        this.damage = damage;
    }

    public void cast(Player caster, Skill skill) {
        if (!cooldowns.isReady(caster.getUniqueId(), skill.getId())) {
            long secs = (cooldowns.remainingMillis(caster.getUniqueId(), skill.getId()) + 999) / 1000;
            caster.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§7" + skill.getName() + " on cooldown (" + secs + "s)"));
            return;
        }
        if (!payCost(caster, skill.getCast().getCost())) {
            caster.sendActionBar(net.kyori.adventure.text.Component.text("§cNot enough resources"));
            return;
        }
        cooldowns.set(caster.getUniqueId(), skill.getId(), skill.getCast().getCooldownTicks());

        Target target = resolver.resolve(caster, skill.getCast());
        vfx.play(caster, target, skill.getVisual());

        for (DamagePoint dp : skill.getDamagePoints()) {
            Location base = baseFor(caster, target, dp.getOrigin());
            Vector forward = forwardFor(caster, target, dp.getOrigin());
            damage.schedule(caster, base, forward, dp);
        }
    }

    private Location baseFor(Player caster, Target target, Origin origin) {
        return switch (origin) {
            case CASTER -> caster.getLocation();
            case TARGET -> target.origin().clone();
            case CAST_DIRECTION -> caster.getEyeLocation();
        };
    }

    private Vector forwardFor(Player caster, Target target, Origin origin) {
        return switch (origin) {
            case CASTER -> caster.getLocation().getDirection();
            case TARGET, CAST_DIRECTION -> target.direction().clone();
        };
    }

    private boolean payCost(Player caster, Cost cost) {
        double amount = cost.getAmount();
        switch (cost.getType()) {
            case NONE, MANA -> {
                // MANA has no built-in resource; treat as free (integrate an external mana plugin later).
                return true;
            }
            case HEALTH -> {
                if (caster.getHealth() <= amount) {
                    return false;
                }
                caster.setHealth(Math.max(1.0, caster.getHealth() - amount));
                return true;
            }
            case HUNGER -> {
                if (caster.getFoodLevel() < amount) {
                    return false;
                }
                caster.setFoodLevel((int) Math.max(0, caster.getFoodLevel() - amount));
                return true;
            }
            case EXPERIENCE -> {
                if (caster.getTotalExperience() < amount) {
                    return false;
                }
                caster.giveExp((int) -amount);
                return true;
            }
            case ITEM -> {
                Material mat = Keys.material(cost.getItemKey());
                if (mat == null) {
                    return true;
                }
                ItemStack stack = new ItemStack(mat, (int) Math.max(1, amount));
                if (!caster.getInventory().containsAtLeast(stack, stack.getAmount())) {
                    return false;
                }
                caster.getInventory().removeItem(stack);
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    // Kept for symmetry / future use by projectile impacts.
    Location worldOffset(Location base, Vector forward, com.signalvfx.model.Vec3 offset) {
        return Coords.localToWorld(base, forward, offset);
    }
}
