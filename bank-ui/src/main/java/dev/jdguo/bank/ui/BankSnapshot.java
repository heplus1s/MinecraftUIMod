package dev.jdguo.bank.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reads the optional UI snapshot. Missing fields stay missing; nothing is invented. */
public final class BankSnapshot {
    public record MoneyRow(String id, String name, String amount, String rate, String unitsPerDiamond) {
    }

    public record NoteRow(String id, String name, String balance, String price) {
    }

    public record CentralRow(
            String id,
            String name,
            String savings,
            String reserve,
            String vault,
            String liability,
            String locked,
            String backedRoom) {
    }

    public record InstitutionRow(String name, String detail) {
    }

    private BankSnapshot() {
    }

    public static boolean admin(JsonObject data) {
        return flag(data, "admin");
    }

    public static boolean confirm(JsonObject data) {
        return flag(data, "confirm");
    }

    public static boolean bankrupt(JsonObject data) {
        return flag(data, "bankrupt");
    }

    public static boolean hasPersonal(JsonObject data) {
        return text(data, "personal") != null || ownNote(data) != null;
    }

    public static JsonObject ownNote(JsonObject data) {
        if (data == null || !data.has("ownNote") || !data.get("ownNote").isJsonObject()) {
            return null;
        }
        return data.getAsJsonObject("ownNote");
    }

    /** Issuer regime name, preferring the core's own wording. */
    public static String regimeName(JsonObject data) {
        JsonObject note = ownNote(data);
        String named = note == null ? null : text(note, "regimeName");
        if (named == null) {
            named = text(data, "regimeName");
        }
        if (named != null) {
            return named;
        }
        String regime = note == null ? null : text(note, "regime");
        if (regime == null) {
            regime = text(data, "regime");
        }
        return regimeLabel(regime);
    }

    public static String text(JsonObject data, String key) {
        if (data == null || !data.has(key) || data.get(key).isJsonNull() || !data.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = data.get(key).getAsString();
        return value == null || value.isBlank() ? null : value;
    }

    public static Boolean frozen(JsonObject data) {
        if (data == null || !data.has("frozen") || !data.get("frozen").isJsonPrimitive()) {
            return null;
        }
        if (!data.get("frozen").getAsJsonPrimitive().isBoolean()) {
            return null;
        }
        return data.get("frozen").getAsBoolean();
    }

    public static boolean has(JsonObject data, String key) {
        return data != null && data.has(key) && !data.get(key).isJsonNull();
    }

    public static List<MoneyRow> currencies(JsonObject data) {
        List<MoneyRow> rows = new ArrayList<>();
        for (JsonObject row : array(data, "currencies")) {
            String id = text(row, "id");
            String name = text(row, "name");
            if (id == null || name == null) {
                continue;
            }
            rows.add(new MoneyRow(id, name, amount(row), first(row, "rate"), first(row, "unitsPerDiamond")));
        }
        return rows;
    }

    public static List<MoneyRow> hardCurrencies(JsonObject data) {
        List<MoneyRow> rows = new ArrayList<>();
        for (MoneyRow row : currencies(data)) {
            if (isHard(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    public static boolean isHard(MoneyRow row) {
        return "copper".equals(row.id()) || "diamond".equals(row.id()) || "note".equals(row.id())
                || "铜锭".equals(row.name()) || "钻石".equals(row.name()) || "银行券".equals(row.name());
    }

    public static String balancePhrase(String amount) {
        if (amount == null || amount.isBlank()) {
            return "余额：暂无";
        }
        return "余额：" + BankHeader.figure(amount);
    }

    public static List<NoteRow> notes(JsonObject data) {
        List<NoteRow> rows = new ArrayList<>();
        for (JsonObject row : array(data, "notes")) {
            String name = text(row, "name");
            if (name == null) {
                continue;
            }
            String price = first(row, "price", "diamondPrice", "rate");
            rows.add(new NoteRow(text(row, "id"), name, amount(row), price));
        }
        return rows;
    }

    public static List<NoteRow> heldNotes(JsonObject data) {
        List<NoteRow> held = new ArrayList<>();
        for (NoteRow note : notes(data)) {
            if (!zero(note.balance())) {
                held.add(note);
            }
        }
        return held;
    }

    public static List<CentralRow> central(JsonObject data) {
        List<CentralRow> rows = new ArrayList<>();
        for (JsonObject row : array(data, "central")) {
            String name = text(row, "name");
            if (name == null) {
                continue;
            }
            rows.add(new CentralRow(
                    text(row, "id"),
                    name,
                    text(row, "savings"),
                    text(row, "reserve"),
                    text(row, "vault"),
                    text(row, "liability"),
                    text(row, "locked"),
                    text(row, "backedRoom")));
        }
        return rows;
    }

    public static List<InstitutionRow> institutions(JsonObject data) {
        List<InstitutionRow> rows = new ArrayList<>();
        for (JsonObject row : array(data, "institutions")) {
            String name = text(row, "name");
            if (name == null) {
                continue;
            }
            rows.add(new InstitutionRow(name, text(row, "detail")));
        }
        return rows;
    }

    public static String regimeLabel(String regime) {
        if (regime == null) {
            return null;
        }
        return switch (regime) {
            case "FLOAT", "float", "浮动", "浮动汇率制" -> "浮动汇率制";
            case "PEG", "peg", "钉住", "联系汇率制" -> "联系汇率制";
            case "HARD", "hard", "定额兑付", "固定汇率制" -> "固定汇率制";
            default -> regime;
        };
    }

    public static boolean zero(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", "").replace("_", "")).signum() == 0;
        } catch (NumberFormatException exception) {
            return "0".equals(raw.trim());
        }
    }

    private static String amount(JsonObject row) {
        String balance = text(row, "balance");
        if (balance != null) {
            return balance;
        }
        return text(row, "amount");
    }

    private static String first(JsonObject row, String... keys) {
        for (String key : keys) {
            String value = text(row, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean flag(JsonObject data, String key) {
        return data != null && data.has(key) && data.get(key).isJsonPrimitive() && data.get(key).getAsBoolean();
    }

    private static List<JsonObject> array(JsonObject data, String key) {
        List<JsonObject> rows = new ArrayList<>();
        if (data == null || !data.has(key) || !data.get(key).isJsonArray()) {
            return rows;
        }
        JsonArray array = data.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                rows.add(element.getAsJsonObject());
            }
        }
        return rows;
    }
}
