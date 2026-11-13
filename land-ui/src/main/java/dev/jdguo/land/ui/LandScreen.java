package dev.jdguo.land.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional land screen. Claiming, joining, marking, and splitting still go through land commands.
 * Chunks are whole 16×16 parcels pieces. The map buttons only add chunks to a local selection.
 */
public final class LandScreen extends Screen {
    private PurchasePlan.Mode mode = PurchasePlan.Mode.WHOLE_PARCEL;
    private ChoiceListWidget list;
    private TextFieldWidget nameField;
    private String notice = "";
    private String selectedParcel = "";
    private int seenGeneration = -1;

    public LandScreen() {
        super(Text.literal("领地"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 160;
        addDrawableChild(ButtonWidget.builder(Text.literal("整宗地"), button -> {
            mode = PurchasePlan.Mode.WHOLE_PARCEL;
            clearAndInit();
        }).dimensions(left, 78, 78, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("拆出区块"), button -> {
            mode = PurchasePlan.Mode.SPLIT_CHUNK;
            clearAndInit();
        }).dimensions(left + 82, 78, 78, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("清除选取"), button -> {
            MapPick.clear();
            notice = MapPick.notice();
            clearAndInit();
        }).dimensions(left + 164, 78, 78, 18).build());

        int listWidth = Math.min(320, this.width - 24);
        int listTop = 104;
        int listHeight = Math.max(54, this.height - listTop - 112);
        list = new ChoiceListWidget(client, (this.width - listWidth) / 2, listTop, listWidth, listHeight);
        list.setChoices(rows());
        list.select(selectedParcel);
        addSelectableChild(list);

        int y = this.height - 46;
        if (mode == PurchasePlan.Mode.WHOLE_PARCEL) {
            addDrawableChild(ButtonWidget.builder(Text.literal("占领脚下区块"), button -> run(claimHere()))
                    .dimensions(left, y, 148, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("并入选中宗地"), button -> run(joinSelected()))
                    .dimensions(left + 154, y, 148, 18).build());
        } else {
            nameField = new TextFieldWidget(textRenderer, left + 40, y - 22, 120, 16, Text.literal("新宗地名称"));
            nameField.setMaxLength(32);
            addDrawableChild(nameField);
            addDrawableChild(ButtonWidget.builder(Text.literal("标记脚下区块"), button -> run(PurchasePlan.mark()))
                    .dimensions(left + 168, y - 24, 148, 18).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("拆成新宗地"), button -> run(PurchasePlan.split(nameField.getText())))
                    .dimensions(left, y, 148, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("关闭"), button -> close())
                .dimensions(this.width / 2 + 90, y, 60, 18).build());
        seenGeneration = ClientClaims.generation();
    }

    @Override
    public void tick() {
        if (ClientClaims.generation() != seenGeneration) {
            clearAndInit();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int x = this.width / 2 - 160;
        context.drawText(textRenderer, "领地", x, 8, 0xFFD0B060, false);
        context.drawText(textRenderer, "每块都是完整的 16×16 区块，不能拆到方块里面。", x, 22, 0xFFE6E6E6, false);
        context.drawText(textRenderer, mapHint(), x, 36, 0xFFB7C7B0, false);
        String standing = standingLine();
        context.drawText(textRenderer, standing, x, 50, 0xFFE6E6E6, false);
        context.drawText(textRenderer, "地图上已选 " + MapPick.selected().size() + " 块。", x, 64, 0xFFE8D090, false);
        if (list != null) {
            list.render(context, mouseX, mouseY, delta);
        }
        if (mode == PurchasePlan.Mode.SPLIT_CHUNK) {
            context.drawText(textRenderer, "名称", x, this.height - 68, 0xFFAAAAAA, false);
        }
        if (!notice.isBlank()) {
            context.drawText(textRenderer, notice, x, 88, 0xFFFFCC88, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && list.isMouseOver(mouseX, mouseY) && list.mouseClicked(mouseX, mouseY, button)) {
            selectedParcel = list.selectedId() == null ? "" : list.selectedId();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<ChoiceListWidget.Choice> rows() {
        if (mode == PurchasePlan.Mode.SPLIT_CHUNK) {
            List<ChoiceListWidget.Choice> rows = new ArrayList<>();
            String dimension = dimension();
            for (MapPick.Chunk chunk : MapPick.inDimension(dimension)) {
                rows.add(new ChoiceListWidget.Choice(chunk.x() + " " + chunk.z(),
                        Dimensions.label(dimension) + " 区块 (" + chunk.x() + ", " + chunk.z() + ") · 16×16"));
            }
            if (rows.isEmpty()) {
                rows.add(new ChoiceListWidget.Choice("", "还没有在地图上选取区块"));
            }
            return rows;
        }
        List<ChoiceListWidget.Choice> rows = new ArrayList<>();
        for (ClientClaims.ParcelView parcel : ClientClaims.parcels(dimension())) {
            rows.add(new ChoiceListWidget.Choice(parcel.id().toString(), parcel.label()));
        }
        if (rows.isEmpty()) {
            rows.add(new ChoiceListWidget.Choice("", ClientClaims.received() ? "这个世界还没有宗地" : "还没有收到领地数据"));
        }
        return rows;
    }

    private String mapHint() {
        boolean worldMap = FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        boolean minimap = FabricLoader.getInstance().isModLoaded("xaerominimap");
        if (worldMap && minimap) {
            return "世界地图上有「选取此区块」，右键也可以选。小地图的路标界面同样有这个按钮。";
        }
        if (worldMap) {
            return "世界地图上有「选取此区块」，右键地图也可以选。";
        }
        if (minimap) {
            return "从小地图打开路标界面时，上面有「选取此区块」。";
        }
        return "当前没有世界地图或小地图。领地命令照常可用。";
    }

    private String standingLine() {
        if (client == null || client.player == null) {
            return "还没有站在世界里。";
        }
        int chunkX = ChunkGrid.chunkOfBlock(client.player.getBlockPos().getX());
        int chunkZ = ChunkGrid.chunkOfBlock(client.player.getBlockPos().getZ());
        return "脚下是" + Dimensions.label(dimension()) + "区块 (" + chunkX + ", " + chunkZ + ")。";
    }

    private PurchasePlan.Decision claimHere() {
        if (!hasPlayer()) {
            return PurchasePlan.Decision.refuse("还没有站在世界里。");
        }
        ClientClaims.Entry entry = ClientClaims.get(dimension(), standingX(), standingZ());
        return PurchasePlan.claim(ClientClaims.received(), entry != null);
    }

    private PurchasePlan.Decision joinSelected() {
        if (!hasPlayer()) {
            return PurchasePlan.Decision.refuse("还没有站在世界里。");
        }
        if (selectedParcel.isBlank()) {
            return PurchasePlan.Decision.refuse("先选中一块宗地。");
        }
        ClientClaims.ParcelView parcel = findParcel(selectedParcel);
        if (parcel == null) {
            return PurchasePlan.Decision.refuse("这块宗地已经不在最新的领地数据里。");
        }
        int chunkX = standingX();
        int chunkZ = standingZ();
        boolean inside = PurchasePlan.contains(chunkX, chunkZ, parcel.packedChunks());
        boolean adjacent = PurchasePlan.adjacentToAny(chunkX, chunkZ, parcel.packedChunks());
        return PurchasePlan.join(PurchasePlan.token(parcel.name(), parcel.shortId()), inside, adjacent);
    }

    private ClientClaims.ParcelView findParcel(String id) {
        for (ClientClaims.ParcelView parcel : ClientClaims.parcels(dimension())) {
            if (parcel.id().toString().equals(id)) {
                return parcel;
            }
        }
        return null;
    }

    private void run(PurchasePlan.Decision decision) {
        if (!decision.allowed()) {
            notice = decision.refusal();
            return;
        }
        if (client == null || client.player == null) {
            notice = "还没有连上服务器。";
            return;
        }
        client.player.networkHandler.sendChatCommand(decision.command());
        notice = "已交给服务器。人必须站在相关的区块里，命令才会生效。";
    }

    private boolean hasPlayer() {
        return client != null && client.player != null && client.world != null;
    }

    private String dimension() {
        if (client == null || client.world == null) {
            return "";
        }
        return client.world.getRegistryKey().getValue().toString();
    }

    private int standingX() {
        return ChunkGrid.chunkOfBlock(client.player.getBlockPos().getX());
    }

    private int standingZ() {
        return ChunkGrid.chunkOfBlock(client.player.getBlockPos().getZ());
    }

}
