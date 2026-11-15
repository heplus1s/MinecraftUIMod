package dev.jdguo.shop.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional shop screen. The currency drawer never offers copper or diamonds.
 * The catalog scrolls inside a frame.
 */
public final class ShopScreen extends Screen {
    private static final int CELL = 72;

    private final JsonObject data;
    private final Map<String, String> quantities = new HashMap<>();
    private final List<QuantityBox> quantityFields = new ArrayList<>();
    private ShopDrawer drawer;
    private boolean currencyOpen;
    private String currencyId = ShopQuotes.NOTE;
    private String category = "";
    private int scroll;
    private int frameX;
    private int frameY;
    private int frameW;
    private int frameH;
    private String notice = "";

    public ShopScreen(JsonObject data) {
        super(Text.literal("商店"));
        this.data = data == null ? new JsonObject() : data;
    }

    void show(JsonObject next) {
        captureQuantities();
        client.setScreen(new ShopScreen(next == null ? new JsonObject() : next, currencyId, currencyOpen, category, scroll, quantities));
    }

    ShopScreen(JsonObject data, String currencyId, boolean currencyOpen, String category, int scroll, Map<String, String> quantities) {
        super(Text.literal("商店"));
        this.data = data == null ? new JsonObject() : data;
        this.currencyId = currencyId == null ? ShopQuotes.NOTE : currencyId;
        this.currencyOpen = currencyOpen;
        this.category = category == null ? "" : category;
        this.scroll = Math.max(0, scroll);
        this.quantities.putAll(quantities);
    }

    @Override
    protected void init() {
        quantityFields.clear();
        List<ShopQuotes.Pay> pays = ShopQuotes.payCurrencies(data);
        if (pays.stream().noneMatch(pay -> pay.id().equals(currencyId))) {
            currencyId = ShopQuotes.NOTE;
        }
        int drawerWidth = ShopLayout.currencyWidth(this.width);
        int drawerHeight = ShopLayout.currencyHeight(currencyOpen, pays.size());
        drawer = new ShopDrawer(client, 8, 8, drawerWidth, drawerHeight);
        drawer.setExpanded(currencyOpen);
        drawer.setChoices(pays.stream().map(pay -> new ShopDrawer.Choice(pay.id(), pay.label())).toList(), currencyId);
        addSelectableChild(drawer);

        addDrawableChild(ButtonWidget.builder(Text.literal("关闭"), button -> close())
                .dimensions(this.width - 68, this.height - 28, 60, 18).build());

        List<String> tabs = tabs();
        if (tabs.stream().noneMatch(tab -> tab.equals(category))) {
            category = tabs.isEmpty() ? "" : tabs.get(0);
        }
        int perRow = Math.max(1, ShopLayout.frameW(this.width) / 72);
        int tabRows = tabs.isEmpty() ? 1 : (tabs.size() + perRow - 1) / perRow;
        frameX = ShopLayout.frameX(this.width);
        frameY = ShopLayout.frameY(tabRows);
        frameW = ShopLayout.frameW(this.width);
        frameH = ShopLayout.frameH(this.height, tabRows);
        int tabX = frameX;
        int tabY = 8;
        int shown = 0;
        for (String tab : tabs) {
            if (shown > 0 && shown % perRow == 0) {
                tabX = frameX;
                tabY += 22;
            }
            String name = tab;
            int width = Math.min(88, Math.max(36, textRenderer.getWidth(name) + 12));
            addDrawableChild(ButtonWidget.builder(Text.literal(name), button -> chooseTab(name))
                    .dimensions(tabX, tabY, width, 18).build());
            tabX += width + 4;
            shown++;
        }
        List<ShopQuotes.Item> items = visibleItems();
        int columns = ShopQuotes.columns(frameW);
        int content = ((items.size() + columns - 1) / columns) * CELL;
        scroll = Math.min(scroll, Math.max(0, content - frameH));
        int cellW = Math.max(40, (frameW - 8) / columns);
        for (int index = 0; index < items.size(); index++) {
            int col = index % columns;
            int row = index / columns;
            int cellX = frameX + 4 + col * cellW;
            int cellY = frameY + 4 + row * CELL - scroll;
            if (cellY < frameY || cellY + CELL > frameY + frameH || cellX + 122 > frameX + frameW - 4) {
                continue;
            }
            ShopQuotes.Item item = items.get(index);
            String current = quantities.getOrDefault(item.id(), "1");
            TextFieldWidget field = new TextFieldWidget(textRenderer, cellX, cellY + 48, 42, 16, Text.literal("数量"));
            field.setMaxLength(8);
            field.setText(current == null || current.isBlank() ? "1" : current);
            addDrawableChild(field);
            quantityFields.add(new QuantityBox(item.id(), field));
            String itemId = item.id();
            addDrawableChild(ButtonWidget.builder(Text.literal("买入"), button -> trade(true, itemId, field))
                    .dimensions(cellX + 46, cellY + 48, 36, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("卖出"), button -> trade(false, itemId, field))
                    .dimensions(cellX + 86, cellY + 48, 36, 18).build());
        }
    }

