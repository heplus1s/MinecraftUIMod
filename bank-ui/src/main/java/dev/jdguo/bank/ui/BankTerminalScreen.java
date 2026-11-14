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
        SAVINGS, DEPOSIT, WITHDRAW, EXCHANGE, ISSUE, REDEEM, MARKET, ISSUANCE, ISSUANCE_SWITCH,
        MANAGEMENT, MANAGEMENT_ADJUST, CONFIRM
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
    private boolean payeeSelf;
    private String amountText = "1";
    private String notice = "";
    private String openDrawer = "";
    private int bookScroll;
    private int bookX;
    private int bookY;
    private int bookW;
    private int bookH;
    private ButtonWidget unbackedButton;
    private ButtonWidget retireButton;
    private ButtonWidget backedButton;
    private boolean closing;
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
        pageBand = null;
        placed.clear();
        unbackedButton = null;
        retireButton = null;
        backedButton = null;
        managementPane = null;
        managementBookBeside = false;
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
            case ISSUE -> initIssue();
            case REDEEM -> initRedeem();
            case MARKET -> initMarket();
            case ISSUANCE -> initIssuance();
            case ISSUANCE_SWITCH -> initIssuanceSwitch();
            case MANAGEMENT -> initManagement();
            case MANAGEMENT_ADJUST -> initAdjust();
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
        int manageWidth = Math.max(120, buttonWidth(BankChrome.MANAGEMENT));
        int y = BankLayout.EDGE + BankLayout.BUTTON_HEIGHT + BankLayout.TOP_GAP;
        BankLayout.Box manage = BankLayout.topRight(this.width, y, BankChrome.MANAGEMENT, manageWidth);
        if (manage.x() < mainLeft()) {
            manage = new BankLayout.Box(manage.label(), mainLeft(), manage.y(), manage.width(), manage.height());
        }
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

    private BankLayout.Band pageBand;
    private BankLayout.ManagementPane managementPane;
    private boolean managementBookBeside;

    private void initMoney(List<BankSnapshot.MoneyRow> rows, boolean includeAll, String verb) {
        ensurePay(rows);
        int y = contentTop() + 28;
        y = placeDrawer("pay", centerX(listWidth()), y, listWidth(), moneyChoices(rows), payId, fieldLimit(1));
        addAmountBlockAt(y, includeAll, verb);
    }

    private void initExchange() {
        List<ChoiceListWidget.Choice> rows = exchangeChoices();
        ensureExchangeIds(rows);
        int y = contentTop() + 36;
        int[] columns = twoColumns();
        int limit = fieldLimit(1);
        int payBottom = placeDrawer("pay", columns[0], y, columns[2], rows, payId, limit);
        int receiveBottom = placeDrawer("receive", columns[1], y, columns[2], rows, receiveId, limit);
        addAmountBlockAt(Math.max(payBottom, receiveBottom), false, "确认兑换");
    }

    private void initIssue() {
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        ensurePay(rows);
        int y = contentTop() + 36;
        int[] columns = twoColumns();
        int limit = fieldLimit(1);
        int payBottom = placeDrawer("pay", columns[0], y, columns[2], moneyChoices(rows), payId, limit);
        int payeeBottom = placeDrawer("payee", columns[1], y, columns[2], List.of(
                new ChoiceListWidget.Choice("bearer", "持票人兑付"),
                new ChoiceListWidget.Choice("self", "仅本人兑付")), payeeSelf ? "self" : "bearer", limit);
        addAmountBlockAt(Math.max(payBottom, payeeBottom), false, "确认开立汇票");
    }

    private void initRedeem() {
        pageBand = BankLayout.band(contentTop(), BankLayout.contentBottom(this.height), 28);
        String label = BankActions.MAIN_HAND_BUTTON;
        int width = buttonWidth(label);
        int x = mainLeft() + Math.max(0, (mainWidth() - width) / 2);
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> send(BankActions.presentHeld()))
                .dimensions(x, pageBand.actionY(), width, 20).build());
    }

    private void initMarket() {
        List<BankSnapshot.NoteRow> notes = BankSnapshot.notes(data);
        ensureNote(notes);
        int y = contentTop() + 28;
        int limit = fieldLimit(1);
        y = placeDrawer("mode", centerX(listWidth()), y, listWidth(), List.of(
                new ChoiceListWidget.Choice("buy", "买入"),
                new ChoiceListWidget.Choice("sell", "卖出"),
                new ChoiceListWidget.Choice("swap", "互换"),
                new ChoiceListWidget.Choice("redeem", "通兑")), marketMode, limit);
        if ("swap".equals(marketMode)) {
            int[] columns = twoColumns();
            int noteBottom = placeDrawer("note", columns[0], y, columns[2], noteChoices(notes), noteName, limit);
            int otherBottom = placeDrawer("other", columns[1], y, columns[2], noteChoices(notes), otherNote, limit);
            y = Math.max(noteBottom, otherBottom);
        } else if ("redeem".equals(marketMode)) {
            List<BankSnapshot.MoneyRow> rows = BankSnapshot.hardCurrencies(data);
            if (findMoney(rows, receiveId) == null) {
                receiveId = currencyId(rows, "银行券", "note");
            }
            int[] columns = twoColumns();
            int noteBottom = placeDrawer("note", columns[0], y, columns[2], noteChoices(notes), noteName, limit);
            int payoutBottom = placeDrawer("receive", columns[1], y, columns[2], moneyChoices(rows), receiveId, limit);
            y = Math.max(noteBottom, payoutBottom);
        } else {
            y = placeDrawer("note", centerX(listWidth()), y, listWidth(), noteChoices(notes), noteName, limit);
        }
        addAmountBlockAt(y, false, marketVerb());
    }

    private void initIssuance() {
        if (!BankSnapshot.hasPersonal(data)) {
            placeDrawer("regime", centerX(listWidth()), contentTop() + 36, listWidth(), List.of(
                    new ChoiceListWidget.Choice("float", "浮动汇率制"),
                    new ChoiceListWidget.Choice("peg", "联系汇率制"),
                    new ChoiceListWidget.Choice("hard", "固定汇率制")), "float", BankLayout.contentBottom(this.height) - 8);
            return;
        }
        addDrawableChild(textButton("宣告破产", mainLeft(), contentTop(), () -> {
            send(BankActions.noteBankrupt());
            page = Page.CONFIRM;
            client.execute(this::clearAndInit);
        }));
        addDrawableChild(textButton("查看能否切换发行制度", mainLeft(), contentTop() + 22, () -> open(Page.ISSUANCE_SWITCH)));
        if (BankSnapshot.bankrupt(data)) {
            return;
        }
        addAmountBlock(false, null);
        addEdgeButton("按准备金发行", true, () -> sendAmount(BankActions::noteIssue));
        addEdgeButton("无准备金虚发", false, () -> sendAmount(BankActions::noteUnbacked));
        int reserveY = contentTop() + 46;
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
        String label = "进入货币调整";
        int width = Math.min(mainWidth(), buttonWidth(label));
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> open(Page.MANAGEMENT_ADJUST))
                .dimensions(centerX(width), BankLayout.contentBottom(this.height) - 22, width, 18).build());
    }

    private void initAdjust() {
        if (!BankSnapshot.admin(data)) {
            page = Page.SAVINGS;
            return;
        }
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        ensurePay(rows);
        int y = contentTop() + 28;
        y = placeFixedList("pay", centerX(listWidth()), y, listWidth(), moneyChoices(rows), payId, fieldLimit(4));
        addAmountBlockAt(y, false, null);
        String name = adjustName();
        String qty = currentAmount();
        int x = mainLeft();
        int width = Math.max(40, mainWidth());
        int buttonY = pageBand.quickY() + 22;
        addDrawableChild(ButtonWidget.builder(Text.literal(BankBook.freezeButton(data)), button -> send(BankActions.adminFreeze()))
                .dimensions(x, buttonY, width, 18).build());
        unbackedButton = ButtonWidget.builder(Text.literal(adjustLabel("无准备金增发", qty, name)), button ->
                        sendAmount(quantity -> BankActions.adminUnbacked(selected("pay", payId), quantity)))
                .dimensions(x, buttonY + 22, width, 18).build();
        retireButton = ButtonWidget.builder(Text.literal(adjustLabel("从流通中注销", qty, name)), button ->
                        sendAmount(quantity -> BankActions.adminRetire(selected("pay", payId), quantity)))
                .dimensions(x, buttonY + 44, width, 18).build();
        backedButton = ButtonWidget.builder(Text.literal("按准备金满额发行 " + name), button -> {
            String currency = selected("pay", payId);
            if (currency == null || currency.isBlank()) {
                notice = "请先在列表中选择一项。";
                return;
            }
            send(BankActions.adminBacked(currency));
        }).dimensions(x, buttonY + 66, width, 18).build();
        addDrawableChild(unbackedButton);
        addDrawableChild(retireButton);
        addDrawableChild(backedButton);
        addDrawableChild(textButton("返回总账", mainRight() - buttonWidth("返回总账"), contentTop(), () -> open(Page.MANAGEMENT)));
    }

    private void initIssuanceSwitch() {
        if (!BankSnapshot.hasPersonal(data)) {
            page = Page.ISSUANCE;
            return;
        }
        addDrawableChild(textButton("返回个人发行", mainLeft(), contentTop(), () -> open(Page.ISSUANCE)));
        String blocked = BankRegimes.blockReason(data);
        if (blocked != null) {
            return;
        }
        int y = contentTop() + 100;
        if (!BankRegimes.switchesKnown(data)) {
            return;
        }
        for (BankRegimes.Offer offer : BankRegimes.offers(data)) {
            String button = offer.button();
            if (button == null) {
                y += 36;
                continue;
            }
            int width = Math.min(mainWidth(), buttonWidth(button));
            addDrawableChild(ButtonWidget.builder(Text.literal(button), pressed -> send(offer.command()))
                    .dimensions(mainLeft(), y, width, 18).build());
            y += 40;
        }
    }

    private int fieldLimit(int buttonRows) {
        int buttonsTop = BankLayout.contentBottom(this.height) - 22 * Math.max(1, buttonRows);
        return buttonsTop - 22 - 20;
    }

    private int placeDrawer(String role, int x, int y, int width, List<ChoiceListWidget.Choice> choices, String selected, int limit) {
        boolean open = role.equals(openDrawer);
        int gap = "note".equals(role) ? 28 : 16;
        int height = BankLayout.drawerHeight(open, choices.size());
        if (y + height + gap > limit) {
            height = Math.max(20, limit - gap - y);
        }
        place(role, x, y, width, height, choices, selected);
        return y + height + gap;
    }

    /** Value adjustments keep an open framed list. Option pickers use drawers. */
    private int placeFixedList(String role, int x, int y, int width, List<ChoiceListWidget.Choice> choices, String selected, int limit) {
        int height = Math.min(72, Math.max(54, choices.size() * 18 + 4));
        if (y + height + 16 > limit) {
            height = Math.max(36, limit - 16 - y);
        }
        if (width < 40 || height < 16) {
            return y + 16;
        }
        ChoiceListWidget widget = new ChoiceListWidget(client, x, y, width, height);
        widget.setDrawer(false);
        widget.setChoices(choices);
        widget.select(selected);
        addSelectableChild(widget);
        placed.add(new Placed(role, widget, x, y, width, height));
        return y + height + 16;
    }

    private void addAmountBlockAt(int y, boolean includeAll, String verb) {
        int fieldY = y;
        int quickY = fieldY + 20;
        int actionY = quickY + 22;
        if (actionY + 18 > BankLayout.contentBottom(this.height)) {
            int shift = actionY + 18 - BankLayout.contentBottom(this.height);
            fieldY -= shift;
            quickY -= shift;
            actionY -= shift;
        }
        pageBand = new BankLayout.Band(0, 0, fieldY, quickY, actionY);
        addAmountBlock(includeAll, verb);
    }

    private static String adjustLabel(String verb, String quantity, String currency) {
        return verb + " " + quantity + " " + currency;
    }

    private String adjustName() {
        String name = currencyName(selected("pay", payId));
        return name == null || name.isBlank() ? "未选择货币" : name;
    }

    private String currentAmount() {
        if (amount != null && amount.getText() != null && !amount.getText().isBlank()) {
            return amount.getText().trim();
        }
        return amountText == null || amountText.isBlank() ? "1" : amountText.trim();
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
        if (pageBand == null) {
            pageBand = BankLayout.band(contentTop(), BankLayout.contentBottom(this.height), 28);
        }
        int fieldY = pageBand.fieldY();
        int quickY = pageBand.quickY();
        int actionY = pageBand.actionY();
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
        if (width < 40 || height < 16) {
            return;
        }
        ChoiceListWidget widget = new ChoiceListWidget(client, x, y, width, height);
        widget.setChoices(choices);
        widget.select(selected);
        widget.setDrawer(true);
        widget.setExpanded(role.equals(openDrawer));
        addSelectableChild(widget);
        placed.add(new Placed(role, widget, x, y, width, height));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        drawColumn(context);
        drawPage(context);
        super.render(context, mouseX, mouseY, delta);
        refreshAdjustLabels();
        for (Placed item : placed) {
            context.fill(item.x, item.y, item.x + item.width, item.y + item.height, 0xF0101014);
            context.fill(item.x, item.y, item.x + item.width, item.y + 1, 0xFF8A7040);
            context.fill(item.x, item.y + item.height - 1, item.x + item.width, item.y + item.height, 0xFF8A7040);
            if (page == Page.EXCHANGE && ("pay".equals(item.role) || "receive".equals(item.role))) {
                context.drawText(textRenderer, "pay".equals(item.role) ? "付出" : "得到", item.x, item.y - 11, 0xFFE8D090, false);
            }
            item.widget.render(context, mouseX, mouseY, delta);
            drawBalanceUnder(context, item);
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
            if (line.startsWith("1 钻石") || line.startsWith("1 银行券") || line.startsWith("1 【")) {
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
            case EXCHANGE -> drawHeading(context, "硬通货兑换", "按牌价在账户之间兑换。这不是通兑，不动发行人的准备金。");
            case ISSUE -> drawHeading(context, "开立汇票", "选择币种和抬头，再填写开出的数量。");
            case REDEEM -> drawRedeem(context);
            case MARKET -> drawHeading(context, "个人银行券交易", marketHeading());
            case ISSUANCE -> drawIssuance(context);
            case ISSUANCE_SWITCH -> drawIssuanceSwitch(context);
            case MANAGEMENT -> drawManagement(context);
            case MANAGEMENT_ADJUST -> drawHeading(context, "货币调整", "准备金和央行储蓄在总账里逐项列出，不随这里的选择改变。");
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
            drawWrapped(context, "还没有记名。点选浮动汇率制、联系汇率制或固定汇率制后，在聊天框里补上名字再回车。名字最多三个汉字或英文字母。",
                    mainLeft(), contentTop() + 16, mainWidth(), 0xFFE6E6E6);
            int essay = contentTop() + 56;
            for (String rule : BankRegimes.rules()) {
                essay = drawWrapped(context, rule, mainLeft(), essay, mainWidth(), 0xFFAAAAAA);
            }
            return;
        }
        int line = contentTop() + 70;
        int limit = pageBand == null ? BankLayout.contentBottom(this.height) - 12 : pageBand.fieldY() - 2;
        String personal = BankSnapshot.text(data, "personal");
        if (personal != null) {
            context.drawText(textRenderer, "「" + personal + "」", x, contentTop() + 16, 0xFFE8D090, false);
        }
        for (String row : BankRegimes.figures(data)) {
            context.drawText(textRenderer, row, mainLeft(), line, row.contains(BankBook.MISSING) || row.contains("暂无牌价") ? 0xFFAAAAAA : 0xFFE6E6E6, false);
            line += 12;
            if (line > limit) {
                break;
            }
        }
        String regime = BankSnapshot.regimeName(data);
        String rule = BankRegimes.FLOAT_RULE;
        if (regime != null && regime.contains("联系")) {
            rule = BankRegimes.PEG_RULE;
        } else if (regime != null && regime.contains("固定")) {
            rule = BankRegimes.HARD_RULE;
        }
        if (line <= limit) {
            drawWrapped(context, rule, mainLeft(), line, mainWidth(), 0xFFAAAAAA);
        }
        if (BankSnapshot.bankrupt(data)) {
            context.drawText(textRenderer, "这张个人银行券已经破产。", mainLeft(), contentTop() + 40, 0xFFFF7777, false);
        }
    }

    private void drawIssuanceSwitch(DrawContext context) {
        int x = mainLeft() + buttonWidth("返回个人发行") + 8;
        context.drawText(textRenderer, "切换发行制度", x, contentTop(), 0xFFD0B060, false);
        drawFramedLines(context, BankRegimes.gates(), mainLeft(), contentTop() + 18, mainWidth(), 76, 0);
        String blocked = BankRegimes.blockReason(data);
        if (blocked != null) {
            context.drawText(textRenderer, blocked, mainLeft(), contentTop() + 100, 0xFFFF7777, false);
            return;
        }
        if (!BankRegimes.switchesKnown(data)) {
            context.drawText(textRenderer, BankRegimes.NO_SWITCH, mainLeft(), contentTop() + 100, 0xFFAAAAAA, false);
            return;
        }
        int y = contentTop() + 100;
        if (BankRegimes.offers(data).isEmpty()) {
            context.drawText(textRenderer, "柜台没有列出其他发行制度。", mainLeft(), y, 0xFFAAAAAA, false);
            return;
        }
        for (BankRegimes.Offer offer : BankRegimes.offers(data)) {
            if (offer.button() == null) {
                drawWrapped(context, offer.refusal(), mainLeft(), y, mainWidth(), 0xFFAAAAAA);
                y += 36;
            } else {
                y += 40;
            }
        }
    }

    private void drawManagement(DrawContext context) {
        context.drawText(textRenderer, BankChrome.MANAGEMENT, mainLeft(), contentTop(), 0xFFD0B060, false);
        int top = contentTop() + 16;
        int bottom = BankLayout.contentBottom(this.height) - 28;
        bookX = mainLeft();
        bookY = top;
        bookW = mainWidth();
        bookH = Math.max(36, bottom - top);
        List<String> lines = BankBook.visibleLedger(data);
        int maxScroll = Math.max(0, lines.size() * 12 - (bookH - 8));
        if (bookScroll > maxScroll) {
            bookScroll = maxScroll;
        }
        drawFramedLines(context, lines, bookX, bookY, bookW, bookH, bookScroll);
    }

    private void drawFramedLines(DrawContext context, List<String> lines, int x, int y, int width, int height, int scroll) {
        context.fill(x, y, x + width, y + height, 0xF0101014);
        int innerLeft = x + 1;
        int innerTop = y + 1;
        int innerRight = x + width - 1;
        int innerBottom = y + height - 1;
        if (innerRight > innerLeft && innerBottom > innerTop) {
            context.enableScissor(innerLeft, innerTop, innerRight, innerBottom);
            int lineY = y + 4 - scroll;
            for (String line : lines) {
                if (lineY > innerBottom) {
                    break;
                }
                if (lineY + 12 > innerTop) {
                    int color = line.contains(BankBook.MISSING) || line.contains("暂无牌价") ? 0xFFAAAAAA : 0xFFE6E6E6;
                    if (line.equals(BankBook.CIRCULATION_NOTE)) {
                        color = 0xFFAAAAAA;
                    }
                    context.drawText(textRenderer, line, x + 4, lineY, color, false);
                }
                lineY += 12;
            }
            context.disableScissor();
        }
        context.fill(x, y, x + width, y + 1, 0xFFC8A060);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFC8A060);
        context.fill(x, y, x + 1, y + height, 0xFFC8A060);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFC8A060);
    }

    private void refreshAdjustLabels() {
        if (page != Page.MANAGEMENT_ADJUST) {
            return;
        }
        String name = adjustName();
        String qty = currentAmount();
        if (unbackedButton != null) {
            unbackedButton.setMessage(Text.literal(adjustLabel("无准备金增发", qty, name)));
        }
        if (retireButton != null) {
            retireButton.setMessage(Text.literal(adjustLabel("从流通中注销", qty, name)));
        }
        if (backedButton != null) {
            backedButton.setMessage(Text.literal("按准备金满额发行 " + name));
        }
    }

    private void drawRedeem(DrawContext context) {
        int y = contentTop() + 8;
        context.drawText(textRenderer, "兑付汇票", mainLeft(), y, 0xFFD0B060, false);
        drawWrapped(context, BankActions.MAIN_HAND_BILL, mainLeft(), y + 16, mainWidth(), 0xFFE6E6E6);
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
        if ("note".equals(item.role)) {
            BankSnapshot.NoteRow note = findNote(item.widget.selectedId());
            String quote = note == null || note.price() == null ? BankRates.UNAVAILABLE : "现价：" + BankHeader.figure(note.price());
            context.drawText(textRenderer, quote, item.x, item.y + item.height + 14, note == null || note.price() == null ? 0xFFAAAAAA : 0xFFE8D090, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Placed item : placed) {
            if (!item.widget.isMouseOver(mouseX, mouseY) || !item.widget.mouseClicked(mouseX, mouseY, button)) {
                continue;
            }
            if (item.widget.pollToggle()) {
                openDrawer = item.role.equals(openDrawer) ? "" : item.role;
                client.execute(this::clearAndInit);
                return true;
            }
            if (item.widget.pollPick()) {
                onList(item);
                if (item.widget.isDrawer()) {
                    openDrawer = "";
                    if (!"regime".equals(item.role)) {
                        client.execute(this::clearAndInit);
                    }
                }
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (Placed item : placed) {
            if (item.widget.mouseScrolled(mouseX, mouseY, amount)) {
                return true;
            }
        }
        if (page == Page.MANAGEMENT && mouseX >= bookX && mouseX < bookX + bookW && mouseY >= bookY && mouseY < bookY + bookH) {
            int lines = BankBook.visibleLedger(data).size();
            int maxScroll = Math.max(0, lines * 12 - (bookH - 8));
            bookScroll = Math.max(0, Math.min(maxScroll, bookScroll - (int) Math.round(amount * 12)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
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
        if (next != page) {
            openDrawer = "";
            bookScroll = 0;
        }
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
            case WITHDRAW -> {
                String id = selected("pay", payId);
                String refusal = BankFunds.withdrawalRefusal(data, id, currencyName(id), quantity);
                if (refusal != null) {
                    notice = refusal;
                } else {
                    sendIfPresent(id, () -> BankActions.withdraw(id, quantity));
                }
            }
            case EXCHANGE -> submitExchange(quantity);
            case ISSUE -> sendIfPresent(selected("pay", payId), () -> BankActions.check(selected("pay", payId), quantity, checkSelf()));
            case REDEEM -> send(BankActions.presentHeld());
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
            notice = "付出和得到不能是同一种。";
            return;
        }
        String refusal = BankFunds.physicalExchangeRefusal(data, pay, currencyName(pay), quantity);
        if (refusal != null) {
            notice = refusal;
            return;
        }
        String command = BankExchange.command(pay, receive, quantity);
        if (command == null) {
            notice = BankExchange.REFUSAL;
            return;
        }
        send(command);
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
        if (BankExchange.personal(id) || "note".equals(role) || "other".equals(role)) {
            String name = BankExchange.personal(id) ? BankExchange.noteName(id) : id;
            BankSnapshot.NoteRow note = findNote(name);
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
        if (page == Page.MANAGEMENT) {
            return false;
        }
        return "pay".equals(role) || "receive".equals(role) || "note".equals(role) || "other".equals(role);
    }

    private String marketHeading() {
        return switch (marketMode) {
            case "sell" -> "卖出个人银行券，只换成银行券，不动发行人的准备金。";
            case "swap" -> "把一张个人银行券换成另一张，不动发行人的准备金。";
            case "redeem" -> "通兑按该券的规则兑付发行人的准备金，不是同一账户里的兑换。";
            default -> "买入个人银行券，只用银行券支付，不动发行人的准备金。";
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

    private String currencyName(String id) {
        BankSnapshot.MoneyRow money = findMoney(BankSnapshot.currencies(data), id);
        return money == null ? "" : money.name();
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

    private int drawFitted(DrawContext context, String line, int x, int y, int maxWidth, int color, int limitY) {
        String rest = line == null ? "" : line;
        while (!rest.isEmpty() && y + 9 <= limitY) {
            String part = rest;
            while (textRenderer.getWidth(part) > maxWidth && part.length() > 1) {
                part = part.substring(0, part.offsetByCodePoints(0, part.codePointCount(0, part.length()) - 1));
            }
            context.drawText(textRenderer, part, x, y, color, false);
            y += 12;
            rest = rest.substring(part.length());
        }
        return y;
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
            case "开立汇票" -> Page.ISSUE;
            case "兑付汇票" -> Page.REDEEM;
            case "个人银行券交易" -> Page.MARKET;
            default -> Page.SAVINGS;
        };
    }

    private int controlsTop() {
        return BankLayout.contentBottom(this.height) - 64;
    }

    private int listFloor() {
        return controlsTop() - 16;
    }

    private int fittedTop(int desired) {
        int floor = listFloor();
        if (desired + 18 <= floor) {
            return desired;
        }
        return Math.max(contentTop(), floor - 18);
    }

    private int fittedHeight(int top) {
        return Math.max(18, listFloor() - top);
    }

    private int[] splitBand(int start, int preferredFirst) {
        int top = fittedTop(start);
        int floor = listFloor();
        int available = Math.max(40, floor - top);
        int first = Math.min(preferredFirst, Math.max(28, available / 2));
        int secondTop = top + first + 4;
        int second = floor - secondTop;
        if (second < 18) {
            second = 18;
            secondTop = Math.max(top + 22, floor - second);
            first = Math.max(18, secondTop - 4 - top);
        }
        return new int[] {top, first, secondTop, second};
    }

    private List<ChoiceListWidget.Choice> exchangeChoices() {
        List<ChoiceListWidget.Choice> choices = new ArrayList<>(moneyChoices(BankSnapshot.hardCurrencies(data)));
        for (BankSnapshot.NoteRow note : BankSnapshot.notes(data)) {
            String label = note.name();
            for (BankSnapshot.MoneyRow money : BankSnapshot.currencies(data)) {
                if (money.name().equals(note.name())) {
                    label = "个人银行券 " + note.name();
                    break;
                }
            }
            choices.add(new ChoiceListWidget.Choice(BankExchange.idForNote(note.name()), label));
        }
        return choices;
    }

    private void ensureExchangeIds(List<ChoiceListWidget.Choice> rows) {
        if (!containsChoice(rows, payId) && !rows.isEmpty()) {
            payId = rows.get(0).id();
        }
        if (!containsChoice(rows, receiveId) || receiveId.equals(payId)) {
            for (ChoiceListWidget.Choice choice : rows) {
                if (!choice.id().equals(payId)) {
                    receiveId = choice.id();
                    return;
                }
            }
        }
    }

    private static boolean containsChoice(List<ChoiceListWidget.Choice> rows, String id) {
        for (ChoiceListWidget.Choice choice : rows) {
            if (choice.id().equals(id)) {
                return true;
            }
        }
        return false;
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
