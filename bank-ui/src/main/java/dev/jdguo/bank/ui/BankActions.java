package dev.jdguo.bank.ui;

/**
 * Payloads the installed bank core already accepts. Nothing here is a new server action.
 * Naming a personal note is a chat command, not a text field: the core's command is
 * {@code /bank note create <float|peg|hard> }, and the player types the name after it.
 */
public final class BankActions {
    private BankActions() {
    }

    public static String deposit(String currencyId, String quantity) {
        return "deposit-transfer " + currencyId + " " + quantity;
    }

    public static String withdraw(String currencyId, String quantity) {
        return "withdraw " + currencyId + " " + quantity;
    }

    public static String exchange(String payId, String receiveId, String quantity) {
        return "exchange " + payId + " " + receiveId + " " + quantity;
    }

    public static String check(String currencyId, String quantity, boolean selfOnly) {
        return "check " + currencyId + " " + quantity + (selfOnly ? " self" : " bearer");
    }

    /** Redeems drafts in the inventory. The core also deposits commodities and bank notes it finds. */
    public static String presentHeld() {
        return "deposit-held";
    }

    public static String noteBuy(String name, String quantity) {
        return "note-buy " + name + " " + quantity;
    }

    public static String noteSell(String name, String quantity) {
        return "note-sell " + name + " " + quantity;
    }

    public static String noteSwap(String payName, String receiveName, String quantity) {
        return "note-swap " + payName + " " + receiveName + " " + quantity;
    }

    public static String noteRedeem(String name, String payoutId, String quantity) {
        return "note-redeem " + name + " " + payoutId + " " + quantity;
    }

    public static String noteIssue(String quantity) {
        return "note-issue " + quantity;
    }

    public static String noteUnbacked(String quantity) {
        return "note-unbacked " + quantity;
    }

    public static String reserveDeposit(String quantity) {
        return "reserve-deposit " + quantity;
    }

    public static String reserveWithdraw(String quantity) {
        return "reserve-withdraw " + quantity;
    }

    public static String noteBankrupt() {
        return "note-bankrupt";
    }

    public static String noteBankruptYes() {
        return "note-bankrupt-yes";
    }

    public static String noteBankruptNo() {
        return "note-bankrupt-no";
    }

    public static String adminFreeze() {
        return "admin-freeze";
    }

    public static String adminUnbacked(String currencyId, String quantity) {
        return "admin-unbacked " + currencyId + " " + quantity;
    }

    public static String adminRetire(String currencyId, String quantity) {
        return "admin-retire " + currencyId + " " + quantity;
    }

    public static String close() {
        return "close";
    }

    /** Chat text, including the leading slash and a trailing space for the name. */
    public static String noteNameCommand(String regime) {
        String kind = switch (regime == null ? "" : regime) {
            case "peg", "PEG", "钉住" -> "peg";
            case "hard", "HARD", "定额兑付" -> "hard";
            default -> "float";
        };
        return "/bank note create " + kind + " ";
    }
}
