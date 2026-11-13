package dev.jdguo.land.ui;

public final class Dimensions {
    private Dimensions() {
    }

    public static String label(String id) {
        if (id == null || id.isBlank()) {
            return "未知世界";
        }
        return switch (id) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> id;
        };
    }
}
