package me.vibing.restful;

import me.vibing.restful.command.BedCommands;
import me.vibing.restful.network.RestfulNetwork;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;

// restful mod - multiple backup respawn points for minecraft
// uses neoforge data attachments for persistence
@Mod(Restful.MODID)
public class Restful {
    public static final String MODID = "restful";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = 
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    
    public static final AttachmentType<BedTracker> BED_TRACKER = AttachmentType
            .builder(BedTracker::new)
            .serialize(new BedTrackerSerializer())
            .copyOnDeath()
            .build();

    public Restful(IEventBus modEventBus, ModContainer modContainer) {
        ATTACHMENT_TYPES.register("bed_tracker", () -> BED_TRACKER);
        ATTACHMENT_TYPES.register(modEventBus);
        
        modEventBus.addListener(RestfulNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        
        // PlayerEvent.Clone needs the game bus, not the mod bus
        NeoForge.EVENT_BUS.register(new BedEventHandler());
        
        // MAX_POINTS is server-side only - each server can have different limits
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
    
    private void registerCommands(RegisterCommandsEvent event) {
        BedCommands.register(event.getDispatcher());
    }
    
    private static class BedTrackerSerializer implements IAttachmentSerializer<CompoundTag, BedTracker> {
        @Override
        public BedTracker read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            BedTracker tracker = new BedTracker();
            tracker.fromTag(tag);
            return tracker;
        }
        
        @Nullable
        @Override
        public CompoundTag write(BedTracker tracker, HolderLookup.Provider provider) {
            return tracker.toTag();
        }
    }
}
