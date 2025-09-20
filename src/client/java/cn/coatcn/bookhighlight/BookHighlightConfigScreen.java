package cn.coatcn.bookhighlight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 简易设置界面：
 * - 显示并可编辑当前选中的中文关键字
 * - 可新增/删除条目
 */
public class BookHighlightConfigScreen extends Screen {

    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final List<ButtonWidget> removeButtons = new ArrayList<>();
    private final List<ButtonWidget> toggleButtons = new ArrayList<>();
    private final List<Boolean> visibleFlags = new ArrayList<>();
    private ButtonWidget addButton;
    private ButtonWidget doneButton;

    public BookHighlightConfigScreen() {
        super(Text.literal("高亮关键字配置"));
    }

    @Override
    protected void init() {
        super.init();
        ConfigManager.getInstance().reloadIfChanged();
        fields.clear();
        removeButtons.clear();
        toggleButtons.clear();
        visibleFlags.clear();

        int y = 40;
        for (var entry : ConfigManager.getInstance().getTargetMap().entrySet()) {
            addEntry(entry.getKey(), entry.getValue(), y);
            y += 24;
        }

        addButton = ButtonWidget.builder(Text.literal("Add"), btn -> addEntry("", true, 40 + fields.size() * 24))
                .dimensions(width / 2 - 100, height - 60, 60, 20)
                .build();
        addDrawableChild(addButton);

        doneButton = ButtonWidget.builder(Text.literal("Done"), btn -> saveAndClose())
                .dimensions(width / 2 - 40, height - 30, 80, 20)
                .build();
        addDrawableChild(doneButton);
    }

    private void addEntry(String text, boolean visible, int y) {
        int fieldX = width / 2 - 100;
        TextFieldWidget field = new TextFieldWidget(textRenderer, fieldX, y, 120, 20, Text.literal("name"));
        field.setMaxLength(64);
        field.setText(text);
        fields.add(field);
        visibleFlags.add(visible);
        addDrawableChild(field);

        ButtonWidget toggle = ButtonWidget.builder(getToggleText(visible), btn -> {
            int idx = toggleButtons.indexOf(btn);
            boolean nv = !visibleFlags.get(idx);
            visibleFlags.set(idx, nv);
            btn.setMessage(getToggleText(nv));
        }).dimensions(fieldX + 125, y, 20, 20).build();
        toggleButtons.add(toggle);
        addDrawableChild(toggle);

        ButtonWidget remove = ButtonWidget.builder(Text.literal("X"), btn -> {
            int idx = fields.indexOf(field);
            if (idx >= 0) {
                remove(field);
                remove(removeButtons.get(idx));
                remove(toggleButtons.get(idx));
                fields.remove(idx);
                removeButtons.remove(idx);
                toggleButtons.remove(idx);
                visibleFlags.remove(idx);
                relayoutEntries();
            }
        }).dimensions(fieldX + 150, y, 20, 20).build();
        removeButtons.add(remove);
        addDrawableChild(remove);
    }

    private void relayoutEntries() {
        for (int i = 0; i < fields.size(); i++) {
            int y = 40 + i * 24;
            fields.get(i).setY(y);
            toggleButtons.get(i).setY(y);
            removeButtons.get(i).setY(y);
        }
    }

    private void saveAndClose() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            String s = fields.get(i).getText().trim();
            if (!s.isEmpty()) {
                map.put(s, visibleFlags.get(i));
            }
        }
        ConfigManager.getInstance().updateTargets(map);
        MinecraftClient.getInstance().setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Text getToggleText(boolean visible) {
        return Text.literal(visible ? "显示" : "隐藏")
                .formatted(visible ? Formatting.GREEN : Formatting.RED);
    }
}