package at.blvckbytes.cm_support;

import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.expression.tokenizer.ExpressionTokenizeException;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.markup.parser.token.HierarchicalToken;
import at.blvckbytes.component_markup.markup.parser.token.OutputFlag;
import at.blvckbytes.component_markup.markup.parser.token.TokenOutput;
import at.blvckbytes.component_markup.util.InputView;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public record AnnotationInformation(
  List<TokenAnnotation> tokenAnnotations,
  List<ParserError> parserErrors
) {

  public AnnotationInformation() {
    this(new ArrayList<>(), new ArrayList<>());
  }

  public AnnotationInformation parseCollectedInfo(CollectedInformation collectedInfo) {
    for (var scalarInfo : collectedInfo.scalarInfos)
      createAnnotationInfo(scalarInfo);

    return this;
  }

  private void createAnnotationInfo(ScalarInfo scalarInfo) {
    var tokenOutput = new TokenOutput(EnumSet.noneOf(OutputFlag.class));
    var inputView = InputView.of(scalarInfo.contents());

    if (scalarInfo.type() == CMMarkerType.EXPRESSION) {
      try {
        tokenOutput.onInitialization(inputView);
        ExpressionParser.parse(inputView, tokenOutput);
        tokenOutput.onInputEnd();
      } catch (ExpressionTokenizeException | ExpressionParseException error) {
        parserErrors.add(new ParserError(error.getErrorMessage(), scalarInfo.startOffset() + error.getErrorPosition()));
        return;
      }
    } else if (scalarInfo.type() == CMMarkerType.MARKUP) {
      try {
        MarkupParser.parse(inputView, BuiltInTagRegistry.INSTANCE, tokenOutput);
      } catch (MarkupParseException error) {
        parserErrors.add(new ParserError(error.getErrorMessage(), scalarInfo.startOffset() + error.getErrorPosition()));
        return;
      }
    } else
      throw new IllegalArgumentException("Unaccounted-for marker-type: " + scalarInfo.type());

    for (var token : tokenOutput.getResult())
      createHighlightsForToken(token, scalarInfo.startOffset());
  }

  private void createHighlightsForToken(HierarchicalToken token, int offset) {
    var range = TextRange.create(
      offset + token.value.startInclusive,
      offset + token.value.endExclusive
    );

    tokenAnnotations.add(new TokenAnnotation(range, CMColorScheme.forType(token.type)));

    var children = token.getChildren();

    if (children != null) {
      for (var childToken : children)
        createHighlightsForToken(childToken, offset);
    }
  }
}
