package cn.coatcn.bookhighlight;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 设置界面：
 * - 支持为每条规则添加多个关键字（全部匹配时命中）
 * - 可新增/删除规则，并单独控制显示开关
 * - 列表支持滚动浏览
 */
public class BookHighlightConfigScreen extends Screen {

    private static final int TOP_PADDING = 40;
    private static final int BOTTOM_PADDING = 80;
    private static final int ROW_HEIGHT = 24;
    private static final int ENTRY_SPACING = 6;
    private static final int KEYWORD_WIDTH = 160;
    private static final int SUB_INDENT = 16;
    private static final int BUTTON_SIZE = 20;
    private static final int TOGGLE_WIDTH = 60;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_START_GAP = 8;

    private final List<Entry> entries = new ArrayList<>();
    private ButtonWidget addButton;
    private ButtonWidget doneButton;
    private double scrollOffset;
    private int contentHeight;

    public BookHighlightConfigScreen() {
        super(Text.literal("高亮关键字配置"));
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        entries.clear();
        scrollOffset = 0;

        ConfigManager.getInstance().reloadIfChanged();

        List<TargetRule> stored = ConfigManager.getInstance().getTargets();
        if (stored.isEmpty()) {
            addEntry(List.of(""), true);
        } else {
            for (TargetRule rule : stored) {
                addEntry(rule.getKeywords(), rule.isVisible());
            }
        }

        addButton = ButtonWidget.builder(Text.literal("新增规则"), btn -> addEntry(List.of(""), true))
                .dimensions(0, 0, 90, 20)
                .build();
        addDrawableChild(addButton);

        doneButton = ButtonWidget.builder(Text.literal("完成"), btn -> saveAndClose())
                .dimensions(0, 0, 90, 20)
                .build();
        addDrawableChild(doneButton);

        layoutFooterButtons();
        updateLayout();
    }

