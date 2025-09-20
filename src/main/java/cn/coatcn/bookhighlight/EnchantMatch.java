package cn.coatcn.bookhighlight;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serializer;

import java.util.Set;

/**
 * 匹配逻辑：
 * 1）不再限制物品类型，任意物品都可参与判断；
 * 2）读取物品的 display.NBT（自定义名称、Lore 等），判断是否包含配置的中文关键字；
 * 3）若名称或任意描述行命中关键字，则需要高亮。
 */
public class EnchantMatch {

    private EnchantMatch() {}

    /**
     * 判断该物品栈是否需要被高亮显示。
     */
    public static boolean shouldHighlightItem(ItemStack stack, Set<String> targetKeywords) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (targetKeywords == null || targetKeywords.isEmpty()) {
            return false;
        }

        // 先检查物品名称（包括自定义名称）
        if (containsKeyword(stack.getName().getString(), targetKeywords)) {
            return true;
        }

        // 读取 Lore 描述行
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = nbt.getCompound("display");
            if (display.contains("Name", NbtElement.STRING_TYPE)) {
                try {
                    Text name = Serializer.fromJson(display.getString("Name"));
                    if (name != null && containsKeyword(name.getString(), targetKeywords)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            if (display.contains("Lore", NbtElement.LIST_TYPE)) {
                NbtList lore = display.getList("Lore", NbtElement.STRING_TYPE);
                for (int i = 0; i < lore.size(); i++) {
                    String json = lore.getString(i);
                    try {
                        Text line = Serializer.fromJson(json);
                        if (line != null && containsKeyword(line.getString(), targetKeywords)) {
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return false;
    }

    private static boolean containsKeyword(String text, Set<String> targetKeywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : targetKeywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}