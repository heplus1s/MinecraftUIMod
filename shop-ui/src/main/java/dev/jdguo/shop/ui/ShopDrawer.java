package dev.jdguo.shop.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Collapsed option drawer. Open rows scroll inside the frame and cannot paint past it. */
final class ShopDrawer extends AlwaysSelectedEntryListWidget<ShopDrawer.Row> {
    static final int HEADER = 20;

    private boolean expanded;
    private int scroll;
    private boolean toggle;
    private boolean picked;

    ShopDrawer(MinecraftClient client, int x, int y, int width, int height) {
        super(client, width, height, y, y + height, 18);
        setLeftPos(x);
        setRenderBackground(false);
        setRenderHorizontalShadows(false);
        setRenderSelection(false);
    }

    void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    boolean pollToggle() {
        boolean value = toggle;
        toggle = false;
        return value;
    }

    boolean pollPick() {
        boolean value = picked;
        picked = false;
        return value;
    }

    void setChoices(List<Choice> choices, String selected) {
        clearEntries();
        for (Choice choice : choices) {
            addEntry(new Row(choice));
        }
        select(selected);
    }

    void select(String id) {
        if (id == null) {
            return;
        }
        for (Row row : children()) {
            if (id.equals(row.choice.id)) {
                setSelected(row);
                return;
            }
        }
    }

    String selectedId() {
        Row row = getSelectedOrNull();
        return row == null ? ShopQuotes.NOTE : row.choice.id;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int bottom = expanded ? this.bottom : this.top + HEADER;
        frame(context, this.left, this.top, this.right, bottom);
        String label = getSelectedOrNull() == null ? "银行券" : getSelectedOrNull().choice.label;
        context.drawText(client.textRenderer, label, this.left + 6, this.top + 6, 0xFFE8E8E8, false);
        String cue = expanded ? "收起" : "展开";
        context.drawText(client.textRenderer, cue, this.right - client.textRenderer.getWidth(cue) - 6, this.top + 6, 0xFFE8D090, false);
        if (!expanded) {
            return;
        }
        int innerLeft = this.left + 1;
        int innerTop = this.top + HEADER;
        int innerRight = this.right - 1;
        int innerBottom = this.bottom - 1;
        if (innerBottom <= innerTop) {
            return;
        }
        context.enableScissor(innerLeft, innerTop, innerRight, innerBottom);
        int index = 0;
        for (Row row : children()) {
            int rowY = innerTop + index * this.itemHeight - scroll;
            if (rowY + this.itemHeight > innerTop && rowY < innerBottom) {
                boolean hovered = mouseX >= innerLeft && mouseX < innerRight && mouseY >= rowY && mouseY < rowY + this.itemHeight;
                row.render(context, index, rowY, this.left + 4, this.width - 16, this.itemHeight, mouseX, mouseY, hovered, delta);
            }
            index++;
        }
        context.disableScissor();
        frame(context, this.left, this.top, this.right, this.bottom);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < this.left || mouseX >= this.right || mouseY < this.top || mouseY >= this.bottom) {
            return false;
        }
        if (mouseY < this.top + HEADER) {
            toggle = true;
            return true;
        }
        if (!expanded) {
            return true;
        }
        int innerTop = this.top + HEADER;
        int index = 0;
        for (Row row : children()) {
            int rowY = innerTop + index * this.itemHeight - scroll;
            if (mouseY >= rowY && mouseY < rowY + this.itemHeight && rowY < this.bottom) {
                setSelected(row);
                picked = true;
                return true;
            }
            index++;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int view = Math.max(0, (this.bottom - 1) - (this.top + HEADER));
        int max = Math.max(0, children().size() * this.itemHeight - view);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.round(amount * this.itemHeight)));
        return true;
    }

    private static void frame(DrawContext context, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, 0xF0101014);
        context.fill(left, top, right, top + 1, 0xFFC8A060);
        context.fill(left, bottom - 1, right, bottom, 0xFFC8A060);
        context.fill(left, top, left + 1, bottom, 0xFFC8A060);
        context.fill(right - 1, top, right, bottom, 0xFFC8A060);
    }

    record Choice(String id, String label) {
    }

    final class Row extends AlwaysSelectedEntryListWidget.Entry<Row> {
        private final Choice choice;

        Row(Choice choice) {
            this.choice = choice;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (getSelectedOrNull() == this || hovered) {
                context.fill(x, y, x + entryWidth, y + entryHeight - 1, 0xAA3A5A38);
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, choice.label, x + 4, y + 4, 0xFFE8E8E8, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this);
            return true;
        }

        @Override
        public Text getNarration() {
            return Text.literal(choice.label);
        }
    }
}
