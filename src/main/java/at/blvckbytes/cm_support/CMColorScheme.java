package at.blvckbytes.cm_support;

import at.blvckbytes.component_markup.markup.parser.token.TokenType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;

public class CMColorScheme {

  // TODO: I should really also come up with a light-theme for this language...
  private static final JBColor COLOR_ESCAPE_SEQUENCE = regularAndDarkRGB(209, 105, 105);
  private static final JBColor COLOR_COMMENT = regularAndDarkRGB(106, 153, 85);
  private static final JBColor COLOR_PLAIN_TEXT = regularAndDarkRGB(255, 255, 255);
  private static final JBColor COLOR_IDENTIFIER = regularAndDarkRGB(156, 220, 254);
  private static final JBColor COLOR_TAG_PUNCTUATION = regularAndDarkRGB(121, 158, 150);
  private static final JBColor COLOR_TAG_IDENTIFIER = regularAndDarkRGB(78, 201, 176);
  private static final JBColor COLOR_LITERAL = regularAndDarkRGB(86, 156, 214);
  private static final JBColor COLOR_STRING = regularAndDarkRGB(206, 145, 120);
  private static final JBColor COLOR_NUMBER = regularAndDarkRGB(181, 206, 168);
  private static final JBColor COLOR_ANY_PUNCTUATION = regularAndDarkRGB(212, 212, 212);

  private static final TextAttributesKey[] attributesByTokenTypeOrdinal;

  static {
    var tokenTypes = TokenType.values();

    attributesByTokenTypeOrdinal = new TextAttributesKey[tokenTypes.length];

    for (var typeOrdinal = 0; typeOrdinal < tokenTypes.length; ++typeOrdinal)
      attributesByTokenTypeOrdinal[typeOrdinal] = decideAttributesKey(tokenTypes[typeOrdinal]);
  }

  public static TextAttributesKey forType(TokenType type) {
    return attributesByTokenTypeOrdinal[type.ordinal()];
  }

  private static JBColor decideTokenColor(TokenType type) {
    return switch (type) {
      case ANY__ESCAPE_SEQUENCE -> COLOR_ESCAPE_SEQUENCE;
      case MARKUP__COMMENT -> COLOR_COMMENT;
      case MARKUP__IDENTIFIER__BINDING, EXPRESSION__IDENTIFIER_ANY -> COLOR_IDENTIFIER;
      case MARKUP__PUNCTUATION__TAG -> COLOR_TAG_PUNCTUATION;
      case MARKUP__LITERAL,
           EXPRESSION__LITERAL,
           MARKUP__IDENTIFIER__ATTRIBUTE_USER,
           MARKUP__IDENTIFIER__ATTRIBUTE_INTRINSIC,
           EXPRESSION__NAMED_PREFIX_OPERATOR,
           EXPRESSION__NAMED_INFIX_OPERATOR -> COLOR_LITERAL;
      case EXPRESSION__STRING,
           MARKUP__STRING -> COLOR_STRING;
      case MARKUP__NUMBER,
           EXPRESSION__NUMBER -> COLOR_NUMBER;
      case MARKUP__IDENTIFIER__TAG -> COLOR_TAG_IDENTIFIER;
      case ANY__INTERPOLATION,
           MARKUP__PUNCTUATION__SUBTREE,
           MARKUP__PUNCTUATION__BINDING_SEPARATOR,
           MARKUP__PUNCTUATION__EQUALS,
           EXPRESSION__PUNCTUATION__ANY,
           MARKUP__OPERATOR__SPREAD,
           MARKUP__OPERATOR__INTRINSIC_LITERAL,
           MARKUP__OPERATOR__INTRINSIC_EXPRESSION,
           MARKUP__OPERATOR__DYNAMIC_ATTRIBUTE,
           MARKUP__OPERATOR__CAPTURE,
           EXPRESSION__SYMBOLIC_OPERATOR__ANY -> COLOR_ANY_PUNCTUATION;
      default -> COLOR_PLAIN_TEXT;
    };
  }

  private static JBColor regularAndDarkRGB(int r, int g, int b) {
    var value = (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    return new JBColor(value, value);
  }

  private static TextAttributesKey decideAttributesKey(TokenType tokenType) {
    var name = "ComponentMarkup." + tokenType.name();

    var textAttributes = new TextAttributes();
    textAttributes.setForegroundColor(decideTokenColor(tokenType));

    //noinspection deprecation
    return TextAttributesKey.createTextAttributesKey(name, textAttributes);
  }
}
