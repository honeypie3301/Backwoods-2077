package net.mcreator.thebackwoods;

import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;

import java.nio.file.Path;
import java.util.Optional;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ClassicPackLoader {
    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path resourcePath = ModList.get().getModFileById("the_backwoods").getFile().findResource("classic_edition");
            
            // In 1.21.1, these classes are moved to net.minecraft.server.packs
            PackLocationInfo locationInfo = new PackLocationInfo(
                "the_backwoods_classic", 
                Component.literal("The Backwoods: Classic Reimagined"), 
                PackSource.BUILT_IN,
                Optional.empty()
            );

            PackSelectionConfig selectionConfig = new PackSelectionConfig(false, Pack.Position.TOP, false);

            Pack pack = Pack.readMetaAndCreate(
                locationInfo,
                new PathPackResources.PathResourcesSupplier(resourcePath),
                PackType.CLIENT_RESOURCES,
                selectionConfig
            );

            if (pack != null) {
                event.addRepositorySource((infoConsumer) -> infoConsumer.accept(pack));
            }
        }
    }
}