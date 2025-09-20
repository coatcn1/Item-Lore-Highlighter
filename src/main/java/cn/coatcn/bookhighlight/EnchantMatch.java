package cn.coatcn.bookhighlight;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.text.Text.Serializer;

import java.util.ArrayList;
import java.util.List;

/**
 * 匹配逻辑：
 * 1）不再限制物品类型，任意物品都可参与判断；
 * 2）读取物品的 display.NBT（自定义名称、Lore 等），判断是否包含配置的中文关键字；
 * 3）若名称或描述文本同时包含某条规则的全部关键字，则需要高亮。
 */
public class EnchantMatch {

    private EnchantMatch() {}

    /**
     * 判断该物品栈是否需要被高亮显示。
     */
    public static boolean shouldHighlightItem(ItemStack stack, List<TargetRule> targetRules) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (targetRules == null || targetRules.isEmpty()) {
            return false;
        }

        List<String> texts = collectTexts(stack);
        if (texts.isEmpty()) {
            return false;
        }

        for (TargetRule rule : targetRules) {
            if (rule == null) continue;
            List<String> keywords = rule.getKeywords();
            if (keywords == null || keywords.isEmpty()) continue;
            if (matchesAllKeywords(texts, keywords)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> collectTexts(ItemStack stack) {
        List<String> texts = new ArrayList<>();
        String name = stack.getName().getString();
        if (name != null && !name.isBlank()) {
            texts.add(name);
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("display", NbtElement.COMPOUND_TYPE)) {
            NbtCompound display = nbt.getCompound("display");
            if (display.contains("Name", NbtElement.STRING_TYPE)) {
                try {
                    Text customName = Serializer.fromJson(display.getString("Name"));
                    if (customName != null) {
                        String s = customName.getString();
                        if (s != null && !s.isBlank()) {
                            texts.add(s);
                        }
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
                        if (line != null) {
                            String s = line.getString();
                            if (s != null && !s.isBlank()) {
                                texts.add(s);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return texts;
    }

    private static boolean matchesAllKeywords(List<String> texts, List<String> keywords) {
        if (keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                return false;
            }
            if (!containsKeyword(texts, keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsKeyword(List<String> texts, String keyword) {
        for (String text : texts) {
            if (text != null && !text.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}