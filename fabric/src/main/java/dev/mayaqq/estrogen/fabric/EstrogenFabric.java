package dev.mayaqq.estrogen.fabric;

import com.simibubi.create.foundation.config.ConfigBase;
import dev.mayaqq.estrogen.Estrogen;
import dev.mayaqq.estrogen.config.EstrogenConfig;
import dev.mayaqq.estrogen.registry.common.EstrogenEffects;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingEntityDamageEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Map;

public class EstrogenFabric implements ModInitializer {
    @Override
    public void onInitialize() {

        Estrogen.init();
        Estrogen.REGISTRATE.register();
        EstrogenFabricEvents.register();
        LivingEntityDamageEvents.HURT.register((event) -> {
            if (event.damaged instanceof Player player) {
                if (event.damageSource.is(DamageTypes.FALL) && player.hasEffect(EstrogenEffects.ESTROGEN_EFFECT)) {
                    event.damageAmount /= 1.5f;
                }
            }
        });

        EstrogenConfig.register();
        for (Map.Entry<ModConfig.Type, ConfigBase> pair : EstrogenConfig.CONFIGS.entrySet())
            ForgeConfigRegistry.INSTANCE.register(Estrogen.MOD_ID, pair.getKey(), pair.getValue().specification);

        ModConfigEvents.loading(Estrogen.MOD_ID).register(EstrogenConfig::onLoad);
        ModConfigEvents.reloading(Estrogen.MOD_ID).register(EstrogenConfig::onReload);
    }
}