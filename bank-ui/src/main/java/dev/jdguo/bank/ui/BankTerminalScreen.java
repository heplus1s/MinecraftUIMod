package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional bank screen. Rates stay in the left column. Choices are lists.
 * Main controls are centered in the space to the right of that column.
 */
public final class BankTerminalScreen extends Screen {
    private enum Page {
        SAVINGS, DEPOSIT, WITHDRAW, EXCHANGE, CHECKS, MARKET, ISSUANCE, MANAGEMENT, CONFIRM
    }

    private record Placed(String role, ChoiceListWidget widget, int x, int y, int width, int height) {
    }

    private JsonObject data;
    private Page page = Page.SAVINGS;
    private String payId = "";
    private String receiveId = "";
    private String noteName = "";
    private String otherNote = "";
    private String marketMode = "buy";
    private String checkView = "issue";
    private boolean payeeSelf;
    private String amountText = "1";
    private String notice = "";
    private boolean closing;
    private boolean redeemButtonReady;
    private TextFieldWidget amount;
    private final List<Placed> placed = new ArrayList<>();

    public BankTerminalScreen(JsonObject data) {
        super(Text.literal(BankChrome.SAVINGS_TITLE));
        this.data = data == null ? new JsonObject() : data;
    }

    void show(JsonObject next) {
        captureSelections();
        boolean confirm = BankSnapshot.confirm(next);
        this.data = next == null ? new JsonObject() : next;
        if (confirm) {
            page = Page.CONFIRM;
        } else if (page == Page.CONFIRM) {
            page = Page.ISSUANCE;
        }
        clearAndInit();
    }

    @Override
    protected void init() {
        amount = null;
        redeemButtonReady = false;
        placed.clear();
        boolean admin = BankSnapshot.admin(data);
        if (page == Page.CONFIRM) {
            addTopButtons(admin);
            addClose();
            initConfirm();
            return;
        }
        addChrome(admin);
        switch (page) {
            case DEPOSIT -> initMoney(BankSnapshot.currencies(data), true, "确认存入");
            case WITHDRAW -> initMoney(BankSnapshot.currencies(data), true, "确认取出");
            case EXCHANGE -> initExchange();
            case CHECKS -> initChecks();
            case MARKET -> initMarket();
            case ISSUANCE -> initIssuance();
            case MANAGEMENT -> initManagement();
            default -> {
            }
        }
    }

    private void addChrome(boolean admin) {
        addTopButtons(admin);
        addRow(BankChrome.ROW_ONE, BankLayout.row1Y(this.height));
        addRow(BankChrome.ROW_TWO, BankLayout.row2Y(this.height));
        addClose();
    }

    private void addTopButtons(boolean admin) {
        int issueWidth = buttonWidth(BankChrome.ISSUANCE);
        BankLayout.Box issue = BankLayout.topRight(this.width, BankLayout.EDGE, BankChrome.ISSUANCE, issueWidth);
        addDrawableChild(ButtonWidget.builder(Text.literal(issue.label()), button -> open(Page.ISSUANCE))
                .dimensions(issue.x(), issue.y(), issue.width(), issue.height()).build());
        if (!admin) {
            return;
        }
        int manageWidth = buttonWidth(BankChrome.MANAGEMENT);
        int y = BankLayout.EDGE + BankLayout.BUTTON_HEIGHT + BankLayout.TOP_GAP;
        BankLayout.Box manage = BankLayout.topRight(this.width, y, BankChrome.MANAGEMENT, manageWidth);
        addDrawableChild(ButtonWidget.builder(Text.literal(manage.label()), button -> open(Page.MANAGEMENT))
                .dimensions(manage.x(), manage.y(), manage.width(), manage.height()).build());
    }

