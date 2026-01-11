package io.github.itskillerluc.familiarfaces.client.config;

import io.github.itskillerluc.familiarfaces.server.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FamiliarFacesConfigScreen {
    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.familiar_faces.config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory common = builder.getOrCreateCategory(Component.translatable("category.familiar_faces.common"));

        common.addEntry(entryBuilder.startIntField(Component.translatable("option.familiar_faces.brushing_cooldown"), Config.Common.brushingCooldown.get())
                .setDefaultValue(5)
                .setTooltip(Component.translatable("tooltip.familiar_faces.brushing_cooldown"))
                .setSaveConsumer(newValue -> Config.Common.brushingCooldown.set(newValue))
                .build());

        return builder.build();
    }
}