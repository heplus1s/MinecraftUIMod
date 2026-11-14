package dev.jdguo.bank.ui;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Lines for the central-bank management page. Missing fields stay missing. */
public final class BankBook {
    public static final String MISSING = "暂无数据";
    public static final String CIRCULATION_NOTE = "以上是流通量统计，不是准备金。";
    public static final String FREEZE_NOTE = "只暂停或恢复用银行券兑换铜锭或钻石。普通存取不受影响。";
    public static final String UNBACKED_NOTE = "不增加准备金。增发所选货币，并记入操作者的账户。";
    public static final String RETIRE_NOTE = "从操作者的账户扣减所选货币，并减少发行额。";
    public static final String BACKED_NOTE = "按准备金满额发行：柜台还没有这个动作。";
    public static final String PAUSE = "暂停银行券兑付";
    public static final String RESUME = "恢复银行券兑付";

    private BankBook() {
    }

    public static List<String> preface(JsonObject data) {
        List<String> lines = new ArrayList<>();
        String status = BankSnapshot.text(data, "status");
        lines.add("兑付状态：" + (status == null ? MISSING : status));
        lines.add(redemption(data));
        String tightness = firstPresent(data, "tightness", "redemptionCapacity");
        if (tightness != null) {
            lines.add("兑付能力：" + BankHeader.figure(tightness));
        }
        String fee = BankSnapshot.text(data, "feePercent");
        if (fee != null) {
            lines.add("手续费：" + (fee.contains("%") ? fee : BankHeader.figure(fee) + "%"));
        }
        lines.addAll(BankHeader.managementAggregates(data));
        lines.add(CIRCULATION_NOTE);
        return lines;
    }

    public static String redemption(JsonObject data) {
        Boolean frozen = BankSnapshot.frozen(data);
        if (frozen != null) {
            return "银行券兑换铜锭或钻石：" + (frozen ? "已暂停" : "正常");
        }
        String status = BankSnapshot.text(data, "status");
        if (status == null) {
            return "银行券兑换铜锭或钻石：" + MISSING;
        }
        return "银行券兑换铜锭或钻石：" + (status.contains("暂停") ? "已暂停" : "正常");
    }

    public static String freezeButton(JsonObject data) {
        Boolean frozen = BankSnapshot.frozen(data);
        if (Boolean.TRUE.equals(frozen)) {
            return RESUME;
        }
        String status = BankSnapshot.text(data, "status");
        if (frozen == null && status != null && status.contains("暂停")) {
            return RESUME;
        }
        return PAUSE;
    }

    public static List<String> book(JsonObject data, String currencyId, String currencyName) {
        BankSnapshot.CentralRow row = find(data, currencyId, currencyName);
        List<String> lines = new ArrayList<>();
        lines.add(field("准备金", row == null ? null : row.reserve()));
        lines.add(field("央行储蓄", row == null ? null : row.savings()));
        lines.add(field("存款保管", row == null ? null : row.vault()));
        lines.add(field("发行额", row == null ? null : row.liability()));
        lines.add(field("已锁定准备金", row == null ? null : row.locked()));
        lines.add(field("按准备金尚可发行", row == null ? null : row.backedRoom()));
        return lines;
    }

    public static List<String> institutions(JsonObject data) {
        List<String> lines = new ArrayList<>();
        if (!BankSnapshot.has(data, "institutions")) {
            lines.add("商店经营账户和土地出让账户：" + MISSING);
            return lines;
        }
        if (BankSnapshot.institutions(data).isEmpty()) {
            lines.add("商店经营账户和土地出让账户：" + MISSING);
            return lines;
        }
        for (BankSnapshot.InstitutionRow row : BankSnapshot.institutions(data)) {
            lines.add(row.name() + "：" + (row.detail() == null ? MISSING : row.detail()));
        }
        return lines;
    }

    public static List<String> actionNotes() {
        return List.of(FREEZE_NOTE, UNBACKED_NOTE, RETIRE_NOTE, BACKED_NOTE);
    }

    private static BankSnapshot.CentralRow find(JsonObject data, String currencyId, String currencyName) {
        BankSnapshot.CentralRow named = null;
        for (BankSnapshot.CentralRow row : BankSnapshot.central(data)) {
            if (currencyId != null && currencyId.equals(row.id())) {
                return row;
            }
            if (currencyName != null && currencyName.equals(row.name()) && named == null) {
                named = row;
            }
        }
        return named;
    }

    private static String field(String label, String raw) {
        return label + "：" + (raw == null ? MISSING : BankHeader.figure(raw));
    }

    private static String firstPresent(JsonObject data, String... keys) {
        for (String key : keys) {
            String value = BankSnapshot.text(data, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
