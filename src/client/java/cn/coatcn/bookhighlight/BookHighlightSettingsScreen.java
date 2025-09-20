package cn.coatcn.bookhighlight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;

/**
 * Mod Menu 设置界面：允许修改打开配置界面的快捷键以及高亮颜色。
 */
public class BookHighlightSettingsScreen extends Screen {

    private final Screen parent;
    private ButtonWidget keyButton;
    private TextFieldWidget colorField;
    private boolean listening = false;

    public BookHighlightSettingsScreen(Screen parent) {
        super(Text.literal("物品高亮设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int y = height / 3;

        keyButton = ButtonWidget.builder(getKeyText(ConfigManager.getInstance().getOpenKey()), btn -> {
            listening = true;
            btn.setMessage(Text.literal("..."));
        }).dimensions(centerX - 100, y, 200, 20).build();
        addDrawableChild(keyButton);

        colorField = new TextFieldWidget(textRenderer, centerX - 100, y + 30, 200, 20, Text.literal("color"));
        colorField.setMaxLength(16);
        colorField.setText(String.format("0x%08X", ConfigManager.getInstance().getHighlightColor()));
        addDrawableChild(colorField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> saveAndClose())
                .dimensions(centerX - 100, y + 60, 200, 20)
                .build());
    }

    private Text getKeyText(int keyCode) {
        return Text.literal("快捷键: " + InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString());
    }

    private void saveAndClose() {
        int color = ConfigManager.parseColor(colorField.getText(), ConfigManager.getInstance().getHighlightColor());
        ConfigManager.getInstance().setHighlightColor(color);
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            ConfigManager.getInstance().setOpenKey(keyCode);
            BookHighlightMod.updateKeyBinding(keyCode);
            keyButton.setMessage(getKeyText(keyCode));
            listening = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 3 - 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