    private void addRow(List<String> labels, int y) {
        int x = BankLayout.EDGE;
        for (String label : labels) {
            int width = buttonWidth(label);
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> open(pageFor(label)))
                    .dimensions(x, y, width, BankLayout.BUTTON_HEIGHT).build());
            x += width + BankLayout.GAP;
        }
    }

    private void addClose() {
        int width = buttonWidth(BankChrome.CLOSE);
        addDrawableChild(ButtonWidget.builder(Text.literal(BankChrome.CLOSE), button -> close())
                .dimensions(this.width - BankLayout.EDGE - width, BankLayout.closeY(this.height), width, BankLayout.BUTTON_HEIGHT).build());
    }

    private void initMoney(List<BankSnapshot.MoneyRow> rows, boolean includeAll, String verb) {
        ensurePay(rows);
        int top = contentTop() + 28;
        place("pay", centerX(listWidth()), top, listWidth(), Math.max(36, fieldTop() - 16 - top), moneyChoices(rows), payId);
        addAmountBlock(includeAll, verb);
    }

    private void initExchange() {
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.hardCurrencies(data);
        ensurePay(rows);
        ensureReceive(rows);
        int top = contentTop() + 28;
        int height = Math.max(36, fieldTop() - 16 - top);
        int[] columns = twoColumns();
        place("pay", columns[0], top, columns[2], height, moneyChoices(rows), payId);
        place("receive", columns[1], top, columns[2], height, moneyChoices(rows), receiveId);
        addAmountBlock(false, "确认兑换");
    }

    private void initChecks() {
        place("check", centerX(listWidth()), contentTop() + 28, listWidth(), 40, List.of(
                new ChoiceListWidget.Choice("issue", "开立汇票"),
                new ChoiceListWidget.Choice("redeem", "兑付汇票")), checkView);
        if (!"issue".equals(checkView)) {
            redeemButtonReady = true;
            String label = "确认兑付背包中的汇票、铜锭、钻石和银行券";
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> send(BankActions.presentHeld()))
                    .dimensions(mainLeft(), contentTop() + 120, buttonWidth(label), 20).build());
            return;
        }
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        ensurePay(rows);
        int top = contentTop() + 74;
        int height = Math.max(36, fieldTop() - 16 - top);
        int[] columns = twoColumns();
        place("pay", columns[0], top, columns[2], height, moneyChoices(rows), payId);
        place("payee", columns[1], top, columns[2], height, List.of(
                new ChoiceListWidget.Choice("bearer", "持票人兑付"),
                new ChoiceListWidget.Choice("self", "仅本人兑付")), payeeSelf ? "self" : "bearer");
        addAmountBlock(false, "确认开立汇票");
    }

    private void initMarket() {
        place("mode", centerX(listWidth()), contentTop() + 28, listWidth(), 76, List.of(
                new ChoiceListWidget.Choice("buy", "买入"),
                new ChoiceListWidget.Choice("sell", "卖出"),
                new ChoiceListWidget.Choice("swap", "互换"),
                new ChoiceListWidget.Choice("redeem", "通兑")), marketMode);
        List<BankSnapshot.NoteRow> notes = BankSnapshot.notes(data);
        ensureNote(notes);
        int top = contentTop() + 110;
        int height = Math.max(36, fieldTop() - 44 - top);
        if ("swap".equals(marketMode)) {
            int[] columns = twoColumns();
            place("note", columns[0], top, columns[2], height, noteChoices(notes), noteName);
            place("other", columns[1], top, columns[2], height, noteChoices(notes), otherNote);
        } else if ("redeem".equals(marketMode)) {
            List<BankSnapshot.MoneyRow> rows = BankSnapshot.hardCurrencies(data);
            if (findMoney(rows, receiveId) == null) {
                receiveId = currencyId(rows, "银行券", "note");
            }
            int[] columns = twoColumns();
            place("note", columns[0], top, columns[2], height, noteChoices(notes), noteName);
            place("receive", columns[1], top, columns[2], height, moneyChoices(rows), receiveId);
        } else {
            place("note", centerX(listWidth()), top, listWidth(), height, noteChoices(notes), noteName);
        }
        addAmountBlock(false, marketVerb());
    }

    private void initIssuance() {
        if (!BankSnapshot.hasPersonal(data)) {
            place("regime", centerX(listWidth()), contentTop() + 40, listWidth(), 72, List.of(
                    new ChoiceListWidget.Choice("float", "浮动"),
                    new ChoiceListWidget.Choice("peg", "钉住"),
                    new ChoiceListWidget.Choice("hard", "定额兑付")), "float");
            return;
        }
        addDrawableChild(textButton("宣告破产", mainLeft(), contentTop(), () -> {
            send(BankActions.noteBankrupt());
            page = Page.CONFIRM;
            client.execute(this::clearAndInit);
        }));
        if (BankSnapshot.bankrupt(data)) {
            return;
        }
        addAmountBlock(false, null);
        addEdgeButton("按准备金发行", true, () -> sendAmount(BankActions::noteIssue));
        addEdgeButton("无准备金虚发", false, () -> sendAmount(BankActions::noteUnbacked));
        int reserveY = contentTop() + 24;
        if (reserveY + 20 < fieldTop()) {
            addDrawableChild(textButton("存入央行准备金", mainLeft() + buttonWidth("宣告破产") + 8, reserveY,
                    () -> sendAmount(BankActions::reserveDeposit)));
            int withdrawX = mainLeft() + buttonWidth("宣告破产") + 8 + buttonWidth("存入央行准备金") + 6;
            if (withdrawX + buttonWidth("取出央行准备金") <= mainRight()) {
                addDrawableChild(textButton("取出央行准备金", withdrawX, reserveY, () -> sendAmount(BankActions::reserveWithdraw)));
            }
        }
    }

    private void initManagement() {
        if (!BankSnapshot.admin(data)) {
            page = Page.SAVINGS;
            return;
        }
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        ensurePay(rows);
        int top = contentTop() + 18 + BankHeader.managementAggregates(data).size() * 12;
        place("pay", centerX(listWidth()), top, listWidth(), Math.max(36, fieldTop() - top), moneyChoices(rows), payId);
        addAmountBlock(false, null);
        int y = BankLayout.contentBottom(this.height) - 20;
        int x = mainLeft();
        addDrawableChild(textButton("暂停或恢复兑付", x, y, () -> send(BankActions.adminFreeze())));
        x += buttonWidth("暂停或恢复兑付") + 6;
        if (x + buttonWidth("无准备金增发") <= mainRight()) {
            int unbackedX = x;
            addDrawableChild(textButton("无准备金增发", unbackedX, y,
                    () -> sendAmount(quantity -> BankActions.adminUnbacked(selected("pay", payId), quantity))));
            x += buttonWidth("无准备金增发") + 6;
        }
        if (x + buttonWidth("注销") <= mainRight()) {
            addDrawableChild(textButton("注销", x, y,
                    () -> sendAmount(quantity -> BankActions.adminRetire(selected("pay", payId), quantity))));
        }
    }

    private void initConfirm() {
        int y = contentTop() + 28;
        int back = buttonWidth("返回");
        int confirm = buttonWidth("确认宣告破产");
        int start = mainLeft() + Math.max(0, (mainWidth() - back - confirm - 8) / 2);
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> {
            send(BankActions.noteBankruptNo());
            page = Page.ISSUANCE;
            client.execute(this::clearAndInit);
        }).dimensions(start, y, back, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("确认宣告破产"), button -> send(BankActions.noteBankruptYes()))
                .dimensions(start + back + 8, y, confirm, 20).build());
    }

    private void addAmountBlock(boolean includeAll, String verb) {
        int actionY = BankLayout.contentBottom(this.height) - 20;
        int quickY = actionY - 22;
        int fieldY = quickY - 20;
        int fieldWidth = Math.min(120, Math.max(70, mainWidth() / 3));
        amount = new TextFieldWidget(textRenderer, centerX(fieldWidth), fieldY, fieldWidth, 16, Text.literal("数量"));
        amount.setMaxLength(32);
        amount.setText(amountText == null || amountText.isBlank() ? "1" : amountText);
        addDrawableChild(amount);
        List<String> labels = new ArrayList<>(List.of("1", "5", "10", "64"));
        if (includeAll) {
            labels.add("全部");
        }
        int total = 0;
        int[] widths = new int[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            widths[i] = "全部".equals(labels.get(i)) ? buttonWidth("全部") : 28;
            total += widths[i];
        }
        total += BankLayout.GAP * (labels.size() - 1);
        int x = mainLeft() + Math.max(0, (mainWidth() - total) / 2);
        for (int i = 0; i < labels.size(); i++) {
            String shown = labels.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal(shown), button -> quick(shown))
                    .dimensions(x, quickY, widths[i], 18).build());
            x += widths[i] + BankLayout.GAP;
        }
        if (verb != null) {
            int width = buttonWidth(verb);
            addDrawableChild(ButtonWidget.builder(Text.literal(verb), button -> submit())
                    .dimensions(centerX(width), actionY, width, 18).build());
        }
    }

    private void addEdgeButton(String label, boolean left, Runnable action) {
        int width = buttonWidth(label);
        int x = left ? mainLeft() : mainRight() - width;
        addDrawableChild(textButton(label, x, BankLayout.contentBottom(this.height) - 20, action));
    }

    private ButtonWidget textButton(String label, int x, int y, Runnable action) {
        return ButtonWidget.builder(Text.literal(label), button -> action.run())
                .dimensions(x, y, buttonWidth(label), 18).build();
    }

    private void place(String role, int x, int y, int width, int height, List<ChoiceListWidget.Choice> choices, String selected) {
        if (width < 40 || height < 20) {
            return;
        }
        ChoiceListWidget widget = new ChoiceListWidget(client, x, y, width, height);
        widget.setChoices(choices);
        widget.select(selected);
        addSelectableChild(widget);
        placed.add(new Placed(role, widget, x, y, width, height));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        drawColumn(context);
        drawPage(context);
        super.render(context, mouseX, mouseY, delta);
        for (Placed item : placed) {
            context.fill(item.x, item.y, item.x + item.width, item.y + item.height, 0x66000000);
            item.widget.render(context, mouseX, mouseY, delta);
            drawBalanceUnder(context, item);
        }
        if (redeemButtonReady) {
            drawRedeem(context);
        }
    }

    private void drawColumn(DrawContext context) {
        int x = BankLayout.EDGE;
        int y = BankLayout.EDGE;
        int width = BankLayout.columnWidth(this.width);
        context.fill(x - 2, BankLayout.EDGE, x + width + 2, BankLayout.contentBottom(this.height), 0x55001018);
        for (String line : BankRates.column(data)) {
            if (y > BankLayout.contentBottom(this.height) - 12) {
                break;
            }
            int color = 0xFFE6E6E6;
            if (line.startsWith("汇率") || line.startsWith("1 钻石")) {
                color = 0xFFE8D090;
            } else if (line.contains(BankRates.UNAVAILABLE)) {
                color = 0xFFAAAAAA;
            } else if (BankHeader.showStatus(line)) {
                color = statusColor(line);
            }
            y = drawWrapped(context, line, x, y, width, color);
        }
        int split = BankLayout.columnRight(this.width) + BankLayout.GAP;
        context.fill(split, BankLayout.EDGE, split + 1, BankLayout.contentBottom(this.height), 0x66D0B060);
    }

    private void drawPage(DrawContext context) {
        switch (page) {
            case SAVINGS -> drawSavings(context);
            case DEPOSIT -> drawHeading(context, "存入个人存款", "选择货币后，余额显示在列表下方。");
            case WITHDRAW -> drawHeading(context, "取出个人存款", "选择货币后，余额显示在列表下方。");
            case EXCHANGE -> drawHeading(context, "硬通货兑换", "只在铜锭、钻石和银行券之间兑换。");
            case CHECKS -> drawHeading(context, "汇票", "issue".equals(checkView) ? "开立汇票" : "兑付汇票");
            case MARKET -> drawHeading(context, "个人银行券交易", marketHeading());
            case ISSUANCE -> drawIssuance(context);
            case MANAGEMENT -> drawManagement(context);
            case CONFIRM -> context.drawText(textRenderer,
                    "确认后才会作废「" + safe(BankSnapshot.text(data, "personal")) + "」。返回则账目不变。",
                    mainLeft(), contentTop() + 8, 0xFFFF7777, false);
            default -> {
            }
        }
        if (!notice.isBlank() && page != Page.CONFIRM) {
            context.drawText(textRenderer, notice, mainLeft(), contentTop(), 0xFFFFCC88, false);
        }
    }

    private void drawSavings(DrawContext context) {
        drawCentered(context, BankChrome.SAVINGS_TITLE, contentTop(), 0xFFD0B060);
        drawCentered(context, "这是你自己的存款。", contentTop() + 14, 0xFFAAAAAA);
        int gap = 16;
        int column = Math.max(70, (mainWidth() - gap) / 2);
        int start = mainLeft() + Math.max(0, (mainWidth() - column * 2 - gap) / 2);
        int titleY = contentTop() + 34;
        context.drawText(textRenderer, "个人存款", start, titleY, 0xFFD0B060, false);
        context.drawText(textRenderer, "持有的个人银行券", start + column + gap, titleY, 0xFFD0B060, false);
        int left = titleY + 16;
        int right = titleY + 16;
        List<BankSnapshot.MoneyRow> currencies = BankSnapshot.currencies(data);
        if (currencies.isEmpty()) {
            context.drawText(textRenderer, "柜台还没有发来个人存款。", start, left, 0xFFAAAAAA, false);
        }
        for (BankSnapshot.MoneyRow row : currencies) {
            left = drawWrapped(context, row.name(), start, left, column, 0xFFE6E6E6);
            left = drawWrapped(context, BankSnapshot.balancePhrase(row.amount()), start, left, column, 0xFFB7C7B0);
            left += 4;
        }
        List<BankSnapshot.NoteRow> held = BankSnapshot.heldNotes(data);
        int noteX = start + column + gap;
        if (held.isEmpty()) {
            context.drawText(textRenderer, "没有持有个人银行券。", noteX, right, 0xFFAAAAAA, false);
            return;
        }
        for (BankSnapshot.NoteRow note : held) {
            right = drawWrapped(context, note.name(), noteX, right, column, 0xFFE6E6E6);
            right = drawWrapped(context, "账面余额：" + note.balance(), noteX, right, column, 0xFFB7C7B0);
            right = drawWrapped(context, note.price() == null ? BankRates.UNAVAILABLE : "现价：" + note.price(),
                    noteX, right, column, note.price() == null ? 0xFFAAAAAA : 0xFFE8D090);
            right += 4;
        }
    }

    private void drawIssuance(DrawContext context) {
        int x = mainLeft() + buttonWidth("宣告破产") + 10;
        context.drawText(textRenderer, "个人发行", x, contentTop(), 0xFFD0B060, false);
        if (!BankSnapshot.hasPersonal(data)) {
            drawWrapped(context, "还没有记名。点选浮动、钉住或定额兑付后，在聊天框里补上名字再回车。名字最多三个汉字或英文字母。",
                    mainLeft(), contentTop() + 16, mainWidth(), 0xFFE6E6E6);
            return;
        }
        int line = contentTop() + 16;
        context.drawText(textRenderer, "「" + safe(BankSnapshot.text(data, "personal")) + "」", x, line, 0xFFE8D090, false);
        line += 14;
        String regime = BankSnapshot.regimeLabel(BankSnapshot.text(data, "regime"));
        if (regime != null) {
            context.drawText(textRenderer, "发行方式：" + regime, x, line, 0xFFE6E6E6, false);
            line += 12;
        }
        BankSnapshot.NoteRow own = findNote(BankSnapshot.text(data, "personal"));
        if (own != null) {
            context.drawText(textRenderer, BankSnapshot.balancePhrase(own.balance()), x, line, 0xFFE6E6E6, false);
            line += 12;
            context.drawText(textRenderer, own.price() == null ? BankRates.UNAVAILABLE : "现价：" + own.price(),
                    x, line, own.price() == null ? 0xFFAAAAAA : 0xFFE8D090, false);
            line += 12;
        }
        if (BankSnapshot.text(data, "circulation") != null) {
            context.drawText(textRenderer, "流通量：" + BankSnapshot.text(data, "circulation"), x, line, 0xFFE6E6E6, false);
            line += 12;
        }
        if (BankSnapshot.text(data, "reserve") != null || BankSnapshot.text(data, "required") != null) {
            context.drawText(textRenderer, "央行准备金：" + safe(BankSnapshot.text(data, "reserve")) + " / " + safe(BankSnapshot.text(data, "required")),
                    x, line, 0xFFE6E6E6, false);
        }
        if (BankSnapshot.bankrupt(data)) {
            context.drawText(textRenderer, "这张个人银行券已经破产。", mainLeft(), contentTop() + 40, 0xFFFF7777, false);
        }
    }

    private void drawManagement(DrawContext context) {
        int x = mainLeft();
        int line = contentTop();
        context.drawText(textRenderer, BankChrome.MANAGEMENT, x, line, 0xFFD0B060, false);
        line += 14;
        for (String aggregate : BankHeader.managementAggregates(data)) {
            context.drawText(textRenderer, aggregate, x, line, 0xFFE6E6E6, false);
            line += 12;
        }
        int limit = fieldTop() - 4;
        if (BankSnapshot.central(data).isEmpty() && !BankSnapshot.has(data, "issuanceDiamond")) {
            line = drawWrapped(context, "发行准备金和储蓄保管不在当前柜台数据里。", x, line, mainWidth(), 0xFFAAAAAA);
        }
        for (BankSnapshot.CentralRow row : BankSnapshot.central(data)) {
            if (line > limit) {
                break;
            }
            StringBuilder text = new StringBuilder(row.name());
            if (row.savings() != null) {
                text.append("  储蓄总池 ").append(row.savings());
            }
            if (row.reserve() != null) {
                text.append("  发行准备金 ").append(row.reserve());
            }
            if (row.vault() != null) {
                text.append("  储蓄保管 ").append(row.vault());
            }
            line = drawWrapped(context, text.toString(), x, line, mainWidth(), 0xFFE6E6E6);
        }
        if (!BankSnapshot.has(data, "institutions")) {
            if (line <= limit) {
                line = drawWrapped(context, "商店经营账户和土地出让账户不在当前柜台数据里。", x, line, mainWidth(), 0xFFAAAAAA);
            }
        } else {
            for (BankSnapshot.InstitutionRow row : BankSnapshot.institutions(data)) {
                if (line > limit) {
                    break;
                }
                line = drawWrapped(context, row.name() + "  " + safe(row.detail()), x, line, mainWidth(), 0xFFE6E6E6);
            }
        }
        if (!BankSnapshot.has(data, "interest") && !BankSnapshot.has(data, "issuable") && line <= limit) {
            drawWrapped(context, "年利率和尚可发行的数量不在当前柜台数据里。", x, line, mainWidth(), 0xFF888888);
        }
    }

    private void drawRedeem(DrawContext context) {
        int y = contentTop() + 78;
        y = drawWrapped(context, "兑付背包里的汇票时，背包中的铜锭、钻石和银行券也会一并存入个人存款。",
                mainLeft(), y, mainWidth(), 0xFFE6E6E6);
        drawWrapped(context, "柜台没有只兑付汇票的动作。", mainLeft(), y, mainWidth(), 0xFFAAAAAA);
    }

    private void drawHeading(DrawContext context, String title, String copy) {
        context.drawText(textRenderer, title, mainLeft(), contentTop(), 0xFFD0B060, false);
        if (copy != null && !copy.isBlank()) {
            context.drawText(textRenderer, copy, mainLeft(), contentTop() + 12, 0xFFAAAAAA, false);
        }
    }

    private void drawCentered(DrawContext context, String text, int y, int color) {
        int width = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, mainLeft() + Math.max(0, (mainWidth() - width) / 2), y, color, false);
    }

    private void drawBalanceUnder(DrawContext context, Placed item) {
        if (!showsBalance(item.role)) {
            return;
        }
        String line = balanceCaption(item) + BankSnapshot.balancePhrase(balanceFor(item.role, item.widget.selectedId()));
        context.drawText(textRenderer, line, item.x, item.y + item.height + 2, 0xFFB7C7B0, false);
        if ("note".equals(item.role) && ("buy".equals(marketMode) || "sell".equals(marketMode) || "redeem".equals(marketMode))) {
            BankSnapshot.NoteRow note = findNote(item.widget.selectedId());
            String price = note == null || note.price() == null ? BankRates.UNAVAILABLE : "现价：" + note.price();
            context.drawText(textRenderer, price, item.x, item.y + item.height + 14, note != null && note.price() != null ? 0xFFE8D090 : 0xFFAAAAAA, false);
            if ("buy".equals(marketMode) || "sell".equals(marketMode)) {
                context.drawText(textRenderer, "结算货币：银行券。", item.x, item.y + item.height + 26, 0xFFAAAAAA, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Placed item : placed) {
            if (!item.widget.isMouseOver(mouseX, mouseY) || !item.widget.mouseClicked(mouseX, mouseY, button)) {
                continue;
            }
            onList(item);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onList(Placed item) {
        String id = item.widget.selectedId();
        if (id == null) {
            return;
        }
        switch (item.role) {
            case "pay" -> payId = id;
            case "receive" -> receiveId = id;
            case "note" -> noteName = id;
            case "other" -> otherNote = id;
            case "mode" -> {
                if (!id.equals(marketMode)) {
                    marketMode = id;
                    if ("redeem".equals(id)) {
                        receiveId = currencyId(BankSnapshot.hardCurrencies(data), "银行券", "note");
                    }
                    client.execute(this::clearAndInit);
                }
            }
            case "check" -> {
                if (!id.equals(checkView)) {
                    checkView = id;
                    client.execute(this::clearAndInit);
                }
            }
            case "payee" -> payeeSelf = "self".equals(id);
            case "regime" -> {
                if (client != null) {
                    client.setScreen(new ChatScreen(BankActions.noteNameCommand(id)));
                }
            }
            default -> {
            }
        }
    }

    private void open(Page next) {
        captureSelections();
        page = next;
        notice = "";
        client.execute(this::clearAndInit);
    }

    private void quick(String shown) {
        amountText = shown;
        if (amount != null) {
            amount.setText(shown);
        }
        submit();
    }

    private void submit() {
        captureSelections();
        String quantity = QuantityTokens.token(amountText);
        switch (page) {
            case DEPOSIT -> sendIfPresent(selected("pay", payId), () -> BankActions.deposit(selected("pay", payId), quantity));
            case WITHDRAW -> sendIfPresent(selected("pay", payId), () -> BankActions.withdraw(selected("pay", payId), quantity));
            case EXCHANGE -> submitExchange(quantity);
            case CHECKS -> {
                if (!"issue".equals(checkView)) {
                    send(BankActions.presentHeld());
                    return;
                }
                sendIfPresent(selected("pay", payId), () -> BankActions.check(selected("pay", payId), quantity, checkSelf()));
            }
            case MARKET -> submitMarket(quantity);
            default -> {
            }
        }
    }

    private void submitExchange(String quantity) {
        String pay = selected("pay", payId);
        String receive = selected("receive", receiveId);
        if (pay.isBlank() || receive.isBlank()) {
            notice = "请选择付出的货币和得到的货币。";
            return;
        }
        if (pay.equals(receive)) {
            notice = "付出和得到不能是同一种货币。";
            return;
        }
        send(BankActions.exchange(pay, receive, quantity));
    }

    private void submitMarket(String quantity) {
        String note = selected("note", noteName);
        if (note.isBlank()) {
            notice = "请选择一张个人银行券。";
            return;
        }
        switch (marketMode) {
            case "sell" -> send(BankActions.noteSell(note, quantity));
            case "swap" -> {
                String other = selected("other", otherNote);
                if (other.isBlank() || other.equals(note)) {
                    notice = "请再选择一张要换成的个人银行券。";
                    return;
                }
                send(BankActions.noteSwap(note, other, quantity));
            }
            case "redeem" -> {
                String payout = selected("receive", receiveId);
                if (payout.isBlank()) {
                    notice = "请选择通兑成的货币。";
                    return;
                }
                send(BankActions.noteRedeem(note, payout, quantity));
            }
            default -> send(BankActions.noteBuy(note, quantity));
        }
    }

    private void sendIfPresent(String id, java.util.function.Supplier<String> action) {
        if (id == null || id.isBlank()) {
            notice = "请先在列表中选择一项。";
            return;
        }
        send(action.get());
    }

    private void sendAmount(java.util.function.Function<String, String> action) {
        captureSelections();
        String quantity = QuantityTokens.token(amountText);
        if ("all".equals(quantity)) {
            notice = "这里不能使用全部，请填写数量。";
            return;
        }
        send(action.apply(quantity));
    }

    private void send(String action) {
        notice = "";
        BankUiClient.send(action);
    }

    private void captureSelections() {
        rememberAmount();
        payId = selected("pay", payId);
        receiveId = selected("receive", receiveId);
        noteName = selected("note", noteName);
        otherNote = selected("other", otherNote);
        String mode = selected("mode", marketMode);
        if (!mode.isBlank()) {
            marketMode = mode;
        }
        String view = selected("check", checkView);
        if (!view.isBlank()) {
            checkView = view;
        }
        String payee = selected("payee", "");
        if ("self".equals(payee)) {
            payeeSelf = true;
        } else if ("bearer".equals(payee)) {
            payeeSelf = false;
        }
    }

    private void rememberAmount() {
        if (amount != null) {
            amountText = amount.getText();
        }
    }

    private String selected(String role, String fallback) {
        Placed item = placed(role);
        if (item != null && item.widget.selectedId() != null) {
            return item.widget.selectedId();
        }
        return fallback == null ? "" : fallback;
    }

    private Placed placed(String role) {
        for (Placed item : placed) {
            if (item.role.equals(role)) {
                return item;
            }
        }
        return null;
    }

    private void ensurePay(List<BankSnapshot.MoneyRow> rows) {
        if (findMoney(rows, payId) == null) {
            payId = rows.isEmpty() ? "" : rows.get(0).id();
        }
    }

    private void ensureReceive(List<BankSnapshot.MoneyRow> rows) {
        if (findMoney(rows, receiveId) != null && !receiveId.equals(payId)) {
            return;
        }
        for (BankSnapshot.MoneyRow row : rows) {
            if (!row.id().equals(payId)) {
                receiveId = row.id();
                return;
            }
        }
        receiveId = payId;
    }

    private void ensureNote(List<BankSnapshot.NoteRow> notes) {
        if (notes.isEmpty()) {
            noteName = "";
            otherNote = "";
            return;
        }
        if (findNote(noteName) == null) {
            noteName = notes.get(0).name();
        }
        if (notes.size() > 1 && (findNote(otherNote) == null || otherNote.equals(noteName))) {
            for (BankSnapshot.NoteRow note : notes) {
                if (!note.name().equals(noteName)) {
                    otherNote = note.name();
                    return;
                }
            }
        }
    }

    private boolean checkSelf() {
        String payee = selected("payee", "");
        if ("self".equals(payee)) {
            return true;
        }
        if ("bearer".equals(payee)) {
            return false;
        }
        return payeeSelf;
    }

    private String balanceFor(String role, String id) {
        if ("note".equals(role) || "other".equals(role)) {
            BankSnapshot.NoteRow note = findNote(id);
            return note == null ? null : note.balance();
        }
        BankSnapshot.MoneyRow money = findMoney(BankSnapshot.currencies(data), id);
        return money == null ? null : money.amount();
    }

    private String balanceCaption(Placed item) {
        boolean paired = page == Page.EXCHANGE
                || (page == Page.MARKET && "swap".equals(marketMode))
                || (page == Page.MARKET && "redeem".equals(marketMode) && "receive".equals(item.role));
        return paired ? caption(item.role) : "";
    }

    private static String caption(String role) {
        return switch (role) {
            case "pay" -> "付出  ";
            case "receive" -> "得到  ";
            case "other" -> "换成  ";
            case "note" -> "个人银行券  ";
            default -> "";
        };
    }

    private boolean showsBalance(String role) {
        return "pay".equals(role) || "receive".equals(role) || "note".equals(role) || "other".equals(role);
    }

    private String marketHeading() {
        return switch (marketMode) {
            case "sell" -> "卖出个人银行券，按银行券结算。";
            case "swap" -> "把一张个人银行券换成另一张。";
            case "redeem" -> "按该券的规则通兑成铜锭、钻石或银行券。";
            default -> "买入个人银行券，按银行券结算。";
        };
    }

    private String marketVerb() {
        return switch (marketMode) {
            case "sell" -> "确认卖出";
            case "swap" -> "确认互换";
            case "redeem" -> "确认通兑";
            default -> "确认买入";
        };
    }

    private List<ChoiceListWidget.Choice> moneyChoices(List<BankSnapshot.MoneyRow> rows) {
        List<ChoiceListWidget.Choice> choices = new ArrayList<>();
        for (BankSnapshot.MoneyRow row : rows) {
            choices.add(new ChoiceListWidget.Choice(row.id(), row.name()));
        }
        return choices;
    }

    private List<ChoiceListWidget.Choice> noteChoices(List<BankSnapshot.NoteRow> notes) {
        List<ChoiceListWidget.Choice> choices = new ArrayList<>();
        for (BankSnapshot.NoteRow note : notes) {
            choices.add(new ChoiceListWidget.Choice(note.name(), note.name()));
        }
        return choices;
    }

    private BankSnapshot.MoneyRow findMoney(List<BankSnapshot.MoneyRow> rows, String id) {
        for (BankSnapshot.MoneyRow row : rows) {
            if (row.id().equals(id)) {
                return row;
            }
        }
        return null;
    }

    private BankSnapshot.NoteRow findNote(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (BankSnapshot.NoteRow note : BankSnapshot.notes(data)) {
            if (name.equals(note.name())) {
                return note;
            }
        }
        return null;
    }

    private static String currencyId(List<BankSnapshot.MoneyRow> rows, String name, String fallback) {
        for (BankSnapshot.MoneyRow row : rows) {
            if (name.equals(row.name()) || fallback.equals(row.id())) {
                return row.id();
            }
        }
        return rows.isEmpty() ? "" : rows.get(0).id();
    }

    private int[] twoColumns() {
        int width = Math.min(150, Math.max(64, (mainWidth() - 12) / 2));
        int total = width * 2 + 12;
        int start = mainLeft() + Math.max(0, (mainWidth() - total) / 2);
        return new int[] {start, start + width + 12, width};
    }

    private int listWidth() {
        return Math.min(180, Math.max(80, mainWidth() - 8));
    }

    private int centerX(int width) {
        return mainLeft() + Math.max(0, (mainWidth() - width) / 2);
    }

    private int mainLeft() {
        return BankLayout.mainLeft(this.width);
    }

    private int mainRight() {
        return BankLayout.mainRight(this.width);
    }

    private int mainWidth() {
        return BankLayout.mainWidth(this.width);
    }

    private int contentTop() {
        return BankLayout.contentTop(BankSnapshot.admin(data) || page == Page.MANAGEMENT);
    }

    private int fieldTop() {
        return BankLayout.contentBottom(this.height) - 62;
    }

    private int buttonWidth(String label) {
        return Math.max(BankLayout.labelWidth(label), textRenderer.getWidth(label) + 16);
    }

    private int drawWrapped(DrawContext context, String line, int x, int y, int maxWidth, int color) {
        if (line == null || line.isEmpty()) {
            return y + 12;
        }
        String rest = line;
        while (!rest.isEmpty() && y < this.height) {
            String part = rest;
            while (textRenderer.getWidth(part) > maxWidth && part.length() > 1) {
                int split = part.lastIndexOf(' ');
                if (split <= 0) {
                    break;
                }
                part = part.substring(0, split);
            }
            context.drawText(textRenderer, part, x, y, color, false);
            y += 12;
            if (part.equals(rest)) {
                break;
            }
            rest = rest.substring(part.length()).stripLeading();
        }
        return y;
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

    private static Page pageFor(String label) {
        return switch (label) {
            case "存入" -> Page.DEPOSIT;
            case "取出" -> Page.WITHDRAW;
            case "硬通货兑换" -> Page.EXCHANGE;
            case "汇票" -> Page.CHECKS;
            case "个人银行券交易" -> Page.MARKET;
            default -> Page.SAVINGS;
        };
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            BankUiClient.send(BankActions.close());
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
