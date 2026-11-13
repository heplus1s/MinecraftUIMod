package dev.jdguo.bank.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional bank screen. Pages sit on a bottom row. There is no side panel.
 * The lines above the page are M0, M1, M2, the exchange rate, and the normal redemption status.
 */
public final class BankTerminalScreen extends Screen {
    private enum Page {
        SAVINGS, DEPOSIT, WITHDRAW, EXCHANGE, CHECKS, MARKET, ISSUANCE, MANAGEMENT, CONFIRM
    }

    private JsonObject data;
    private Page page = Page.SAVINGS;
    private int step;
    private String anchorId = "";
    private String regime = "";
    private String otherId = "";
    private String pickedLabel = "";
    private ChoiceListWidget list;
    private TextFieldWidget amount;
    private TextFieldWidget nameField;
    private boolean closing;
    private int headerBottom = 72;

    public BankTerminalScreen(JsonObject data) {
        super(Text.literal("中央银行"));
        this.data = data;
    }

    void show(JsonObject data) {
        boolean confirm = data.has("confirm") && data.get("confirm").getAsBoolean();
        this.data = data;
        if (confirm) {
            page = Page.CONFIRM;
            step = 0;
        } else if (page == Page.CONFIRM) {
            page = Page.ISSUANCE;
            step = 0;
        }
        clearAndInit();
    }

