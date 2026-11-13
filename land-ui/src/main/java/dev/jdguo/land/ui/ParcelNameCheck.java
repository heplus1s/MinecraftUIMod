package dev.jdguo.land.ui;

/** Display names a player can give a parcel. Empty means unnamed. */
public final class ParcelNameCheck {
    public static final int MAX_LENGTH = 32;

    private ParcelNameCheck() {
    }

    /**
     * @return the trimmed name, an empty string when the name is cleared, or null when it is illegal
     */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.trim();
        if (name.codePointCount(0, name.length()) > MAX_LENGTH) {
            return null;
        }
        for (int index = 0; index < name.length(); ) {
            int codePoint = name.codePointAt(index);
            if (Character.isISOControl(codePoint)) {
                return null;
            }
            index += Character.charCount(codePoint);
        }
        return name;
    }
}
