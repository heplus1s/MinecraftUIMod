package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator screen opened by /bank admin. It does not use the counter's tabs,
 * and Creative mode is not what puts a player here. The snapshot's admin flag is.
 */
public final class BankAdminScreen extends Screen {
    private JsonObject data;
    private String payId = "";
    private String amountText = "1";
    private String notice = "";
    private int bookScroll;
    private TextFieldWidget amount;
    private ChoiceListWidget currencies;
    private BankLayout.AdminBands bands;
    private boolean closing;

    public BankAdminScreen(JsonObject data) {
        super(Text.literal(BankChrome.MANAGEMENT));
        this.data = data == null ? new JsonObject() : data;
    }

    void show(JsonObject next) {
        if (amount != null && amount.getText() != null) {
            amountText = amount.getText();
        }
        if (currencies != null && currencies.selectedId() != null) {
            payId = currencies.selectedId();
        }
        this.data = next == null ? new JsonObject() : next;
        String reported = BankSnapshot.text(this.data, "refusal");
        if (reported != null) {
            notice = reported;
        }
        clearAndInit();
    }

    @Override
    protected void init() {
        bands = BankLayout.adminBands(this.height);
        List<BankSnapshot.MoneyRow> rows = BankSnapshot.currencies(data);
        if (find(rows, payId) == null) {
            payId = rows.isEmpty() ? "" : rows.get(0).id();
        }
        currencies = new ChoiceListWidget(client, BankLayout.EDGE, bands.listY(), this.width - BankLayout.EDGE * 2, bands.listH());
        currencies.setDrawer(false);
        currencies.setChoices(choices(rows));
        currencies.select(payId);
        addSelectableChild(currencies);

        int stepper = 22 + 4 + 88 + 4 + 22;
        int x = Math.max(BankLayout.EDGE, (this.width - stepper) / 2);
        addDrawableChild(ButtonWidget.builder(Text.literal("－"), button -> adjust(false))
                .dimensions(x, bands.stepperY(), 22, 18).build());
        amount = new TextFieldWidget(textRenderer, x + 26, bands.stepperY() + 1, 88, 16, Text.literal("数量"));
        amount.setMaxLength(32);
        amount.setText(amountText == null || amountText.isBlank() ? "1" : amountText);
        addDrawableChild(amount);
        addDrawableChild(ButtonWidget.builder(Text.literal("＋"), button -> adjust(true))
                .dimensions(x + 26 + 88 + 4, bands.stepperY(), 22, 18).build());

        List<String> extras = List.of(
                "无准备金增发",
                "从存款转入准备金",
                "从准备金转回存款",
                "按准备金满额发行",
                BankBook.freezeButton(data));
        int cursor = BankLayout.EDGE;
        int y = bands.extraY();
        for (String label : extras) {
            int width = Math.max(BankLayout.labelWidth(label), textRenderer.getWidth(label) + 12);
            if (cursor + width > this.width - BankLayout.EDGE) {
                cursor = BankLayout.EDGE;
                y += 22;
            }
            String action = label;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> extra(action))
                    .dimensions(cursor, y, width, 18).build());
            cursor += width + BankLayout.GAP;
        }
        int closeWidth = Math.max(BankLayout.labelWidth(BankChrome.CLOSE), textRenderer.getWidth(BankChrome.CLOSE) + 12);
        addDrawableChild(ButtonWidget.builder(Text.literal(BankChrome.CLOSE), button -> close())
                .dimensions(this.width - BankLayout.EDGE - closeWidth, BankLayout.closeY(this.height), closeWidth, BankLayout.BUTTON_HEIGHT)
                .build());
    }

    private void adjust(boolean issue) {
        remember();
        String currency = selected();
        if (currency.isBlank()) {
            notice = "请先在列表中选择一项。";
            return;
        }
        if (!typed(amountText)) {
            notice = "请在中间的格子里输入数量。点一次加或减，就按这个数量办理。";
            return;
        }
        notice = "";
        BankUiClient.send(issue
                ? BankActions.adminBackedAmount(currency, amountText.trim())
                : BankActions.adminRetire(currency, amountText.trim()));
    }

    private void extra(String label) {
        remember();
        if (label.equals(BankBook.PAUSE) || label.equals(BankBook.RESUME)) {
            notice = "";
            BankUiClient.send(BankActions.adminFreeze());
            return;
        }
        String currency = selected();
        if (currency.isBlank()) {
            notice = "请先在列表中选择一项。";
            return;
        }
        if ("按准备金满额发行".equals(label)) {
            notice = "";
            BankUiClient.send(BankActions.adminBacked(currency));
            return;
        }
        if (!typed(amountText)) {
            notice = "请在中间的格子里输入数量。";
            return;
        }
        notice = "";
        String quantity = amountText.trim();
        String command = switch (label) {
            case "无准备金增发" -> BankActions.adminUnbacked(currency, quantity);
            case "从存款转入准备金" -> BankActions.adminReserveFromDeposits(currency, quantity);
            case "从准备金转回存款" -> BankActions.adminReserveToDeposits(currency, quantity);
            default -> null;
        };
        if (command != null) {
            BankUiClient.send(command);
        }
    }

    private static boolean typed(String quantity) {
        if (quantity == null || quantity.isBlank() || "all".equalsIgnoreCase(quantity) || "全部".equals(quantity)) {
            return false;
        }
        return BankRates.parse(quantity) != null && BankRates.parse(quantity).signum() > 0;
    }

    private void remember() {
        if (amount != null && amount.getText() != null) {
            amountText = amount.getText();
        }
        if (currencies != null && currencies.selectedId() != null) {
            payId = currencies.selectedId();
        }
    }

    private String selected() {
        if (currencies != null && currencies.selectedId() != null && !currencies.selectedId().isBlank()) {
            return currencies.selectedId();
        }
        return payId == null ? "" : payId;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int x = BankLayout.EDGE;
        context.drawText(textRenderer, BankRates.centralQuote(data), x, bands.quoteY(), quoteColor(BankRates.centralQuote(data)), false);
        context.drawText(textRenderer, BankRates.bankNoteQuote(data), x, bands.quoteY() + 12, quoteColor(BankRates.bankNoteQuote(data)), false);
        drawBook(context);
        context.drawText(textRenderer, "选择要调整的货币", x, bands.listY() - 12, 0xFFE8D090, false);
        super.render(context, mouseX, mouseY, delta);
        if (currencies != null) {
            currencies.render(context, mouseX, mouseY, delta);
        }
        if (!notice.isBlank()) {
            context.drawText(textRenderer, notice, x, this.height - 12, 0xFFFFCC88, false);
        }
    }

    private void drawBook(DrawContext context) {
        int x = BankLayout.EDGE;
        int y = bands.bookY();
        int width = this.width - BankLayout.EDGE * 2;
        int height = bands.bookH();
        List<String> lines = BankBook.visibleLedger(data);
        int max = Math.max(0, lines.size() * 12 - (height - 8));
        if (bookScroll > max) {
            bookScroll = max;
        }
        context.fill(x, y, x + width, y + height, 0xFF101014);
        context.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        int lineY = y + 4 - bookScroll;
        for (String line : lines) {
            if (lineY > y + height) {
                break;
            }
            if (lineY + 12 > y) {
                int color = line.contains(BankBook.MISSING) || line.contains(BankRates.UNAVAILABLE) ? 0xFFAAAAAA : 0xFFE6E6E6;
                if (line.equals(BankBook.CIRCULATION_NOTE)) {
                    color = 0xFFAAAAAA;
                }
                context.drawText(textRenderer, line, x + 4, lineY, color, false);
            }
            lineY += 12;
        }
        context.disableScissor();
        frame(context, x, y, x + width, y + height);
    }

    private static int quoteColor(String line) {
        return line.contains(BankRates.UNAVAILABLE) ? 0xFFAAAAAA : 0xFFE8D090;
    }

    private static void frame(DrawContext context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, top + 1, 0xFFC8A060);
        context.fill(left, bottom - 1, right, bottom, 0xFFC8A060);
        context.fill(left, top, left + 1, bottom, 0xFFC8A060);
        context.fill(right - 1, top, right, bottom, 0xFFC8A060);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currencies != null && currencies.isMouseOver(mouseX, mouseY) && currencies.mouseClicked(mouseX, mouseY, button)) {
            if (currencies.selectedId() != null) {
                payId = currencies.selectedId();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (currencies != null && currencies.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        int x = BankLayout.EDGE;
        if (mouseX >= x && mouseX < this.width - BankLayout.EDGE && mouseY >= bands.bookY() && mouseY < bands.bookY() + bands.bookH()) {
            int lines = BankBook.visibleLedger(data).size();
            int max = Math.max(0, lines * 12 - (bands.bookH() - 8));
            bookScroll = Math.max(0, Math.min(max, bookScroll - (int) Math.round(amount * 12)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private static List<ChoiceListWidget.Choice> choices(List<BankSnapshot.MoneyRow> rows) {
        List<ChoiceListWidget.Choice> choices = new ArrayList<>();
        for (BankSnapshot.MoneyRow row : rows) {
            choices.add(new ChoiceListWidget.Choice(row.id(), row.name()));
        }
        return choices;
    }

    private static BankSnapshot.MoneyRow find(List<BankSnapshot.MoneyRow> rows, String id) {
        for (BankSnapshot.MoneyRow row : rows) {
            if (row.id().equals(id)) {
                return row;
            }
        }
        return null;
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