    private void trade(boolean buy, String itemId, TextFieldWidget field) {
        String quantity = field.getText() == null ? "" : field.getText().trim();
        quantities.put(itemId, quantity);
        if (quantity.isEmpty() || !quantity.chars().allMatch(Character::isDigit) || "0".equals(quantity)) {
            notice = "请填写购买或出售的数量。";
            return;
        }
        notice = "";
        int count;
        try {
            count = Integer.parseInt(quantity);
        } catch (NumberFormatException exception) {
            notice = "请填写购买或出售的数量。";
            return;
        }
        boolean explicitCurrency = "true".equals(ShopQuotes.text(data, "explicitCurrency"));
        if (!explicitCurrency && !ShopQuotes.serverCurrency(data, currencyId)) {
            notice = "服务器需要升级商店模组，才能使用所选币种。";
            return;
        }
        ShopUiClient.sendTrade(buy, itemId, count, explicitCurrency ? currencyId : null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawText(textRenderer, "商店", 8, this.height - 42, 0xFFD0B060, false);
        drawCatalog(context);
        super.render(context, mouseX, mouseY, delta);
        if (drawer != null) {
            drawer.render(context, mouseX, mouseY, delta);
        }
        frame(context, frameX, frameY, frameX + frameW, frameY + frameH);
        if (!notice.isBlank()) {
            context.drawText(textRenderer, notice, 8, this.height - 44, 0xFFFFCC88, false);
        }
    }

    private void chooseTab(String name) {
        captureQuantities();
        category = name;
        scroll = 0;
        client.execute(this::clearAndInit);
    }

    private List<String> tabs() {
        return ShopQuotes.categories(tagged(ShopQuotes.items(data)));
    }

    private List<ShopQuotes.Item> visibleItems() {
        List<ShopQuotes.Item> tagged = tagged(ShopQuotes.items(data));
        if (category == null || category.isBlank()) {
            return tagged;
        }
        return ShopQuotes.inCategory(tagged, category);
    }

    private List<ShopQuotes.Item> tagged(List<ShopQuotes.Item> items) {
        List<ShopQuotes.Item> rows = new ArrayList<>();
        for (ShopQuotes.Item item : items) {
            rows.add(new ShopQuotes.Item(item.id(), item.name(), item.diamondPrice(), item.notes(), categoryOf(item)));
        }
        return rows;
    }

    private String categoryOf(ShopQuotes.Item item) {
        if (item.category() != null && !item.category().isBlank()) {
            return item.category();
        }
        ItemStack stack = icon(item.id());
        if (stack.isEmpty()) {
            return "其他";
        }
        var vanilla = stack.getItem();
        if (vanilla.getFoodComponent() != null) {
            return "食物与饮品";
        }
        if (vanilla instanceof SwordItem || vanilla instanceof BowItem || vanilla instanceof CrossbowItem
                || vanilla instanceof ArmorItem || vanilla instanceof TridentItem || vanilla instanceof ShieldItem) {
            return "战斗";
        }
        if (vanilla instanceof MiningToolItem || vanilla instanceof FishingRodItem || vanilla instanceof ShearsItem
                || vanilla instanceof FlintAndSteelItem) {
            return "工具与实用物品";
        }
        String path = Registries.ITEM.getId(vanilla).getPath();
        if (path.contains("redstone") || path.contains("piston") || path.contains("observer")
                || path.contains("repeater") || path.contains("comparator") || path.contains("hopper")
                || path.contains("dispenser") || path.contains("dropper") || path.contains("lever")
                || path.contains("button") || path.contains("pressure_plate") || path.contains("rail")) {
            return "红石";
        }
        if (vanilla instanceof BlockItem) {
            if (path.contains("wool") || path.contains("terracotta") || path.contains("concrete")
                    || path.contains("carpet") || path.contains("stained") || path.contains("glass")) {
                return "染色方块";
            }
            if (path.contains("log") || path.contains("wood") || path.contains("leaves") || path.contains("ore")
                    || path.contains("dirt") || path.contains("sand") || path.contains("gravel")
                    || path.contains("stone") && !path.contains("redstone")) {
                return "自然方块";
            }
            if (path.contains("chest") || path.contains("furnace") || path.contains("crafting")
                    || path.contains("bed") || path.contains("barrel") || path.contains("table")) {
                return "功能方块";
            }
            return "建筑方块";
        }
        return "原材料";
    }

    private void drawCatalog(DrawContext context) {
        List<ShopQuotes.Item> items = visibleItems();
        int columns = ShopQuotes.columns(frameW);
        int cellW = Math.max(40, (frameW - 8) / columns);
        context.enableScissor(frameX + 1, frameY + 1, frameX + frameW - 1, frameY + frameH - 1);
        if (ShopQuotes.items(data).isEmpty()) {
            context.drawText(textRenderer, "柜台还没有商品。", frameX + 8, frameY + 8, 0xFFAAAAAA, false);
        } else if (items.isEmpty()) {
            context.drawText(textRenderer, "这一类还没有商品。", frameX + 8, frameY + 8, 0xFFAAAAAA, false);
        }
        for (int index = 0; index < items.size(); index++) {
            int col = index % columns;
            int row = index / columns;
            int cellX = frameX + 4 + col * cellW;
            int cellY = frameY + 4 + row * CELL - scroll;
            if (cellY + CELL < frameY || cellY > frameY + frameH) {
                continue;
            }
            ShopQuotes.Item item = items.get(index);
            context.drawItem(icon(item.id()), cellX, cellY + 2);
            context.drawText(textRenderer, item.name(), cellX + 20, cellY + 6, 0xFFE8E8E8, false);
            String price = ShopQuotes.due(data, item, currencyId);
            int color = ShopQuotes.UNAVAILABLE.equals(price) ? 0xFFAAAAAA : 0xFFE8D090;
            String quantity = quantityOf(item.id());
            String total = ShopQuotes.total(price, quantity);
            context.drawText(textRenderer, "单价：" + price, cellX, cellY + 20, color, false);
            context.drawText(textRenderer, "合计：" + total, cellX, cellY + 32, color, false);
        }
        context.disableScissor();
    }

    private String quantityOf(String itemId) {
        for (QuantityBox box : quantityFields) {
            if (itemId.equals(box.itemId())) {
                String text = box.field().getText();
                return text == null || text.isBlank() ? "1" : text.trim();
            }
        }
        return quantities.getOrDefault(itemId, "1");
    }

    private static ItemStack icon(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(Registries.ITEM.get(identifier));
    }

    private static void frame(DrawContext context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, top + 1, 0xFFC8A060);
        context.fill(left, bottom - 1, right, bottom, 0xFFC8A060);
        context.fill(left, top, left + 1, bottom, 0xFFC8A060);
        context.fill(right - 1, top, right, bottom, 0xFFC8A060);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (drawer != null && drawer.isMouseOver(mouseX, mouseY) && drawer.mouseClicked(mouseX, mouseY, button)) {
            if (drawer.pollToggle()) {
                currencyOpen = !currencyOpen;
                client.execute(this::clearAndInit);
                return true;
            }
            if (drawer.pollPick()) {
                currencyId = drawer.selectedId();
                currencyOpen = false;
                client.execute(this::clearAndInit);
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (drawer != null && drawer.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        if (mouseX >= frameX && mouseX < frameX + frameW && mouseY >= frameY && mouseY < frameY + frameH) {
            captureQuantities();
            int columns = ShopQuotes.columns(frameW);
            int rows = (visibleItems().size() + columns - 1) / columns;
            int max = Math.max(0, rows * CELL - frameH);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.round(amount * CELL)));
            client.execute(this::clearAndInit);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private void captureQuantities() {
        for (QuantityBox box : quantityFields) {
            quantities.put(box.itemId, box.field.getText());
        }
    }

    private record QuantityBox(String itemId, TextFieldWidget field) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
