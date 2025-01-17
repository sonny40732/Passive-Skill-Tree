package daripher.skilltree.mixin.minecraft;

import daripher.skilltree.api.PlayerContainer;
import daripher.skilltree.skill.bonus.player.CraftedItemBonus;
import daripher.skilltree.skill.bonus.SkillBonusHandler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin extends BaseContainerBlockEntity
    implements PlayerContainer {
  private @Nullable Player player;

  @SuppressWarnings("DataFlowIssue")
  protected BrewingStandBlockEntityMixin() {
    super(null, null, null);
  }

  @Inject(method = "doBrew", at = @At("TAIL"))
  private static void enhanceBrewedPotions(
      Level level,
      BlockPos blockPos,
      NonNullList<ItemStack> itemStacks,
      CallbackInfo callbackInfo) {
    BlockEntity blockEntity = level.getBlockEntity(blockPos);
    if (!(blockEntity instanceof PlayerContainer playerContainer)) return;
    Optional<Player> player = playerContainer.getViewingPlayer();
    if (player.isEmpty()) return;
    for (int slot = 0; slot < 3; slot++) {
      ItemStack potionStack = itemStacks.get(slot);
      SkillBonusHandler.getSkillBonuses(player.get(), CraftedItemBonus.class)
          .forEach(bonus -> bonus.itemCrafted(potionStack));
    }
  }

  @Override
  public AbstractContainerMenu createMenu(
      int window, @NotNull Inventory inventory, @NotNull Player player) {
    this.player = player;
    return super.createMenu(window, inventory, player);
  }

  @Override
  public Optional<Player> getViewingPlayer() {
    return Optional.ofNullable(player);
  }
}
