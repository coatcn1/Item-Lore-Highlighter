package cn.coatcn.bookhighlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

/**
 * 配置管理：
 * 1）配置文件路径：.minecraft/config/item_lore_highlighter/targets_cn.json
 * 2）内容包含：高亮颜色（ARGB 16 进制字符串）与中文关键字列表（targets）
 * 3）首次运行若不存在，则从 resources/item_lore_highlighter/targets_cn.json 拷贝默认文件
 */
public class ConfigManager {

    private static final String CONFIG_DIR_NAME = "item_lore_highlighter";
    private static final String LEGACY_CONFIG_DIR = "book_highlight";
    private static final String CONFIG_FILE_NAME = "targets_cn.json";
    private static final String INTERNAL_DEFAULT = "/item_lore_highlighter/targets_cn.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    private final List<TargetRule> targets = new ArrayList<>();
    private int highlightColor = 0x80FFD700; // 默认半透明金色
    private int openKey = GLFW.GLFW_KEY_B;
    private Path configPath;
    private long lastModified = 0L;

    private ConfigManager() {}

    public synchronized void loadOrInit() {
        try {
            Path configRoot = FabricLoader.getInstance().getConfigDir();
            Path cfgDir = configRoot.resolve(CONFIG_DIR_NAME);
            Path cfgFile = cfgDir.resolve(CONFIG_FILE_NAME);
            if (!Files.exists(cfgDir)) {
                Files.createDirectories(cfgDir);
            }
            if (!Files.exists(cfgFile)) {
                Path legacyFile = configRoot.resolve(LEGACY_CONFIG_DIR).resolve(CONFIG_FILE_NAME);
                if (Files.exists(legacyFile)) {
                    Files.copy(legacyFile, cfgFile);
                } else {
                    // 从 jar 内置默认文件拷贝一份到 config 目录，便于用户修改
                    try (InputStream in = getClass().getResourceAsStream(INTERNAL_DEFAULT)) {
                        if (in == null) {
                            throw new IOException("找不到内置默认配置：" + INTERNAL_DEFAULT);
                        }
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                             BufferedWriter bw = Files.newBufferedWriter(cfgFile, StandardCharsets.UTF_8)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                bw.write(line);
                                bw.newLine();
                            }
                        }
                    }
                }
            }
            this.configPath = cfgFile;
            // 读取配置
            try (BufferedReader reader = Files.newBufferedReader(cfgFile, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                this.highlightColor = parseColor(obj.get("highlightColor").getAsString(), 0x80FFD700);
                this.openKey = obj.has("openKey") ? obj.get("openKey").getAsInt() : GLFW.GLFW_KEY_B;
                loadTargetsFromJson(obj);
            }
            this.lastModified = Files.getLastModifiedTime(cfgFile).toMillis();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果外部读失败，退回到内置默认
            loadFromInternalDefault();
        }
    }

    private void loadFromInternalDefault() {
        this.targets.clear();
        this.highlightColor = 0x80FFD700;
        this.openKey = GLFW.GLFW_KEY_B;
        try (InputStream in = getClass().getResourceAsStream(INTERNAL_DEFAULT)) {
            if (in == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                JsonObject obj = GSON.fromJson(br, JsonObject.class);
                this.highlightColor = parseColor(obj.get("highlightColor").getAsString(), 0x80FFD700);
                this.openKey = obj.has("openKey") ? obj.get("openKey").getAsInt() : GLFW.GLFW_KEY_B;
                loadTargetsFromJson(obj);
            }
        } catch (IOException ignored) {}
    }

    public static int parseColor(String hex, int fallback) {
        try {
            String h = hex.trim().toLowerCase();
            if (h.startsWith("0x")) {
                h = h.substring(2);
            }
            if (h.startsWith("#")) {
                h = h.substring(1);
            }
            // 允许 ARGB 或 RGBA，不足补齐
            long v = Long.parseUnsignedLong(h, 16);
            if (h.length() <= 6) {
                // 仅 RGB，则补上 alpha 0x80
                v |= 0x80_00_00_00L;
            }
            return (int) v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public List<TargetRule> getTargets() {
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }
        List<TargetRule> copy = new ArrayList<>(targets.size());
        for (TargetRule target : targets) {
            copy.add(target.copy());
        }
        return copy;
    }

    public List<TargetRule> getVisibleTargets() {
        if (targets.isEmpty()) {
            return Collections.emptyList();
        }
        List<TargetRule> visible = new ArrayList<>();
        for (TargetRule target : targets) {
            if (target.isVisible()) {
                visible.add(target.copy());
            }
        }
        return visible;
    }

    public int getOpenKey() {
        return openKey;
    }

    public void setOpenKey(int key) {
        this.openKey = key;
        saveCurrentToConfig();
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(int color) {
        this.highlightColor = color;
        saveCurrentToConfig();
    }

    // 将当前配置写回并更新修改时间
    public synchronized void saveCurrentToConfig() {
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
            Path cfgFile = cfgDir.resolve(CONFIG_FILE_NAME);
            if (!Files.exists(cfgDir)) Files.createDirectories(cfgDir);

            JsonObject obj = new JsonObject();
            obj.addProperty("highlightColor", String.format("0x%08X", highlightColor));
            obj.addProperty("openKey", openKey);
            var arr = new com.google.gson.JsonArray();
            for (TargetRule target : targets) {
                var keywords = new com.google.gson.JsonArray();
                for (String keyword : target.getKeywords()) {
                    keywords.add(keyword);
                }
                if (keywords.size() == 0) {
                    continue;
                }
                JsonObject t = new JsonObject();
                t.add("keywords", keywords);
                t.addProperty("visible", target.isVisible());
                if (keywords.size() > 0) {
                    t.addProperty("name", target.getKeywords().get(0));
                }
                arr.add(t);
            }
            obj.add("targets", arr);

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(cfgFile), StandardCharsets.UTF_8))) {
                bw.write(GSON.toJson(obj));
            }
            this.configPath = cfgFile;
            this.lastModified = Files.getLastModifiedTime(cfgFile).toMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 检测文件是否被外部修改，若是则重新加载
    public void reloadIfChanged() {
        if (configPath == null) return;
        try {
            long lm = Files.getLastModifiedTime(configPath).toMillis();
            if (lm != lastModified) {
                loadOrInit();
            }
        } catch (IOException ignored) {}
    }

    // 更新关键字集合（来自界面），并立即保存
    public void updateTargets(List<TargetRule> newTargets) {
        this.targets.clear();
        if (newTargets != null) {
            for (TargetRule rule : newTargets) {
                if (rule == null) continue;
                List<String> keywords = rule.getKeywords();
                if (keywords == null || keywords.isEmpty()) continue;
                this.targets.add(new TargetRule(keywords, rule.isVisible()));
            }
        }
        saveCurrentToConfig();
    }

    private void loadTargetsFromJson(JsonObject obj) {
        this.targets.clear();
        if (obj == null) {
            return;
        }
        if (obj.has("targets") && obj.get("targets").isJsonArray()) {
            obj.getAsJsonArray("targets").forEach(element -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject targetObj = element.getAsJsonObject();
                boolean visible = !targetObj.has("visible") || targetObj.get("visible").getAsBoolean();
                List<String> keywords = extractKeywords(targetObj);
                if (!keywords.isEmpty()) {
                    this.targets.add(new TargetRule(keywords, visible));
                }
            });
        } else if (obj.has("names") && obj.get("names").isJsonArray()) {
            obj.getAsJsonArray("names").forEach(e -> {
                String s = e.getAsString();
                if (s != null && !s.isBlank()) {
                    this.targets.add(new TargetRule(List.of(s.trim()), true));
                }
            });
        }
    }

    private static List<String> extractKeywords(JsonObject targetObj) {
        List<String> keywords = new ArrayList<>();
        if (targetObj.has("keywords") && targetObj.get("keywords").isJsonArray()) {
            targetObj.getAsJsonArray("keywords").forEach(element -> {
                if (!element.isJsonPrimitive()) {
                    return;
                }
                String value = element.getAsString();
                if (value != null && !value.isBlank()) {
                    keywords.add(value.trim());
                }
            });
        }
        if (keywords.isEmpty() && targetObj.has("name")) {
            String name = targetObj.get("name").getAsString();
            if (name != null && !name.isBlank()) {
                keywords.add(name.trim());
            }
        }
        return keywords;
    }
}