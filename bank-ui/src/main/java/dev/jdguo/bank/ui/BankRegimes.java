package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Issuer regime copy and the core's switch offers. Missing data stays missing. */
public final class BankRegimes {
    public static final String FLOAT_NAME = "浮动汇率制";
    public static final String PEG_NAME = "联系汇率制";
    public static final String HARD_NAME = "固定汇率制";
    public static final String NO_SWITCH = "是否可以切换：暂无数据";
    public static final String SWITCH_NOTE = "切换不增发，也不免除已经锁定的准备金。";

    public static final String FLOAT_RULE = "牌价随买卖变动。发行和兑付都按当时的牌价结算，不把牌价固定在面值上。新开的报价从官方牌价或面值起步。";
    public static final String PEG_RULE = "先选定铜锭、钻石或银行券作为锚。官方牌价按切换当时的现价一次定下，之后买卖不再改动这张牌价。";
    public static final String HARD_RULE = "兑付按面值进行。锁定的准备金如果不够兑付全部流通量，只兑到锁定的抵押品为止，其余仍留在持有人处，面值并不改变。";

    public static final String FLOAT_GATE = "换成浮动汇率制，须按面值锁足个人的铜锭或钻石，并且央行票据至少达到流通量的百分之十。";
    public static final String PEG_GATE = "换成联系汇率制要先选锚。锚为铜锭或钻石时，个人锁定至少达到面值的百分之八十，央行票据至少达到钉住价值的百分之二十。锚为银行券时，不锁定铜锭或钻石，央行票据须足额覆盖面值。";
    public static final String HARD_GATE = "换成固定汇率制只能选铜锭或钻石。个人须按面值锁足，央行票据至少达到面值的百分之五。现价若已偏离面值，或锁定的商品不够按面值覆盖全部流通量，则不能切换。";

    private BankRegimes() {
    }

    public record Offer(String regime, String anchor, String name, boolean allowed, String reason) {
        public String command() {
            if (!allowed) {
                return null;
            }
            String kind = BankActions.regimeToken(regime);
            String peg = BankActions.anchorToken(anchor);
            if ("note".equals(peg) && !"peg".equals(kind)) {
                return null;
            }
            if (peg == null) {
                return "note-switch " + kind;
            }
            return BankActions.noteSwitch(kind, peg);
        }

        public String button() {
            if (command() == null) {
                return null;
            }
            return "可以切换为" + (name == null || name.isBlank() ? fallbackName() : name);
        }

        public String refusal() {
            if (allowed && command() != null) {
                return null;
            }
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
            return NO_SWITCH;
        }

        private String fallbackName() {
            String base = switch (BankActions.regimeToken(regime)) {
                case "peg" -> PEG_NAME;
                case "hard" -> HARD_NAME;
                default -> FLOAT_NAME;
            };
            String peg = BankActions.anchorToken(anchor);
            if (peg == null) {
                return base;
            }
            String anchorName = switch (peg) {
                case "diamond" -> "钻石";
                case "copper" -> "铜锭";
                default -> "银行券";
            };
            return base + "，锚为" + anchorName;
        }
    }

    public static List<String> rules() {
        return List.of(FLOAT_NAME + "：" + FLOAT_RULE, PEG_NAME + "：" + PEG_RULE, HARD_NAME + "：" + HARD_RULE);
    }

    public static List<String> gates() {
        return List.of(SWITCH_NOTE, FLOAT_GATE, PEG_GATE, HARD_GATE);
    }

    public static List<String> figures(JsonObject data) {
        JsonObject note = BankSnapshot.ownNote(data);
        List<String> lines = new ArrayList<>();
        String regime = BankSnapshot.regimeName(data);
        lines.add("当前发行制度：" + (regime == null ? BankBook.MISSING : regime));
        lines.add("锚：" + anchorName(first(note, data, "anchor")));
        lines.add(amount("平价", first(note, data, "par")));
        lines.add(amount("面值", first(note, data, "face")));
        lines.add(amount("已锁定", first(note, data, "locked")));
        lines.add(amount("个人准备金", note == null ? null : BankSnapshot.text(note, "personalReserve")));
        lines.add(amount("记在这张券上的央行准备金", note == null ? null : BankSnapshot.text(note, "centralReserve")));
        lines.add(amount("流通量", first(note, data, "circulation")));
        lines.add(price("现价", note == null ? null : BankSnapshot.text(note, "live")));
        lines.add(price("钻石牌价", note == null ? null : BankSnapshot.text(note, "diamondPrice")));
        lines.add(price("官方牌价", note == null ? null : BankSnapshot.text(note, "official")));
        lines.add(amount("央行银行券准备金", BankSnapshot.text(data, "reserve")));
        lines.add(amount("所需准备金", BankSnapshot.text(data, "required")));
        return lines;
    }

    public static boolean switchesKnown(JsonObject data) {
        JsonObject note = BankSnapshot.ownNote(data);
        return note != null && note.has("switches");
    }

    public static List<Offer> offers(JsonObject data) {
        List<Offer> rows = new ArrayList<>();
        JsonObject note = BankSnapshot.ownNote(data);
        if (note == null || !note.has("switches")) {
            return rows;
        }
        List<JsonObject> array = arrayOf(note);
        for (JsonObject row : array) {
            String regime = BankSnapshot.text(row, "regime");
            if (regime == null) {
                continue;
            }
            boolean allowed = row.has("allowed") && row.get("allowed").isJsonPrimitive() && row.get("allowed").getAsBoolean();
            rows.add(new Offer(regime, BankSnapshot.text(row, "anchor"), BankSnapshot.text(row, "name"), allowed,
                    BankSnapshot.text(row, "reason")));
        }
        return rows;
    }

    public static String blockReason(JsonObject data) {
        if (BankSnapshot.bankrupt(data)) {
            return "这张个人银行券已经破产，不能切换发行制度。";
        }
        if (data != null && data.has("suspended") && data.get("suspended").isJsonPrimitive() && data.get("suspended").getAsBoolean()) {
            return "这张个人银行券已暂停，不能切换发行制度。";
        }
        return null;
    }

    private static List<JsonObject> arrayOf(JsonObject data) {
        List<JsonObject> rows = new ArrayList<>();
        if (data == null || !data.has("switches") || !data.get("switches").isJsonArray()) {
            return rows;
        }
        for (var element : data.getAsJsonArray("switches")) {
            if (element.isJsonObject()) {
                rows.add(element.getAsJsonObject());
            }
        }
        return rows;
    }

    private static String first(JsonObject note, JsonObject root, String key) {
        String value = note == null ? null : BankSnapshot.text(note, key);
        if (value != null) {
            return value;
        }
        return BankSnapshot.text(root, key);
    }

    private static String amount(String label, String raw) {
        return label + "：" + (raw == null ? BankBook.MISSING : BankHeader.figure(raw));
    }

    private static String price(String label, String raw) {
        return label + "：" + (raw == null ? "暂无牌价" : BankHeader.figure(raw));
    }

    private static String anchorName(String anchor) {
        String token = BankActions.anchorToken(anchor);
        if (token == null) {
            return anchor == null ? BankBook.MISSING : anchor;
        }
        return switch (token) {
            case "diamond" -> "钻石";
            case "copper" -> "铜锭";
            default -> "银行券";
        };
    }
}