    @Override
    protected void init() {
        list = null;
        amount = null;
        nameField = null;
        headerBottom = 20 + BankHeader.lines(data).size() * 12 + 16;
        if (page == Page.CONFIRM) {
            addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> {
                BankUiClient.send("note-bankrupt-no");
                page = Page.ISSUANCE;
                step = 0;
                clearAndInit();
            }).dimensions(this.width / 2 - 130, this.height - 28, 80, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("确认宣告破产"), button -> BankUiClient.send("note-bankrupt-yes"))
                    .dimensions(this.width / 2 - 40, this.height - 28, 160, 20).build());
            return;
        }
        addTabs();
        if (step > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> back())
                    .dimensions(this.width / 2 - 160, this.height - 48, 60, 18).build());
        }
        if (page == Page.ISSUANCE && data.has("personal") && step == 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("宣告破产"), button -> {
                BankUiClient.send("note-bankrupt");
                page = Page.CONFIRM;
                clearAndInit();
            }).dimensions(this.width / 2 + 70, this.height - 48, 90, 18).build());
        }
        if (!formStep()) {
            int listWidth = Math.min(260, Math.max(140, this.width - 24));
            int listHeight = Math.max(54, this.height - headerBottom - 78);
            list = new ChoiceListWidget(client, (this.width - listWidth) / 2, headerBottom, listWidth, listHeight);
            list.setChoices(choices());
            addSelectableChild(list);
            return;
        }
        int fieldX = this.width / 2 - 60;
        int fieldY = headerBottom + 16;
        amount = field(fieldX, fieldY, 120, "1");
        addDrawableChild(amount);
        if (page == Page.DEPOSIT || page == Page.WITHDRAW) {
            addQuick(fieldX - 16, fieldY + 24);
        }
        if (page == Page.ISSUANCE && !data.has("personal")) {
            nameField = field(fieldX, fieldY + 24, 120, "");
            addDrawableChild(nameField);
            addDrawableChild(ButtonWidget.builder(Text.literal("记名"), button -> submit())
                    .dimensions(fieldX, fieldY + 48, 120, 18).build());
            return;
        }
        int verbY = page == Page.DEPOSIT || page == Page.WITHDRAW ? fieldY + 48 : fieldY + 24;
        addDrawableChild(ButtonWidget.builder(Text.literal(verb()), button -> submit())
                .dimensions(fieldX, verbY, 120, 18).build());
    }

    private void addTabs() {
        List<String> tabs = new ArrayList<>();
        for (String tab : BankChrome.TABS) {
            if ("管理".equals(tab) && !admin()) {
                continue;
            }
            tabs.add(tab);
        }
        tabs.add("关闭");
        int buttonWidth = 88;
        int gap = 4;
        int perRow = Math.max(1, (this.width - 8) / (buttonWidth + gap));
        int rows = (tabs.size() + perRow - 1) / perRow;
        int y = this.height - 8 - rows * 22;
        for (int i = 0; i < tabs.size(); i++) {
            int column = i % perRow;
            int row = i / perRow;
            int rowCount = Math.min(perRow, tabs.size() - row * perRow);
            int rowWidth = rowCount * buttonWidth + (rowCount - 1) * gap;
            int x = (this.width - rowWidth) / 2 + column * (buttonWidth + gap);
            String tab = tabs.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal(tab), button -> onTab(tab))
                    .dimensions(x, y + row * 22, buttonWidth, 18).build());
        }
    }

    private void addQuick(int x, int y) {
        for (String label : new String[] {"1", "5", "10", "64"}) {
            String shown = label;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> quick(shown))
                    .dimensions(x, y, 28, 18).build());
            x += 30;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("全部"), button -> quick("全部"))
                .dimensions(x, y, 48, 18).build());
    }

    private void onTab(String tab) {
        if ("关闭".equals(tab)) {
            close();
            return;
        }
        page = switch (tab) {
            case "存入" -> Page.DEPOSIT;
            case "取出" -> Page.WITHDRAW;
            case "兑换" -> Page.EXCHANGE;
            case "汇票" -> Page.CHECKS;
            case "交易" -> Page.MARKET;
            case "个人发行" -> Page.ISSUANCE;
            case "管理" -> Page.MANAGEMENT;
            default -> Page.SAVINGS;
        };
        step = 0;
        anchorId = "";
        regime = "";
        otherId = "";
        pickedLabel = "";
        clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int x = 16;
        context.drawText(textRenderer, "中央银行", x, 8, 0xFFD0B060, false);
        int y = 20;
        String status = text("status");
        for (String line : BankHeader.lines(data)) {
            int color = 0xFFE6E6E6;
            if (line.startsWith("汇率")) {
                color = 0xFFE8D090;
            } else if (line.equals(status)) {
                color = statusColor(status);
            }
            context.drawText(textRenderer, line, x, y, color, false);
            y += 12;
        }
        if (page == Page.CONFIRM) {
            context.drawText(textRenderer, "确认后才会作废「" + text("personal") + "」。返回则账目不变。", x, y + 8, 0xFFFF7777, false);
            return;
        }
        context.drawText(textRenderer, pageTitle(), x, y, 0xFFD0B060, false);
        if (list != null) {
            list.render(context, mouseX, mouseY, delta);
        }
        if (formStep()) {
            String caption = pickedLabel.isBlank() ? "数量" : pickedLabel;
            context.drawText(textRenderer, caption, this.width / 2 - 60, headerBottom + 4, 0xFFE8D090, false);
            if (page == Page.ISSUANCE && !data.has("personal")) {
                context.drawText(textRenderer, "名字", this.width / 2 - 60, headerBottom + 28, 0xFFAAAAAA, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && list.isMouseOver(mouseX, mouseY) && list.mouseClicked(mouseX, mouseY, button)) {
            if (mouseX < list.getRowRight() - 8 && page != Page.SAVINGS && !formStep()) {
                ChoiceListWidget.Choice picked = selected();
                if (picked != null) {
                    choose(picked);
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void choose(ChoiceListWidget.Choice picked) {
        switch (page) {
            case DEPOSIT, WITHDRAW, CHECKS -> {
                anchorId = picked.id();
                pickedLabel = picked.label();
                regime = picked.id();
                step = 1;
            }
            case EXCHANGE -> {
                if (step == 0) {
                    anchorId = picked.id();
                    step = 1;
                } else {
                    regime = picked.id();
                    pickedLabel = picked.label();
                    step = 2;
                }
            }
            case MARKET -> {
                if (step == 0) {
                    anchorId = picked.id();
                    step = 1;
                } else if (step == 1) {
                    regime = picked.id();
                    pickedLabel = picked.label();
                    step = needsOther() ? 2 : 3;
                } else {
                    otherId = picked.id();
                    step = 3;
                }
            }
            case ISSUANCE -> {
                if (!data.has("personal")) {
                    if (step == 0) {
                        regime = picked.id();
                        pickedLabel = picked.label();
                        step = 1;
                    } else {
                        anchorId = picked.id();
                        step = 2;
                    }
                } else {
                    regime = picked.id();
                    pickedLabel = picked.label();
                    step = 1;
                }
            }
            case MANAGEMENT -> {
                regime = picked.id();
                pickedLabel = picked.label();
                if ("freeze".equals(regime)) {
                    BankUiClient.send("admin-freeze");
                    return;
                }
                step = 1;
            }
            default -> {
                return;
            }
        }
        clearAndInit();
    }

    private boolean formStep() {
        return switch (page) {
            case DEPOSIT, WITHDRAW, CHECKS -> step >= 1;
            case EXCHANGE -> step >= 2;
            case MARKET -> step >= 3;
            case ISSUANCE -> data.has("personal") ? step >= 1 : step >= 2;
            case MANAGEMENT -> step >= 1;
            default -> false;
        };
    }

    private boolean needsOther() {
        return "swap".equals(regime) || "redeem".equals(regime);
    }

    private List<ChoiceListWidget.Choice> choices() {
        if (page == Page.MARKET && step == 1) {
            return List.of(
                    new ChoiceListWidget.Choice("buy", "买入"),
                    new ChoiceListWidget.Choice("sell", "卖出"),
                    new ChoiceListWidget.Choice("swap", "互换"),
                    new ChoiceListWidget.Choice("redeem", "通兑"));
        }
        if (page == Page.MARKET && step == 2) {
            return "swap".equals(regime) ? notes() : currencies();
        }
        if (page == Page.MARKET && step == 0) {
            return notes();
        }
        if (page == Page.ISSUANCE && !data.has("personal") && step == 0) {
            return List.of(
                    new ChoiceListWidget.Choice("float", "浮动"),
                    new ChoiceListWidget.Choice("peg", "钉住"),
                    new ChoiceListWidget.Choice("hard", "定额兑付"));
        }
        if (page == Page.ISSUANCE && !data.has("personal")) {
            return List.of(
                    new ChoiceListWidget.Choice("diamond", "钻石"),
                    new ChoiceListWidget.Choice("copper", "铜锭"),
                    new ChoiceListWidget.Choice("note", "银行券"));
        }
        if (page == Page.ISSUANCE) {
            return List.of(
                    new ChoiceListWidget.Choice("issue", "按准备金发行"),
                    new ChoiceListWidget.Choice("unbacked", "无准备金发行"),
                    new ChoiceListWidget.Choice("reserve-deposit", "存入央行准备金"),
                    new ChoiceListWidget.Choice("reserve-withdraw", "取出央行准备金"));
        }
        if (page == Page.MANAGEMENT) {
            return List.of(
                    new ChoiceListWidget.Choice("freeze", "暂停或恢复兑付"),
                    new ChoiceListWidget.Choice("unbacked", "无准备金增发"),
                    new ChoiceListWidget.Choice("retire", "注销"));
        }
        if (page == Page.SAVINGS) {
            List<ChoiceListWidget.Choice> rows = currencies();
            rows.addAll(heldNotes());
            return rows;
        }
        return currencies();
    }

    private List<ChoiceListWidget.Choice> currencies() {
        List<ChoiceListWidget.Choice> rows = new ArrayList<>();
        if (!data.has("currencies")) {
            return rows;
        }
        JsonArray array = data.getAsJsonArray("currencies");
        for (int i = 0; i < array.size(); i++) {
            JsonObject row = array.get(i).getAsJsonObject();
            rows.add(new ChoiceListWidget.Choice(row.get("id").getAsString(),
                    row.get("name").getAsString() + "  " + BankHeader.fixed6(row.get("balance").getAsString())));
        }
        return rows;
    }

    private List<ChoiceListWidget.Choice> notes() {
        List<ChoiceListWidget.Choice> rows = new ArrayList<>();
        if (!data.has("notes")) {
            return rows;
        }
        JsonArray array = data.getAsJsonArray("notes");
        for (int i = 0; i < array.size(); i++) {
            JsonObject row = array.get(i).getAsJsonObject();
            rows.add(new ChoiceListWidget.Choice(row.get("name").getAsString(),
                    row.get("name").getAsString() + "  " + BankHeader.fixed6(row.get("balance").getAsString())));
        }
        return rows;
    }

    private List<ChoiceListWidget.Choice> heldNotes() {
        List<ChoiceListWidget.Choice> rows = new ArrayList<>();
        if (!data.has("notes")) {
            return rows;
        }
        JsonArray array = data.getAsJsonArray("notes");
        for (int i = 0; i < array.size(); i++) {
            JsonObject row = array.get(i).getAsJsonObject();
            String balance = row.get("balance").getAsString();
            if ("0".equals(balance)) {
                continue;
            }
            rows.add(new ChoiceListWidget.Choice(row.get("name").getAsString(),
                    "个人银行券 " + row.get("name").getAsString() + "  " + BankHeader.fixed6(balance)));
        }
        return rows;
    }

    private ChoiceListWidget.Choice selected() {
        if (list == null || list.selectedId() == null) {
            return null;
        }
        return new ChoiceListWidget.Choice(list.selectedId(), list.selectedLabel());
    }

    private void quick(String shown) {
        if (amount != null) {
            amount.setText(shown);
        }
        submit();
    }

    private void submit() {
        String qty = QuantityTokens.token(amount == null ? "1" : amount.getText());
        switch (page) {
            case DEPOSIT -> BankUiClient.send("deposit-transfer " + anchorId + " " + qty);
            case WITHDRAW -> BankUiClient.send("withdraw " + anchorId + " " + qty);
            case EXCHANGE -> BankUiClient.send("exchange " + anchorId + " " + regime + " " + qty);
            case CHECKS -> BankUiClient.send("check " + anchorId + " " + qty + " bearer");
            case MARKET -> {
                if ("swap".equals(regime)) {
                    if (!otherId.isBlank()) {
                        BankUiClient.send("note-swap " + anchorId + " " + otherId + " " + qty);
                    }
                } else if ("redeem".equals(regime)) {
                    if (!otherId.isBlank()) {
                        BankUiClient.send("note-redeem " + anchorId + " " + otherId + " " + qty);
                    }
                } else if ("sell".equals(regime)) {
                    BankUiClient.send("note-sell " + anchorId + " " + qty);
                } else {
                    BankUiClient.send("note-buy " + anchorId + " " + qty);
                }
            }
            case ISSUANCE -> {
                if (!data.has("personal")) {
                    String name = nameField == null ? "" : nameField.getText().trim();
                    BankUiClient.send("note-create " + regime + " " + anchorId + " " + name + " " + qty);
                } else if ("unbacked".equals(regime)) {
                    BankUiClient.send("note-unbacked " + qty);
                } else if ("reserve-deposit".equals(regime)) {
                    BankUiClient.send("reserve-deposit " + qty);
                } else if ("reserve-withdraw".equals(regime)) {
                    BankUiClient.send("reserve-withdraw " + qty);
                } else {
                    BankUiClient.send("note-issue " + qty);
                }
            }
            case MANAGEMENT -> {
                if ("retire".equals(regime)) {
                    BankUiClient.send("admin-retire note " + qty);
                } else {
                    BankUiClient.send("admin-unbacked note " + qty);
                }
            }
            default -> {
            }
        }
    }

    private String verb() {
        return switch (page) {
            case DEPOSIT -> "划拨存入";
            case WITHDRAW -> "划拨取出";
            case EXCHANGE -> "兑换";
            case CHECKS -> "开出汇票";
            case MARKET -> switch (regime) {
                case "sell" -> "卖出";
                case "swap" -> "互换";
                case "redeem" -> "通兑";
                default -> "买入";
            };
            case ISSUANCE -> switch (regime) {
                case "unbacked" -> "无准备金发行";
                case "reserve-deposit" -> "划入准备金";
                case "reserve-withdraw" -> "划出准备金";
                default -> "发行";
            };
            case MANAGEMENT -> "retire".equals(regime) ? "注销" : "无准备金增发";
            default -> "执行";
        };
    }

    private String pageTitle() {
        return switch (page) {
            case SAVINGS -> "储蓄 · 储蓄总池 " + BankHeader.fixed6(text("savings"));
            case DEPOSIT -> "划拨存入";
            case WITHDRAW -> "划拨取出";
            case EXCHANGE -> step == 0 ? "兑换 · 付出" : "兑换 · 得到";
            case CHECKS -> "汇票";
            case MARKET -> switch (step) {
                case 0 -> "交易 · 选择银行券";
                case 1 -> "交易 · 方式";
                case 2 -> "swap".equals(regime) ? "交易 · 换成" : "交易 · 兑成";
                default -> "交易";
            };
            case ISSUANCE -> {
                if (data.has("personal")) {
                    yield "个人发行 · " + text("personal");
                }
                yield step == 0 ? "个人发行 · 方式" : "个人发行 · 抵押";
            }
            case MANAGEMENT -> "管理";
            case CONFIRM -> "确认宣告破产";
        };
    }

    private void back() {
        if (step > 0) {
            step--;
            if (page == Page.MARKET && step == 2 && !needsOther()) {
                step = 1;
            }
            clearAndInit();
        }
    }

    private boolean admin() {
        return data.has("admin") && data.get("admin").getAsBoolean();
    }

    private String text(String key) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsString() : "";
    }

    private int statusColor(String status) {
        if (status.contains("无法") || status.contains("暂停")) {
            return 0xFFFF7777;
        }
        if (status.contains("不足") || status.contains("紧张")) {
            return 0xFFE8D090;
        }
        return 0xFF77DD77;
    }

    private TextFieldWidget field(int x, int y, int width, String value) {
        TextFieldWidget widget = new TextFieldWidget(textRenderer, x, y, width, 16, Text.literal(""));
        widget.setText(value);
        widget.setMaxLength(64);
        return widget;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            BankUiClient.send("close");
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
