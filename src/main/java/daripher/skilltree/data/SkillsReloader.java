package daripher.skilltree.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;

import daripher.skilltree.SkillTreeMod;
import daripher.skilltree.data.serializers.AttributeSerializer;
import daripher.skilltree.data.serializers.PairSerializer;
import daripher.skilltree.data.serializers.SupplierSerializer;
import daripher.skilltree.skill.PassiveSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = SkillTreeMod.MOD_ID)
public class SkillsReloader extends SimpleJsonResourceReloadListener {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<ResourceLocation, PassiveSkill> SKILLS = new HashMap<>();
	public static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
			.registerTypeAdapter(Attribute.class, new AttributeSerializer())
			.registerTypeAdapter(new TypeToken<Supplier<String>>() {}.getType(), new SupplierSerializer<>(String.class))
			.registerTypeAdapter(new TypeToken<Pair<Attribute, AttributeModifier>>() {}.getType(),
					new PairSerializer<>("attribute", "modifier", Attribute.class, AttributeModifier.class))
			.create();

	public SkillsReloader() {
		super(GSON, "skills");
	}

	@SubscribeEvent
	public static void reloadSkills(AddReloadListenerEvent event) {
		event.addListener(new SkillsReloader());
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
			ProfilerFiller profilerFiller) {
		SKILLS.clear();
		map.forEach(this::readSkill);
	}

	protected void readSkill(ResourceLocation id, JsonElement json) {
		try {
			PassiveSkill skill = GSON.fromJson(json, PassiveSkill.class);
			SKILLS.put(skill.getId(), skill);
		} catch (Exception exception) {
			LOGGER.error("Couldn't load passive skill {}", id);
			exception.printStackTrace();
		}
	}

	public static Map<ResourceLocation, PassiveSkill> getSkills() {
		return SKILLS;
	}

	public static @Nullable PassiveSkill getSkillById(ResourceLocation id) {
		return SKILLS.get(id);
	}
}