package daripher.skilltree.item;

import com.google.common.collect.ImmutableList;
import daripher.skilltree.api.HasAdditionalSockets;
import daripher.skilltree.compat.apotheosis.ApotheosisCompatibility;
import daripher.skilltree.config.Config;
import daripher.skilltree.init.PSTRegistries;
import daripher.skilltree.init.PSTTags;
import daripher.skilltree.skill.bonus.item.ItemBonus;
import daripher.skilltree.skill.bonus.item.ItemSocketsBonus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemHelper {
  private static final String POISONS = "Poisons";

  public static boolean canInsertGem(ItemStack stack) {
    if (ModList.get().isLoaded("apotheosis")) {
      if (ApotheosisCompatibility.INSTANCE.adventureModuleEnabled()) return false;
    }
    return hasSockets(stack);
  }

  public static boolean hasSockets(ItemStack stack) {
    List<? extends String> blacklist = Config.socket_blacklist;
    if (blacklist.contains("*:*")) return false;
    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
    if (itemId == null) return false;
    if (blacklist.contains(itemId.toString())) return false;
    String namespace = itemId.getNamespace();
    if (blacklist.contains(namespace + ":*")) return false;
    return isEquipment(stack) || isJewelry(stack);
  }

  public static void setPoisons(ItemStack result, ItemStack poisonStack) {
    List<MobEffectInstance> effects = PotionUtils.getMobEffects(poisonStack);
    ListTag poisonsTag = new ListTag();
    for (MobEffectInstance effect : effects) {
      CompoundTag effectTag = effect.save(new CompoundTag());
      poisonsTag.add(effectTag);
    }
    result.getOrCreateTag().put(POISONS, poisonsTag);
  }

  public static boolean hasPoisons(ItemStack stack) {
    return stack.hasTag() && stack.getTag().contains(POISONS);
  }

  public static List<MobEffectInstance> getPoisons(ItemStack stack) {
    ListTag poisonsTag = stack.getTag().getList(POISONS, 10);
    ArrayList<MobEffectInstance> effects = new ArrayList<>();
    for (Tag tag : poisonsTag) {
      MobEffectInstance effect = MobEffectInstance.load((CompoundTag) tag);
      effects.add(effect);
    }
    return effects;
  }

  public static EquipmentSlot getSlotForItem(ItemStack stack) {
    EquipmentSlot slot = Player.getEquipmentSlotForItem(stack);
    if (ItemHelper.isMeleeWeapon(stack) && slot == EquipmentSlot.OFFHAND) {
      slot = EquipmentSlot.MAINHAND;
    }
    return slot;
  }

  public static int getDefaultSockets(ItemStack stack) {
    if (isHelmet(stack)) return Config.default_helmet_sockets;
    if (isChestplate(stack)) return Config.default_chestplate_sockets;
    if (isLeggings(stack)) return Config.default_leggings_sockets;
    if (isBoots(stack)) return Config.default_boots_sockets;
    if (isWeapon(stack)) return Config.default_weapon_sockets;
    if (isShield(stack)) return Config.default_shield_sockets;
    if (isRing(stack)) return Config.default_ring_sockets;
    if (isNecklace(stack)) return Config.default_necklace_sockets;
    return 0;
  }

  public static int getAdditionalSockets(ItemStack stack) {
    int sockets =
        getItemBonuses(stack, ItemSocketsBonus.class).stream()
            .map(ItemSocketsBonus::sockets)
            .reduce(Integer::sum)
            .orElse(0);
    if (stack.getItem() instanceof HasAdditionalSockets bonus) {
      sockets += bonus.getAdditionalSockets();
    }
    return sockets;
  }

  public static boolean isArmor(ItemStack stack) {
    return isHelmet(stack)
        || isChestplate(stack)
        || isLeggings(stack)
        || isBoots(stack)
        || stack.is(Tags.Items.ARMORS);
  }

  public static boolean isShield(ItemStack stack) {
    if (stack.canPerformAction(ToolActions.SHIELD_BLOCK)) return true;
    if (Config.forced_shields.contains(stack.getItem())) return true;
    return stack.getItem() instanceof ShieldItem || stack.is(Tags.Items.TOOLS_SHIELDS);
  }

  public static boolean isMeleeWeapon(ItemStack stack) {
    if (Config.forced_melee_weapon.contains(stack.getItem())) return true;
    return isSword(stack) || isAxe(stack) || isTrident(stack);
  }

  public static boolean isRangedWeapon(ItemStack stack) {
    if (Config.forced_ranged_weapon.contains(stack.getItem())) return true;
    return isCrossbow(stack) || isBow(stack);
  }

  public static boolean isCrossbow(ItemStack stack) {
    if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("tetra:modular_crossbow"))
      return true;
    return stack.getItem() instanceof CrossbowItem || stack.is(Tags.Items.TOOLS_CROSSBOWS);
  }

  public static boolean isBow(ItemStack stack) {
    if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("tetra:modular_bow"))
      return true;
    return stack.getItem() instanceof BowItem || stack.is(Tags.Items.TOOLS_BOWS);
  }

  public static boolean isTrident(ItemStack stack) {
    if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("tetra:modular_single"))
      return true;
    return stack.getItem() instanceof TridentItem || stack.is(Tags.Items.TOOLS_TRIDENTS);
  }

  public static boolean isAxe(ItemStack stack) {
    if (stack.canPerformAction(ToolActions.AXE_DIG)) return true;
    return stack.getItem() instanceof AxeItem || stack.is(Tags.Items.TOOLS_AXES);
  }

  public static boolean isSword(ItemStack stack) {
    if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals("tetra:modular_sword"))
      return true;
    if (stack.canPerformAction(ToolActions.SWORD_DIG)) return true;
    if (stack.canPerformAction(ToolActions.SWORD_SWEEP)) return true;
    return stack.getItem() instanceof SwordItem || stack.is(Tags.Items.TOOLS_SWORDS);
  }

  public static boolean isWeapon(ItemStack stack) {
    return isMeleeWeapon(stack) || isRangedWeapon(stack);
  }

  public static boolean isHelmet(ItemStack stack) {
    if (Config.forced_helmets.contains(stack.getItem())) return true;
    if (stack.getItem() instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.HEAD)
      return true;
    return stack.is(Tags.Items.ARMORS_HELMETS);
  }

  public static boolean isChestplate(ItemStack stack) {
    if (Config.forced_chestplates.contains(stack.getItem())) return true;
    if (stack.getItem() instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.CHEST)
      return true;
    return stack.is(Tags.Items.ARMORS_CHESTPLATES);
  }

  public static boolean isLeggings(ItemStack stack) {
    if (Config.forced_leggings.contains(stack.getItem())) return true;
    if (stack.getItem() instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.LEGS)
      return true;
    return stack.is(Tags.Items.ARMORS_LEGGINGS);
  }

  public static boolean isBoots(ItemStack stack) {
    if (Config.forced_boots.contains(stack.getItem())) return true;
    if (stack.getItem() instanceof ArmorItem armor && armor.getSlot() == EquipmentSlot.FEET)
      return true;
    return stack.is(Tags.Items.ARMORS_BOOTS);
  }

  public static boolean isPickaxe(ItemStack stack) {
    if (stack.canPerformAction(ToolActions.PICKAXE_DIG)) return true;
    return stack.getItem() instanceof PickaxeItem || stack.is(Tags.Items.TOOLS_PICKAXES);
  }

  public static boolean isFood(ItemStack stack) {
    return stack.getFoodProperties(null) != null;
  }

  public static boolean isEquipment(ItemStack stack) {
    return isWeapon(stack) || isArmor(stack) || isShield(stack) || isTool(stack);
  }

  private static boolean isTool(ItemStack stack) {
    return stack.getItem() instanceof DiggerItem;
  }

  public static boolean isJewelry(ItemStack stack) {
    return isRing(stack) || isNecklace(stack);
  }

  public static boolean isRing(ItemStack stack) {
    return !stack.isEmpty() && stack.is(PSTTags.RINGS);
  }

  public static boolean isNecklace(ItemStack stack) {
    return !stack.isEmpty() && stack.is(PSTTags.NECKLACES);
  }

  public static boolean isQuiver(ItemStack stack) {
    return !stack.isEmpty() && stack.is(PSTTags.QUIVERS);
  }

  public static boolean isArrow(ItemStack stack) {
    return stack.is(ItemTags.ARROWS);
  }

  public static List<String> getBonuses() {
    return List.of(POISONS);
  }

  public static List<ItemBonus<?>> getItemBonuses(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    if (tag == null) return ImmutableList.of();
    ListTag bonusesTag = tag.getList("SkillBonuses", Tag.TAG_COMPOUND);
    return bonusesTag.stream()
        .map(CompoundTag.class::cast)
        .map(ItemHelper::deserializeBonus)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static <T extends ItemBonus<?>> List<T> getItemBonuses(ItemStack stack, Class<T> aClass) {
    return getItemBonuses(stack).stream()
        .filter(aClass::isInstance)
        .map(aClass::cast)
        .collect(Collectors.toList());
  }

  public static void addItemBonus(ItemStack stack, ItemBonus<?> bonus) {
    ListTag bonusesTag = new ListTag();
    bonusesTag.add(serializeBonus(bonus));
    ItemHelper.getItemBonuses(stack).stream()
        .map(bonus2 -> mergeIfPossible(bonus, bonus2, bonusesTag))
        .map(ItemHelper::serializeBonus)
        .forEach(bonusesTag::add);
    stack.getOrCreateTag().put("SkillBonuses", bonusesTag);
  }

  public static void removeItemBonus(ItemStack stack, ItemBonus<?> bonus) {
    ListTag bonusesTag = new ListTag();
    ItemHelper.getItemBonuses(stack).stream()
        .filter(Predicate.not(bonus::equals))
        .map(ItemHelper::serializeBonus)
        .forEach(bonusesTag::add);
    stack.getOrCreateTag().put("SkillBonuses", bonusesTag);
  }

  private static ItemBonus<? extends ItemBonus<?>> mergeIfPossible(
      ItemBonus<?> bonus1, ItemBonus<?> bonus2, ListTag bonusesTag) {
    if (bonus1.canMerge(bonus2)) {
      bonusesTag.remove(0);
      return bonus1.merge(bonus2);
    }
    return bonus2;
  }

  private static CompoundTag serializeBonus(ItemBonus<? extends ItemBonus<?>> bonus) {
    ItemBonus.Serializer serializer = bonus.getSerializer();
    CompoundTag bonusTag = serializer.serialize(bonus.copy());
    ResourceLocation id = PSTRegistries.ITEM_BONUSES.get().getKey(serializer);
    bonusTag.putString("type", Objects.requireNonNull(id).toString());
    return bonusTag;
  }

  private static ItemBonus<?> deserializeBonus(CompoundTag tag) {
    if (!tag.contains("type")) return null;
    ResourceLocation id = new ResourceLocation(tag.getString("type"));
    ItemBonus.Serializer serializer = PSTRegistries.ITEM_BONUSES.get().getValue(id);
    if (serializer == null) return null;
    return serializer.deserialize(tag);
  }
}
