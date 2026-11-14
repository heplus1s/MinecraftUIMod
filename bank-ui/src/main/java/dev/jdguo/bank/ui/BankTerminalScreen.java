package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * Optional bank screen. Quantity controls sit on the same page as the currency choices.
 * Function tabs are two left-aligned rows. Issuance and management are top-right.
 * Close is alone at the bottom right. There is no sidebar.
 */
public final class BankTerminalScreen extends Screen {
    private enum Page {
        SAVINGS, DEPOSIT, WITHDRAW, EXCHANGE, CHECKS, MARKET, ISSUANCE, MANAGEMENT, CONFIRM
    }

    private JsonObject data;
    private Page page = Page.SAVINGS;
    private String payId = "";
    private String receiveId = "";
    private String noteName = "";
    private String otherNote = "";
    private String marketMode = "buy";
    private boolean checkSelf;
    private String amountText = "1";
    private String notice = "";
    private boolean closing;
    private TextFieldWidget amount;
    private ChoiceListWidget noteList;
    private ChoiceListWidget otherList;
    private int contentTop = 80;

    public BankTerminalScreen(JsonObject data) {
        super(Text.literal(BankChrome.SAVINGS_TITLE));
        this.data = data == null ? new JsonObject() : data;
    }

    void show(JsonObject data) {
        boolean confirm = BankSnapshot.confirm(data);
        this.data = data == null ? new JsonObject() : data;
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
        noteList = null;
        otherList = null;
        boolean admin = BankSnapshot.admin(data);
        contentTop = BankLayout.contentTop(BankHeader.lines(data).size(), admin);
        if (page != Page.CONFIRM) {
            addChrome(admin);
        } else {
            addClose();
        }
        switch (page) {
            case DEPOSIT, WITHDRAW -> initTransfer();
            case EXCHANGE -> initExchange();
            case CHECKS -> initChecks();
            case MARKET -> initMarket();
            case ISSUANCE -> initIssuance();
            case MANAGEMENT -> initManagement();
            case CONFIRM -> initConfirm();
            default -> {
            }
        }
    }

    private void addChrome(boolean admin) {
        for (BankLayout.Box box : BankLayout.functionTabs(this.width, this.height)) {
            addDrawableChild(ButtonWidget.builder(Text.literal(box.label()), button -> open(pageFor(box.label())))
                    .dimensions(box.x(), box.y(), box.width(), box.height()).build());
        }
        BankLayout.Box issue = BankLayout.issuanceBox(this.width);
        addDrawableChild(ButtonWidget.builder(Text.literal(issue.label()), button -> open(Page.ISSUANCE))
                .dimensions(issue.x(), issue.y(), issue.width(), issue.height()).build());
        if (admin) {
            BankLayout.Box manage = BankLayout.managementBox(this.width);
            addDrawableChild(ButtonWidget.builder(Text.literal(manage.label()), button -> open(Page.MANAGEMENT))
                    .dimensions(manage.x(), manage.y(), manage.width(), manage.height()).build());
        }
        addClose();
    }

    private void addClose() {
        BankLayout.Box close = BankLayout.closeBox(this.width, this.height);
        addDrawableChild(ButtonWidget.builder(Text.literal(close.label()), button -> close())
                .dimensions(close.x(), close.y(), close.width(), close.height()).build());
    }

    private void initTransfer() {
        ensurePay();
        int right = this.width - BankLayout.EDGE;
        int y = contentTop + 28;
        y = addMoneyButtons(BankSnapshot.currencies(data), payId, id -> payId = id, BankLayout.EDGE, y, right, fieldTop() - 4);
        addAmountBlock(y, true, page == Page.DEPOSIT ? "划拨存入" : "划拨取出");
    }