    @Override
    public void tick() {
        for (Entry entry : entries) {
            for (KeywordRow row : entry.rows) {
                row.field.tick();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int areaHeight = Math.max(0, height - TOP_PADDING - BOTTOM_PADDING);
        if (contentHeight <= areaHeight || areaHeight <= 0) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }
        double maxScroll = contentHeight - areaHeight;
        scrollOffset = MathHelper.clamp(scrollOffset - amount * 20, 0, maxScroll);
        updateLayout();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (addButton != null && addButton.isMouseOver(mouseX, mouseY)) {
            if (addButton.mouseClicked(mouseX, mouseY, button)) {
                setFocused(addButton);
                return true;
            }
        }
        if (doneButton != null && doneButton.isMouseOver(mouseX, mouseY)) {
            if (doneButton.mouseClicked(mouseX, mouseY, button)) {
                setFocused(doneButton);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
        drawScrollbar(context);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void addEntry(List<String> keywords, boolean visible) {
        Entry entry = new Entry(keywords, visible);
        entries.add(entry);
        updateLayout();
    }

    private void removeEntry(Entry entry) {
        for (KeywordRow row : entry.rows) {
            remove(row.field);
            if (row.removeButton != null) {
                remove(row.removeButton);
            }
        }
        remove(entry.addKeywordButton);
        remove(entry.toggleButton);
        remove(entry.removeEntryButton);
        entries.remove(entry);
        updateLayout();
    }

    private void addKeywordRow(Entry entry, String value, boolean primary) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, KEYWORD_WIDTH, 20, Text.literal("keyword"));
        field.setMaxLength(64);
        field.setText(value == null ? "" : value);
        addDrawableChild(field);

        ButtonWidget removeButton = null;
        if (!primary) {
            removeButton = ButtonWidget.builder(Text.literal("-"), btn -> removeKeywordRow(entry, field))
                    .dimensions(0, 0, BUTTON_SIZE, 20)
                    .build();
            addDrawableChild(removeButton);
        }

        entry.rows.add(new KeywordRow(field, removeButton));
    }

    private void removeKeywordRow(Entry entry, TextFieldWidget field) {
        Iterator<KeywordRow> iterator = entry.rows.iterator();
        while (iterator.hasNext()) {
            KeywordRow row = iterator.next();
            if (row.field == field) {
                remove(row.field);
                if (row.removeButton != null) {
                    remove(row.removeButton);
                }
                iterator.remove();
                break;
            }
        }
        if (entry.rows.isEmpty()) {
            addKeywordRow(entry, "", true);
        }
        updateLayout();
    }

    private void updateLayout() {
        recalcContentHeight();
        clampScroll();
        int totalWidth = KEYWORD_WIDTH + BUTTON_START_GAP + BUTTON_SIZE + BUTTON_GAP + TOGGLE_WIDTH + BUTTON_GAP + BUTTON_SIZE;
        int fieldX = width / 2 - totalWidth / 2;
        int buttonStartX = fieldX + KEYWORD_WIDTH + BUTTON_START_GAP;
        int y = TOP_PADDING - (int) Math.round(scrollOffset);

        for (Entry entry : entries) {
            if (entry.rows.isEmpty()) {
                addKeywordRow(entry, "", true);
            }
            KeywordRow mainRow = entry.rows.get(0);
            configureField(mainRow.field, fieldX, y, KEYWORD_WIDTH);

            entry.addKeywordButton.setX(buttonStartX);
            entry.addKeywordButton.setY(y);
            entry.toggleButton.setX(buttonStartX + BUTTON_SIZE + BUTTON_GAP);
            entry.toggleButton.setY(y);
            entry.removeEntryButton.setX(buttonStartX + BUTTON_SIZE + BUTTON_GAP + TOGGLE_WIDTH + BUTTON_GAP);
            entry.removeEntryButton.setY(y);

            y += ROW_HEIGHT;

            for (int i = 1; i < entry.rows.size(); i++) {
                KeywordRow row = entry.rows.get(i);
                configureField(row.field, fieldX + SUB_INDENT, y, KEYWORD_WIDTH - SUB_INDENT);
                if (row.removeButton != null) {
                    row.removeButton.setX(buttonStartX);
                    row.removeButton.setY(y);
                }
                y += ROW_HEIGHT;
            }

            y += ENTRY_SPACING;
        }

        layoutFooterButtons();
    }

    private void recalcContentHeight() {
        int total = 0;
        for (Entry entry : entries) {
            int rows = Math.max(1, entry.rows.size());
            total += rows * ROW_HEIGHT;
            total += ENTRY_SPACING;
        }
        if (total > 0) {
            total -= ENTRY_SPACING;
        }
        contentHeight = total;
    }

    private void clampScroll() {
        int areaHeight = Math.max(0, height - TOP_PADDING - BOTTOM_PADDING);
        if (areaHeight <= 0) {
            scrollOffset = 0;
            return;
        }
        double maxScroll = Math.max(0, contentHeight - areaHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    private void layoutFooterButtons() {
        int footerY = height - 45;
        if (addButton != null) {
            addButton.setX(width / 2 - 120);
            addButton.setY(footerY);
        }
        if (doneButton != null) {
            doneButton.setX(width / 2 + 30);
            doneButton.setY(footerY);
        }
    }

    private void configureField(TextFieldWidget field, int x, int y, int width) {
        field.setX(x);
        field.setY(y);
        field.setWidth(width);
    }

    private void drawScrollbar(DrawContext context) {
        int areaHeight = Math.max(0, height - TOP_PADDING - BOTTOM_PADDING);
        if (contentHeight <= areaHeight || areaHeight <= 0) {
            return;
        }
        int trackX = width - 12;
        int trackWidth = 6;
        int trackY = TOP_PADDING;
        context.fill(trackX, trackY, trackX + trackWidth, trackY + areaHeight, 0x33000000);

        int knobHeight = Math.max(20, (int) ((float) areaHeight * areaHeight / contentHeight));
        int maxScroll = contentHeight - areaHeight;
        int knobY = trackY + (int) ((areaHeight - knobHeight) * (scrollOffset / maxScroll));
        context.fill(trackX, knobY, trackX + trackWidth, knobY + knobHeight, 0x99FFFFFF);
    }

    private void saveAndClose() {
        List<TargetRule> rules = new ArrayList<>();
        for (Entry entry : entries) {
            List<String> keywords = new ArrayList<>();
            for (KeywordRow row : entry.rows) {
                String text = row.field.getText().trim();
                if (!text.isEmpty()) {
                    keywords.add(text);
                }
            }
            if (!keywords.isEmpty()) {
                rules.add(new TargetRule(keywords, entry.visible));
            }
        }
        ConfigManager.getInstance().updateTargets(rules);
        MinecraftClient.getInstance().setScreen(null);
    }

    private Text getToggleText(boolean visible) {
        return Text.literal(visible ? "显示" : "隐藏")
                .formatted(visible ? Formatting.GREEN : Formatting.RED);
    }

    private final class Entry {
        final List<KeywordRow> rows = new ArrayList<>();
        boolean visible;
        final ButtonWidget addKeywordButton;
        final ButtonWidget toggleButton;
        final ButtonWidget removeEntryButton;

        Entry(List<String> keywords, boolean visible) {
            this.visible = visible;
            this.addKeywordButton = ButtonWidget.builder(Text.literal("+"), btn -> {
                        addKeywordRow(this, "", false);
                        updateLayout();
                    })
                    .dimensions(0, 0, BUTTON_SIZE, 20)
                    .build();
            this.toggleButton = ButtonWidget.builder(getToggleText(this.visible), btn -> {
                        this.visible = !this.visible;
                        btn.setMessage(getToggleText(this.visible));
                    })
                    .dimensions(0, 0, TOGGLE_WIDTH, 20)
                    .build();
            this.removeEntryButton = ButtonWidget.builder(Text.literal("X"), btn -> removeEntry(this))
                    .dimensions(0, 0, BUTTON_SIZE, 20)
                    .build();

            addDrawableChild(addKeywordButton);
            addDrawableChild(toggleButton);
            addDrawableChild(removeEntryButton);

            List<String> initial = keywords == null || keywords.isEmpty() ? List.of("") : keywords;
            boolean first = true;
            for (String keyword : initial) {
                addKeywordRow(this, keyword, first);
                first = false;
            }
        }
    }

    private record KeywordRow(TextFieldWidget field, ButtonWidget removeButton) {
    }
}
