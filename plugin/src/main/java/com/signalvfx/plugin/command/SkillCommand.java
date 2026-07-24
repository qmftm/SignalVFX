package com.signalvfx.plugin.command;

import com.signalvfx.model.Skill;
import com.signalvfx.plugin.SignalVfxPlugin;
import com.signalvfx.plugin.SkillRegistry;
import com.signalvfx.plugin.cast.CastService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /svfx reload | list | cast <skill> [player]}. */
public final class SkillCommand implements CommandExecutor, TabCompleter {

    private final SignalVfxPlugin plugin;
    private final SkillRegistry registry;
    private final CastService castService;

    public SkillCommand(SignalVfxPlugin plugin, SkillRegistry registry, CastService castService) {
        this.plugin = plugin;
        this.registry = registry;
        this.castService = castService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§b/svfx §7reload | list | cast <skill> [player]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                registry.loadAll();
                sender.sendMessage("§aReloaded §f" + registry.count() + "§a skill(s).");
            }
            case "list" -> {
                if (registry.count() == 0) {
                    sender.sendMessage("§7No skills loaded.");
                } else {
                    sender.sendMessage("§bSkills: §f" + String.join(", ",
                            registry.all().stream().map(Skill::getId).toList()));
                }
            }
            case "cast" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /svfx cast <skill> [player]");
                    return true;
                }
                Skill skill = registry.get(args[1]);
                if (skill == null) {
                    sender.sendMessage("§cUnknown skill: " + args[1]);
                    return true;
                }
                Player caster = resolveCaster(sender, args);
                if (caster == null) {
                    sender.sendMessage("§cNo target player (run in-game or pass a player name).");
                    return true;
                }
                castService.cast(caster, skill);
                sender.sendMessage("§aCast §f" + skill.getId() + "§a as §f" + caster.getName());
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }

    private Player resolveCaster(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            return Bukkit.getPlayerExact(args[2]);
        }
        return sender instanceof Player p ? p : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("reload", "list", "cast")) {
                if (s.startsWith(args[0].toLowerCase())) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cast")) {
            for (Skill skill : registry.all()) {
                if (skill.getId().startsWith(args[1].toLowerCase())) {
                    out.add(skill.getId());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("cast")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
