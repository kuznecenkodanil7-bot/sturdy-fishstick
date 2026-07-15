package dev.raidmine.stafftool.util;

import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Finds the exact rendered player token under the mouse, not the prefix component beside it. */
public final class NicknameHitConsumer implements DrawnTextConsumer {
    private static final int LINE_HEIGHT = 9;
    private static final Transformation DEFAULT_TRANSFORMATION = new Transformation(new Matrix3x2f());

    private final TextRenderer textRenderer;
    private final int clickX;
    private final int clickY;
    private Transformation transformation = DEFAULT_TRANSFORMATION;
    private Hit hit;

    public NicknameHitConsumer(TextRenderer textRenderer, int clickX, int clickY) {
        this.textRenderer = textRenderer;
        this.clickX = clickX;
        this.clickY = clickY;
    }

    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
    }

    @Override
    public void text(Alignment alignment, int x, int y, Transformation transformation, OrderedText text) {
        if (hit != null) return;
        ScreenRect scissor = transformation.scissor();
        if (scissor != null && !scissor.contains(clickX, clickY)) return;

        Matrix3x2f inverse = new Matrix3x2f(transformation.pose()).invert();
        Vector2f local = inverse.transformPosition(new Vector2f(clickX, clickY));
        int adjustedX = alignment.getAdjustedX(x, textRenderer, text);
        if (local.y < y || local.y >= y + LINE_HEIGHT) return;

        List<Piece> pieces = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        final float[] cursor = {adjustedX};
        text.accept((index, style, codePoint) -> {
            String character = new String(Character.toChars(codePoint));
            int charStart = line.length();
            line.append(character);
            float advance = textRenderer.getTextHandler().getWidth(OrderedText.styled(codePoint, style));
            pieces.add(new Piece(charStart, line.length(), cursor[0], cursor[0] + advance, style));
            cursor[0] += advance;
            return true;
        });

        Piece clicked = null;
        for (Piece piece : pieces) {
            if (local.x >= piece.left() && local.x < piece.right()) {
                clicked = piece;
                break;
            }
        }
        if (clicked == null) return;

        int tokenStart = clicked.start();
        int tokenEnd = clicked.end();
        String raw = line.toString();
        while (tokenStart > 0 && NicknameResolver.isNicknameChar(raw.charAt(tokenStart - 1))) tokenStart--;
        while (tokenEnd < raw.length() && NicknameResolver.isNicknameChar(raw.charAt(tokenEnd))) tokenEnd++;

        Optional<String> resolved = NicknameResolver.fromClickedLine(raw, tokenStart, tokenEnd);
        if (resolved.isEmpty()) return;

        float tokenLeft = Float.MAX_VALUE;
        float tokenRight = Float.MIN_VALUE;
        for (Piece piece : pieces) {
            if (piece.end() > tokenStart && piece.start() < tokenEnd) {
                tokenLeft = Math.min(tokenLeft, piece.left());
                tokenRight = Math.max(tokenRight, piece.right());
            }
        }
        if (tokenLeft == Float.MAX_VALUE || tokenRight <= tokenLeft) return;

        Matrix3x2f pose = new Matrix3x2f(transformation.pose());
        Vector2f topLeft = pose.transformPosition(new Vector2f(tokenLeft, y));
        Vector2f bottomRight = pose.transformPosition(new Vector2f(tokenRight, y + LINE_HEIGHT));
        hit = new Hit(resolved.get(), raw.substring(tokenStart, tokenEnd),
                Math.round(Math.min(topLeft.x, bottomRight.x)),
                Math.round(Math.min(topLeft.y, bottomRight.y)),
                Math.round(Math.max(topLeft.x, bottomRight.x)),
                Math.round(Math.max(topLeft.y, bottomRight.y)));
    }

    @Override
    public void marqueedText(Text text, int x, int left, int right, int top, int bottom,
                             Transformation transformation) {
        int width = textRenderer.getWidth(text);
        int y = (top + bottom - LINE_HEIGHT) / 2 + 1;
        int adjustedX = Math.max(left, Math.min(right - width, x - width / 2));
        text(Alignment.LEFT, adjustedX, y, transformation, text.asOrderedText());
    }

    public Optional<String> nickname() {
        return hit().map(Hit::nickname);
    }

    public Optional<Hit> hit() {
        return Optional.ofNullable(hit);
    }

    private record Piece(int start, int end, float left, float right, Style style) {
    }

    public record Hit(String nickname, String visibleToken, int left, int top, int right, int bottom) {
        public int width() { return Math.max(1, right - left); }
        public int height() { return Math.max(1, bottom - top); }
    }
}