    private void initExchange() {
        ensurePay();
        ensureReceive();
        int right = this.width - BankLayout.EDGE;
        int y = contentTop + 28;
        int limit = fieldTop() - 28;
        y = addMoneyButtons(BankSnapshot.currencies(data), payId, id -> payId = id, BankLayout.EDGE, y, right, limit);
        y = addMoneyButtons(BankSnapshot.currencies(data), receiveId, id -> receiveId = id, BankLayout.EDGE, y + 12, right, limit);
        if (y + 20 < fieldTop()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("对调"), button -> swapCurrencies())
                    .dimensions(BankLayout.EDGE, y, 52, 18).build());
        }
        addAmountBlock(y, false, "兑换");
    }

    private void initChecks() {
        ensurePay();
        int right = this.width - BankLayout.EDGE;
        int y = contentTop + 28;
        y = addMoneyButtons(BankSnapshot.currencies(data), payId, id -> payId = id, BankLayout.EDGE, y, right, fieldTop() - 26);
        int selfX = BankLayout.EDGE;
        addDrawableChild(toggle("仅本人兑付", checkSelf, selfX, Math.min(y, fieldTop() - 24), () -> checkSelf = true));
        addDrawableChild(toggle("持票人兑付", !checkSelf, selfX + 100, Math.min(y, fieldTop() - 24), () -> checkSelf = false));
        addAmountBlock(y, false, "开出汇票");
        String present = "兑付背包里的汇票";
        int presentWidth = textRenderer.getWidth(present) + 16;
        int presentX = this.width - BankLayout.EDGE - presentWidth;
        int issueRight = BankLayout.EDGE + textRenderer.getWidth("开出汇票") + 24;
        int presentY = BankLayout.contentBottom(this.height) - 20;
        if (presentX < issueRight) {
            presentX = BankLayout.EDGE;
            presentY = fieldTop() - 22;
        }
        if (presentY > contentTop + 48) {
            addDrawableChild(ButtonWidget.builder(Text.literal(present), button -> send(BankActions.presentHeld()))
                    .dimensions(presentX, presentY, presentWidth, 18).build());
        }
    }

    private void initMarket() {
        ensureNote();
        int y = contentTop + 28;
        int x = BankLayout.EDGE;
        x = addMode(x, y, "buy", "买入");
        x = addMode(x, y, "sell", "卖出");
        x = addMode(x, y, "swap", "互换");
        addMode(x, y, "redeem", "通兑");
        int listTop = y + 24;
        int listBottom = fieldTop() - 4;
        if ("redeem".equals(marketMode)) {
            if (currency(receiveId) == null) {
                receiveId = currencyId("银行券", "note");
            }
            if (receiveId.isBlank() && !BankSnapshot.currencies(data).isEmpty()) {
                receiveId = BankSnapshot.currencies(data).get(0).id();
            }
            listBottom -= 24;
            addMoneyButtons(BankSnapshot.currencies(data), receiveId, id -> receiveId = id,
                    BankLayout.EDGE, listBottom, this.width - BankLayout.EDGE, fieldTop() - 2);
        }
        List<BankSnapshot.NoteRow> notes = BankSnapshot.notes(data);
        if (listBottom - listTop >= 36 && notes.size() > 6) {
            noteList = list(BankLayout.EDGE, listTop, Math.min(220, this.width / 2 - 12), listBottom - listTop, noteChoices(notes), noteName);
            if ("swap".equals(marketMode)) {
                otherList = list(this.width / 2 + 4, listTop, Math.min(220, this.width / 2 - 16), listBottom - listTop, noteChoices(notes), otherNote);
            }
        } else {
            int row = listTop;
            row = addNoteButtons(notes, noteName, id -> noteName = id, BankLayout.EDGE, row, this.width - BankLayout.EDGE, listBottom);
            if ("swap".equals(marketMode)) {
                addNoteButtons(notes, otherNote, id -> otherNote = id, BankLayout.EDGE, row + 12, this.width - BankLayout.EDGE, listBottom);
            }
        }
        addAmountBlock(listTop, false, marketVerb());
    }

    private void initIssuance() {
        if (!BankSnapshot.hasPersonal(data)) {
            int y = contentTop + 48;
            int x = BankLayout.EDGE;
            addDrawableChild(regimeButton("浮动", "float", x, y));
            addDrawableChild(regimeButton("钉住", "peg", x + 80, y));
            addDrawableChild(regimeButton("定额兑付", "hard", x + 160, y));
            return;
        }
        if (BankSnapshot.bankrupt(data)) {
            return;
        }
        int actionY = BankLayout.contentBottom(this.height) - 20;
        addAmountBlock(contentTop + 70, false, null);
        int unbackedWidth = textRenderer.getWidth("无准备金发行") + 16;
        int issueWidth = textRenderer.getWidth("按准备金发行") + 16;
        int unbackedX = this.width - BankLayout.EDGE - unbackedWidth;
        int issueX = unbackedX - 4 - issueWidth;
        if (issueX >= BankLayout.EDGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("按准备金发行"), button -> sendAmount(BankActions::noteIssue))
                    .dimensions(issueX, actionY, issueWidth, 18).build());
        } else if (contentTop + 66 < fieldTop()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("按准备金发行"), button -> sendAmount(BankActions::noteIssue))
                    .dimensions(BankLayout.EDGE, contentTop + 48, issueWidth, 18).build());
        }
        if (unbackedX >= BankLayout.EDGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("无准备金发行"), button -> sendAmount(BankActions::noteUnbacked))
                    .dimensions(unbackedX, actionY, unbackedWidth, 18).build());
        }
        int reserveY = contentTop + 64;
        int reserveX = BankLayout.EDGE + 100;
        if (reserveY + 18 < fieldTop() && reserveX + 120 <= this.width - BankLayout.EDGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("存入央行准备金"), button -> sendAmount(BankActions::reserveDeposit))
                    .dimensions(reserveX, reserveY, 120, 18).build());
            int withdrawX = reserveX + 124;
            int withdrawY = reserveY;
            if (withdrawX + 120 > this.width - BankLayout.EDGE) {
                withdrawX = reserveX;
                withdrawY = reserveY + 22;
            }
            if (withdrawY + 18 < fieldTop()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("取出央行准备金"), button -> sendAmount(BankActions::reserveWithdraw))
                        .dimensions(withdrawX, withdrawY, 120, 18).build());
            }
        }
        int bankruptWidth = textRenderer.getWidth("宣告破产") + 16;
        addDrawableChild(ButtonWidget.builder(Text.literal("宣告破产"), button -> {
            send(BankActions.noteBankrupt());
            page = Page.CONFIRM;
            client.execute(this::clearAndInit);
        }).dimensions(BankLayout.EDGE, contentTop + 28, bankruptWidth, 18).build());
    }

    private void initManagement() {
        if (!BankSnapshot.admin(data)) {
            page = Page.SAVINGS;
            return;
        }
        ensurePay();
        int y = Math.min(contentTop + 28 + linesOfBooks() * 12, fieldTop() - 28);
        addMoneyButtons(BankSnapshot.currencies(data), payId, id -> payId = id, BankLayout.EDGE, y, this.width - BankLayout.EDGE, fieldTop() - 4);
        addAmountBlock(y, false, null);
        int actionY = BankLayout.contentBottom(this.height) - 20;
        int x = BankLayout.EDGE;
        addDrawableChild(ButtonWidget.builder(Text.literal("暂停或恢复兑付"), button -> send(BankActions.adminFreeze()))
                .dimensions(x, actionY, 120, 18).build());
        x += 124;
        if (x + 110 <= this.width - BankLayout.EDGE) {
            int unbackedX = x;
            addDrawableChild(ButtonWidget.builder(Text.literal("无准备金增发"), button -> sendAmount(qty -> BankActions.adminUnbacked(payId, qty)))
                    .dimensions(unbackedX, actionY, 110, 18).build());
            x += 114;
        }
        if (x + 52 <= this.width - BankLayout.EDGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("注销"), button -> sendAmount(qty -> BankActions.adminRetire(payId, qty)))
                    .dimensions(x, actionY, 52, 18).build());
        }
    }

    private void initConfirm() {
        addDrawableChild(ButtonWidget.builder(Text.literal("返回"), button -> {
            send(BankActions.noteBankruptNo());
            page = Page.ISSUANCE;
            client.execute(this::clearAndInit);
        }).dimensions(this.width / 2 - 150, this.height / 2, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("确认宣告破产"), button -> send(BankActions.noteBankruptYes()))
                .dimensions(this.width / 2 - 60, this.height / 2, 160, 20).build());
    }

    private void addAmountBlock(int unusedTop, boolean includeAll, String verb) {
        int actionY = BankLayout.contentBottom(this.height) - 20;
        int quickY = actionY - 22;
        int fieldY = quickY - 20;
        if (fieldY < contentTop + 36) {
            fieldY = actionY - 22;
            quickY = -1;
        }
        amount = new TextFieldWidget(textRenderer, BankLayout.EDGE, fieldY, 90, 16, Text.literal("数量"));
        amount.setMaxLength(32);
        amount.setText(amountText == null || amountText.isBlank() ? "1" : amountText);
        addDrawableChild(amount);
        if (quickY > 0) {
            int x = BankLayout.EDGE + 96;
            for (String label : new String[] {"1", "5", "10", "64"}) {
                String shown = label;
                addDrawableChild(ButtonWidget.builder(Text.literal(shown), button -> quick(shown))
                        .dimensions(x, quickY, 28, 18).build());
                x += 30;
            }
            if (includeAll) {
                addDrawableChild(ButtonWidget.builder(Text.literal("全部"), button -> quick("全部"))
                        .dimensions(x, quickY, 48, 18).build());
            }
        }
        if (verb != null) {
            int width = Math.max(72, textRenderer.getWidth(verb) + 16);
            addDrawableChild(ButtonWidget.builder(Text.literal(verb), button -> submit())
                    .dimensions(BankLayout.EDGE, actionY, width, 18).build());
        }
    }

    private int fieldTop() {
        return BankLayout.contentBottom(this.height) - 62;
    }

    private int addMoneyButtons(List<BankSnapshot.MoneyRow> rows, String selected, Consumer<String> pick, int x, int y, int right, int maxY) {
        int cursor = x;
        int rowY = y;
        for (BankSnapshot.MoneyRow row : rows) {
            if (rowY + 18 > maxY) {
                break;
            }
            String shown = row.name() + "  " + row.amount();
            if (row.id().equals(selected)) {
                shown = "【" + shown + "】";
            }
            int width = Math.max(52, textRenderer.getWidth(shown) + 12);
            if (cursor + width > right && cursor > x) {
                cursor = x;
                rowY += 22;
                if (rowY + 18 > maxY) {
                    break;
                }
            }
            String id = row.id();
            addDrawableChild(ButtonWidget.builder(Text.literal(shown), button -> {
                rememberAmount();
                pick.accept(id);
                client.execute(this::clearAndInit);
            }).dimensions(cursor, rowY, width, 18).build());
            cursor += width + 4;
        }
        return rowY + 22;
    }

    private int addNoteButtons(List<BankSnapshot.NoteRow> notes, String selected, Consumer<String> pick, int x, int y, int right, int maxY) {
        int cursor = x;
        int rowY = y;
        for (BankSnapshot.NoteRow note : notes) {
            if (rowY + 18 > maxY) {
                break;
            }
            String shown = note.name().equals(selected) ? "【" + note.name() + "】" : note.name();
            int width = Math.max(52, textRenderer.getWidth(shown) + 12);
            if (cursor + width > right && cursor > x) {
                cursor = x;
                rowY += 22;
                if (rowY + 18 > maxY) {
                    break;
                }
            }
            String name = note.name();
            addDrawableChild(ButtonWidget.builder(Text.literal(shown), button -> {
                rememberAmount();
                pick.accept(name);
                client.execute(this::clearAndInit);
            }).dimensions(cursor, rowY, width, 18).build());
            cursor += width + 4;
        }
        return rowY + 22;
    }

    private ButtonWidget toggle(String label, boolean selected, int x, int y, Runnable pick) {
        String shown = selected ? "【" + label + "】" : label;
        int width = Math.max(90, textRenderer.getWidth(shown) + 12);
        return ButtonWidget.builder(Text.literal(shown), button -> {
            rememberAmount();
            pick.run();
            client.execute(this::clearAndInit);
        }).dimensions(x, y, width, 18).build();
    }

    private ButtonWidget regimeButton(String label, String regime, int x, int y) {
        int width = Math.max(72, textRenderer.getWidth(label) + 16);
        return ButtonWidget.builder(Text.literal(label), button -> {
            if (client != null) {
                client.setScreen(new ChatScreen(BankActions.noteNameCommand(regime)));
            }
        }).dimensions(x, y, width, 20).build();
    }

    private int addMode(int x, int y, String id, String label) {
        String shown = id.equals(marketMode) ? "【" + label + "】" : label;
        int width = Math.max(52, textRenderer.getWidth(shown) + 12);
        addDrawableChild(ButtonWidget.builder(Text.literal(shown), button -> {
            rememberAmount();
            marketMode = id;
            if ("redeem".equals(id)) {
                String payout = currencyId("银行券", "note");
                if (!payout.isBlank()) {
                    receiveId = payout;
                }
            }
            client.execute(this::clearAndInit);
        }).dimensions(x, y, width, 18).build());
        return x + width + 4;
    }

    private ChoiceListWidget list(int x, int y, int width, int height, List<ChoiceListWidget.Choice> choices, String selected) {
        ChoiceListWidget widget = new ChoiceListWidget(client, x, y, width, height);
        widget.setChoices(choices);
        widget.select(selected);
        addSelectableChild(widget);
        return widget;
    }

    private List<ChoiceListWidget.Choice> noteChoices(List<BankSnapshot.NoteRow> notes) {
        return notes.stream()
                .map(note -> new ChoiceListWidget.Choice(note.name(), note.name() + "  " + note.balance()))
                .toList();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        drawHeader(context);
        drawPage(context);
        super.render(context, mouseX, mouseY, delta);
        if (noteList != null) {
            noteList.render(context, mouseX, mouseY, delta);
        }
        if (otherList != null) {
            otherList.render(context, mouseX, mouseY, delta);
        }
    }

    private void drawHeader(DrawContext context) {
        int x = BankLayout.EDGE;
        int y = BankLayout.EDGE;
        int max = Math.max(48, BankLayout.headerRight(this.width, BankSnapshot.admin(data)) - x);
        String status = BankSnapshot.text(data, "status");
        for (String line : BankHeader.lines(data)) {
            int color = 0xFFE6E6E6;
            if (line.startsWith("汇率")) {
                color = 0xFFE8D090;
            } else if (status != null && line.equals(status)) {
                color = statusColor(status);
            }
            y = drawWrapped(context, line, x, y, max, color);
        }
    }

    private void drawPage(DrawContext context) {
        int x = BankLayout.EDGE;
        int y = contentTop;
        switch (page) {
            case SAVINGS -> drawSavings(context, x, y);
            case DEPOSIT -> drawPrompt(context, x, y, "存入", "请选择要存入的货币，再填写数量。");
            case WITHDRAW -> drawPrompt(context, x, y, "取出", "请选择要取出的货币，再填写数量。");
            case EXCHANGE -> drawPrompt(context, x, y, "兑换", "先选付出，再选得到。对调会交换这两种货币。");
            case CHECKS -> {
                context.drawText(textRenderer, "汇票", x, y, 0xFFD0B060, false);
                context.drawText(textRenderer, "选择币种和抬头，再填写开出的数量。", x, y + 14, 0xFFAAAAAA, false);
                drawWrapped(context, "兑付背包里的汇票时，背包中的铜锭、钻石和银行券也会一并存入。", x, y + 28, this.width - 16, 0xFF888888);
            }
            case MARKET -> {
                context.drawText(textRenderer, "交易", x, y, 0xFFD0B060, false);
                context.drawText(textRenderer, "买入和卖出按银行券结算。", x, y + 14, 0xFFAAAAAA, false);
            }
            case ISSUANCE -> drawIssuance(context, x, y);
            case MANAGEMENT -> drawManagement(context, x, y);
            case CONFIRM -> context.drawText(textRenderer, "确认后才会作废「" + safe(BankSnapshot.text(data, "personal")) + "」。返回则账目不变。", x, this.height / 2 - 20, 0xFFFF7777, false);
            default -> {
            }
        }
        if (!notice.isBlank() && page != Page.CONFIRM) {
            context.drawText(textRenderer, notice, this.width / 2, contentTop, 0xFFFFCC88, false);
        }
    }

    private void drawSavings(DrawContext context, int x, int y) {
        context.drawText(textRenderer, BankChrome.SAVINGS_TITLE, x, y, 0xFFD0B060, false);
        context.drawText(textRenderer, "这是你自己的存款。", x, y + 14, 0xFFAAAAAA, false);
        int line = y + 32;
        List<BankSnapshot.MoneyRow> currencies = BankSnapshot.currencies(data);
        if (currencies.isEmpty()) {
            context.drawText(textRenderer, "柜台还没有发来货币余额。", x, line, 0xFFE6E6E6, false);
            line += 12;
        }
        for (BankSnapshot.MoneyRow row : currencies) {
            context.drawText(textRenderer, row.name() + "  " + row.amount(), x, line, 0xFFE6E6E6, false);
            line += 12;
        }
        line += 6;
        context.drawText(textRenderer, "持有的个人银行券", x, line, 0xFFD0B060, false);
        line += 14;
        List<BankSnapshot.NoteRow> held = BankSnapshot.heldNotes(data);
        if (held.isEmpty()) {
            context.drawText(textRenderer, "没有持有个人银行券。", x, line, 0xFFAAAAAA, false);
            return;
        }
        for (BankSnapshot.NoteRow note : held) {
            String text = note.name() + "  账面余额 " + note.balance();
            if (note.price() != null) {
                text += "  现价 " + note.price();
            }
            line = drawWrapped(context, text, x, line, this.width - 16, 0xFFE6E6E6);
        }
    }

    private void drawPrompt(DrawContext context, int x, int y, String title, String copy) {
        context.drawText(textRenderer, title, x, y, 0xFFD0B060, false);
        context.drawText(textRenderer, copy, x, y + 14, 0xFFAAAAAA, false);
    }

    private void drawIssuance(DrawContext context, int x, int y) {
        context.drawText(textRenderer, "个人发行", x, y, 0xFFD0B060, false);
        if (!BankSnapshot.hasPersonal(data)) {
            drawWrapped(context, "还没有记名。点选浮动、钉住或定额兑付后，在聊天框里补上名字再回车。名字最多三个汉字或英文字母。", x, y + 16, this.width - 16, 0xFFE6E6E6);
            return;
        }
        String name = BankSnapshot.text(data, "personal");
        context.drawText(textRenderer, "「" + name + "」", x, y + 14, 0xFFE8D090, false);
        int line = y + 28;
        String regime = BankSnapshot.regimeLabel(BankSnapshot.text(data, "regime"));
        if (regime != null) {
            context.drawText(textRenderer, "方式  " + regime, x + 100, line, 0xFFE6E6E6, false);
            line += 12;
        }
        BankSnapshot.NoteRow own = ownNote(name);
        if (own != null) {
            context.drawText(textRenderer, "持有  " + own.balance(), x + 100, line, 0xFFE6E6E6, false);
            line += 12;
            if (own.price() != null) {
                context.drawText(textRenderer, "现价  " + own.price(), x + 100, line, 0xFFE6E6E6, false);
                line += 12;
            }
        }
        String circulation = BankSnapshot.text(data, "circulation");
        if (circulation != null) {
            context.drawText(textRenderer, "流通量  " + circulation, x + 100, line, 0xFFE6E6E6, false);
            line += 12;
        }
        String reserve = BankSnapshot.text(data, "reserve");
        String required = BankSnapshot.text(data, "required");
        if (reserve != null || required != null) {
            context.drawText(textRenderer, "央行准备金  " + safe(reserve) + " / " + safe(required), x + 100, line, 0xFFE6E6E6, false);
        }
        if (BankSnapshot.bankrupt(data)) {
            context.drawText(textRenderer, "这张个人银行券已经破产。", x, y + 32, 0xFFFF7777, false);
        }
    }

    private void drawManagement(DrawContext context, int x, int y) {
        context.drawText(textRenderer, BankChrome.MANAGEMENT, x, y, 0xFFD0B060, false);
        int line = y + 16;
        int limit = BankLayout.contentBottom(this.height) - 78;
        List<BankSnapshot.CentralRow> central = BankSnapshot.central(data);
        if (central.isEmpty() && !BankSnapshot.has(data, "issuanceDiamond") && !BankSnapshot.has(data, "vaultDiamond")) {
            context.drawText(textRenderer, "发行准备金和储蓄保管不在当前柜台数据里。", x, line, 0xFFAAAAAA, false);
            line += 12;
        }
        for (BankSnapshot.CentralRow row : central) {
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
            line = drawWrapped(context, text.toString(), x, line, this.width - 16, 0xFFE6E6E6);
        }
        if (central.isEmpty()) {
            if (BankSnapshot.text(data, "issuanceDiamond") != null && line <= limit) {
                line = drawWrapped(context, "钻石发行准备金  " + BankSnapshot.text(data, "issuanceDiamond"), x, line, this.width - 16, 0xFFE6E6E6);
            }
            if (BankSnapshot.text(data, "vaultDiamond") != null && line <= limit) {
                line = drawWrapped(context, "钻石储蓄保管  " + BankSnapshot.text(data, "vaultDiamond"), x, line, this.width - 16, 0xFFE6E6E6);
            }
        }
        if (!BankSnapshot.has(data, "institutions")) {
            if (line <= limit) {
                line = drawWrapped(context, "商店经营账户和土地出让账户不在当前柜台数据里。", x, line, this.width - 16, 0xFFAAAAAA);
            }
        } else {
            for (BankSnapshot.InstitutionRow row : BankSnapshot.institutions(data)) {
                if (line > limit) {
                    break;
                }
                line = drawWrapped(context, row.name() + "  " + safe(row.detail()), x, line, this.width - 16, 0xFFE6E6E6);
            }
        }
        if (!BankSnapshot.has(data, "interest") && !BankSnapshot.has(data, "issuable") && line <= limit) {
            drawWrapped(context, "年利率和尚可发行的数量不在当前柜台数据里。", x, line, this.width - 16, 0xFF888888);
        }
    }

    private int linesOfBooks() {
        int lines = BankSnapshot.central(data).size();
        if (BankSnapshot.has(data, "institutions")) {
            lines += BankSnapshot.institutions(data).size();
        } else {
            lines += 2;
        }
        return Math.max(1, lines);
    }

    private int drawWrapped(DrawContext context, String line, int x, int y, int maxWidth, int color) {
        if (line == null || line.isEmpty()) {
            return y + 12;
        }
        String rest = line;
        while (!rest.isEmpty()) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickList(noteList, mouseX, mouseY, button, true)) {
            return true;
        }
        if (clickList(otherList, mouseX, mouseY, button, false)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickList(ChoiceListWidget list, double mouseX, double mouseY, int button, boolean primary) {
        if (list == null || !list.isMouseOver(mouseX, mouseY) || !list.mouseClicked(mouseX, mouseY, button)) {
            return false;
        }
        if (list.selectedId() != null) {
            if (primary) {
                noteName = list.selectedId();
            } else {
                otherNote = list.selectedId();
            }
        }
        return true;
    }

    private void open(Page next) {
        rememberAmount();
        page = next;
        notice = "";
        client.execute(this::clearAndInit);
    }

    private void swapCurrencies() {
        rememberAmount();
        String held = payId;
        payId = receiveId;
        receiveId = held;
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
        rememberAmount();
        String quantity = QuantityTokens.token(amountText);
        switch (page) {
            case DEPOSIT -> sendIfCurrency(payId, () -> BankActions.deposit(payId, quantity));
            case WITHDRAW -> sendIfCurrency(payId, () -> BankActions.withdraw(payId, quantity));
            case EXCHANGE -> {
                if (payId.isBlank() || receiveId.isBlank()) {
                    notice = "请选择付出和得到的货币。";
                    return;
                }
                if (payId.equals(receiveId)) {
                    notice = "付出和得到不能是同一种货币。";
                    return;
                }
                send(BankActions.exchange(payId, receiveId, quantity));
            }
            case CHECKS -> sendIfCurrency(payId, () -> BankActions.check(payId, quantity, checkSelf));
            case MARKET -> submitMarket(quantity);
            default -> {
            }
        }
    }

    private void submitMarket(String quantity) {
        if (noteName.isBlank()) {
            notice = "请选择一张个人银行券。";
            return;
        }
        switch (marketMode) {
            case "sell" -> send(BankActions.noteSell(noteName, quantity));
            case "swap" -> {
                if (otherNote.isBlank() || otherNote.equals(noteName)) {
                    notice = "请再选择一张要换成的个人银行券。";
                    return;
                }
                send(BankActions.noteSwap(noteName, otherNote, quantity));
            }
            case "redeem" -> {
                if (receiveId.isBlank()) {
                    notice = "请选择兑成的货币。";
                    return;
                }
                send(BankActions.noteRedeem(noteName, receiveId, quantity));
            }
            default -> send(BankActions.noteBuy(noteName, quantity));
        }
    }

    private void sendIfCurrency(String id, java.util.function.Supplier<String> action) {
        if (id == null || id.isBlank()) {
            notice = "请先选择一种货币。";
            return;
        }
        send(action.get());
    }

    private void sendAmount(java.util.function.Function<String, String> action) {
        rememberAmount();
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

    private void rememberAmount() {
        if (amount != null) {
            amountText = amount.getText();
        }
    }

    private void ensurePay() {
        if (!payId.isBlank() && currency(payId) != null) {
            return;
        }
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        payId = rows.isEmpty() ? "" : rows.get(0).id();
    }

    private void ensureReceive() {
        if (!receiveId.isBlank() && currency(receiveId) != null && !receiveId.equals(payId)) {
            return;
        }
        String preferred = currencyId("银行券", "note");
        if (!preferred.isBlank() && !preferred.equals(payId)) {
            receiveId = preferred;
            return;
        }
        for (BankSnapshot.MoneyRow row : BankSnapshot.currencies(data)) {
            if (!row.id().equals(payId)) {
                receiveId = row.id();
                return;
            }
        }
        receiveId = payId;
    }

    private void ensureNote() {
        List<BankSnapshot.NoteRow> notes = BankSnapshot.notes(data);
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
                    break;
                }
            }
        }
    }

    private BankSnapshot.MoneyRow currency(String id) {
        for (BankSnapshot.MoneyRow row : BankSnapshot.currencies(data)) {
            if (row.id().equals(id)) {
                return row;
            }
        }
        return null;
    }

    private String currencyId(String name, String fallback) {
        for (BankSnapshot.MoneyRow row : BankSnapshot.currencies(data)) {
            if (name.equals(row.name()) || fallback.equals(row.id())) {
                return row.id();
            }
        }
        return "";
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

    private BankSnapshot.NoteRow ownNote(String name) {
        return findNote(name);
    }

    private String marketVerb() {
        return switch (marketMode) {
            case "sell" -> "卖出";
            case "swap" -> "互换";
            case "redeem" -> "通兑";
            default -> "买入";
        };
    }

    private static Page pageFor(String label) {
        return switch (label) {
            case "存入" -> Page.DEPOSIT;
            case "取出" -> Page.WITHDRAW;
            case "兑换" -> Page.EXCHANGE;
            case "汇票" -> Page.CHECKS;
            case "交易" -> Page.MARKET;
            default -> Page.SAVINGS;
        };
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
