package cn.coatcn.bookhighlight.mixin;

import cn.coatcn.bookhighlight.ConfigManager;
import cn.coatcn.bookhighlight.EnchantMatch;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected ScreenHandler handler;

    private static boolean book_highlight$isSupportedContainer(ScreenHandler handler) {
        if (handler == null) return false;
        return handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler;
    }

    // 关键：method 指向 intermediary 名 + 描述符；remap=false
    @Inject(
        method = "method_2385(Lnet/minecraft/class_332;Lnet/minecraft/class_1735;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void book_highlight$afterDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!book_highlight$isSupportedContainer(handler)) return;

        Inventory inv = slot.inventory;
        if (inv == null || handler instanceof PlayerScreenHandler) return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty()) return;

        var targets = ConfigManager.getInstance().getVisibleNamesCn();
        if (EnchantMatch.shouldHighlightItem(stack, targets)) {
            int color = ConfigManager.getInstance().getHighlightColor(); // ARGB
            // context 在 drawSlot 之前已平移到容器原点，这里无需再次加 x/y
            int left = slot.x + 1;
            int top  = slot.y + 1;
            context.fill(left, top, left + 16, top + 16, color);
        }
    }
}
