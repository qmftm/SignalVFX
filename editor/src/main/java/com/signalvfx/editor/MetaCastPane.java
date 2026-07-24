package com.signalvfx.editor;

import com.signalvfx.model.CastSettings;
import com.signalvfx.model.CastType;
import com.signalvfx.model.Cost;
import com.signalvfx.model.Skill;
import com.signalvfx.model.Targeting;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

/**
 * Editor panel for skill identity (id/name/description/icon) and cast settings
 * (trigger type, targeting, timing, cost).
 */
final class MetaCastPane extends ScrollPane {

    private final Runnable onChange;

    MetaCastPane(Runnable onChange) {
        this.onChange = onChange;
        setFitToWidth(true);
    }

    void bind(Skill skill) {
        GridPane grid = Fx.form();
        int r = 0;

        grid.add(section("Identity"), 0, r++, 2, 1);
        Fx.row(grid, r++, "ID", Fx.text(skill.getId(), v -> {
            skill.setId(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Name", Fx.text(skill.getName(), v -> {
            skill.setName(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Author", Fx.text(skill.getAuthor(), v -> {
            skill.setAuthor(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Icon item", Fx.text(skill.getIcon(), v -> {
            skill.setIcon(v);
            onChange.run();
        }));

        TextArea desc = new TextArea(skill.getDescription());
        desc.setPrefRowCount(3);
        desc.setWrapText(true);
        desc.textProperty().addListener((o, a, b) -> {
            skill.setDescription(b);
            onChange.run();
        });
        Fx.row(grid, r++, "Description", desc);

        CastSettings cast = skill.getCast();
        grid.add(section("Cast"), 0, r++, 2, 1);
        Fx.row(grid, r++, "Trigger", Fx.enumCombo(CastType.class, cast.getType(), v -> {
            cast.setType(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Targeting", Fx.enumCombo(Targeting.class, cast.getTargeting(), v -> {
            cast.setTargeting(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Cast time (ticks)", Fx.intField(cast.getCastTimeTicks(), v -> {
            cast.setCastTimeTicks(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Cooldown (ticks)", Fx.intField(cast.getCooldownTicks(), v -> {
            cast.setCooldownTicks(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Range (blocks)", Fx.doubleField(cast.getRange(), v -> {
            cast.setRange(v);
            onChange.run();
        }));

        Cost cost = cast.getCost();
        grid.add(section("Cost"), 0, r++, 2, 1);
        Fx.row(grid, r++, "Type", Fx.enumCombo(Cost.Type.class, cost.getType(), v -> {
            cost.setType(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Amount", Fx.doubleField(cost.getAmount(), v -> {
            cost.setAmount(v);
            onChange.run();
        }));
        Fx.row(grid, r++, "Item (if ITEM)", Fx.text(cost.getItemKey(), v -> {
            cost.setItemKey(v);
            onChange.run();
        }));

        setContent(grid);
    }

    private Label section(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 2 0; -fx-font-size: 13px;");
        return l;
    }
}
