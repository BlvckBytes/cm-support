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
import at.blvckbytes.component_markup.util.ErrorProvider;
import at.blvckbytes.component_markup.util.InputView;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.EnumSet;

public class CMYamlScalarAnnotator implements Annotator {

  private static final Key<CMMarkerType> KEY_MARKED_TYPE = Key.create("cm-support:marked-type");

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    // Header-comment (very first comment in the file); correspond first k/v-pair manually, if applicable.
    if (element instanceof PsiComment comment && element.getParent() instanceof YAMLFile yamlFile) {
      var keyValue = getTopLevelKeyValueIfAny(yamlFile);

      if (keyValue == null)
        return;

      keyValue.putUserData(KEY_MARKED_TYPE, CMMarkerType.byComment(comment));
      return;
    }

    if (!(element instanceof YAMLScalar scalar))
      return;

    var markedType = computeMarkedTypeTransitively(scalar);

    if (markedType != null)
      createAnnotations(scalar, markedType, holder);
  }

  private @Nullable CMMarkerType computeMarkedTypeTransitively(YAMLScalar scalar) {
    PsiElement currentElement = scalar;

    while (currentElement != null && !(currentElement instanceof YAMLFile)) {
      var markedType = currentElement.getUserData(KEY_MARKED_TYPE);

      if (markedType == null && currentElement instanceof YAMLKeyValue keyValue) {
        markedType = getMarkedTypeOfKeyValue(keyValue);
        keyValue.putUserData(KEY_MARKED_TYPE, markedType);
      }

      if (markedType != null)
        return markedType;

      currentElement = currentElement.getParent();
    }

    return null;
  }

  private @Nullable CMMarkerType getMarkedTypeOfKeyValue(YAMLKeyValue keyValue) {
    PsiElement currentSibling = keyValue;

    while ((currentSibling = currentSibling.getPrevSibling()) != null) {
      if (currentSibling instanceof PsiComment comment) {
        var cmType = CMMarkerType.byComment(comment);

        if (cmType != null)
          return cmType;
      }

      if (currentSibling instanceof LeafPsiElement)
        continue;

      // Encountered other k/v-pairs or the like, so there's certainly no attached matching comment up above.
      return null;
    }

    return null;
  }

  private @Nullable YAMLKeyValue getTopLevelKeyValueIfAny(YAMLFile yamlFile) {
    var documents = yamlFile.getDocuments();

    if (documents.isEmpty())
      return null;

    var topLevelValue = documents.getFirst().getTopLevelValue();

    if (!(topLevelValue instanceof YAMLMapping firstMappingInFile))
      return null;

    var firstKey = firstMappingInFile.getKeyValues().stream().findFirst().orElse(null);

    if (firstKey == null)
      return null;

    return firstMappingInFile.getKeyValueByKey(firstKey.getKeyText());
  }

  private void createAnnotations(YAMLScalar scalar, CMMarkerType type, @NotNull AnnotationHolder holder) {
    var text = scalar.getText();

    if (text.isEmpty())
      return;

    var firstChar = text.charAt(0);

    var contents = text;
    int charOffset = 0;

    // "Keep newlines" block-scalar style
    if (firstChar == '|') {
      // There are optional block-chomping modifier-chars that could follow (+/-), but since there
      // is absolutely no need to use them with ComponentMarkup, I will not bother supporting them.
      if (text.length() == 1 || text.charAt(1) != '\n')
        return;

      contents = text.substring(2);
      charOffset = 2;
    }

    // "Replace newlines with spaces" block-scalar style; again, ComponentMarkup has no use-case for this mode.
    else if (firstChar == '>')
      return;

    else if (firstChar == '\'' || firstChar == '\"') {
      contents = text.substring(1, text.length() - 1);
      charOffset = 1;
    }

    var totalOffset = scalar.getTextRange().getStartOffset() + charOffset;

    var tokenOutput = new TokenOutput(EnumSet.of(OutputFlag.ALLOW_MISSING_ATTRIBUTES, OutputFlag.ENABLE_DUMMY_TAG));
    var inputView = InputView.of(contents);

    if (type == CMMarkerType.EXPRESSION) {
      try {
        tokenOutput.onInitialization(inputView);
        ExpressionParser.parse(inputView, tokenOutput);
        tokenOutput.onInputEnd();
      } catch (ExpressionTokenizeException | ExpressionParseException error) {
        createAnnotationForError(error, holder, totalOffset);
        return;
      }
    }

    else if (type == CMMarkerType.MARKUP) {
      try {
        MarkupParser.parse(inputView, BuiltInTagRegistry.INSTANCE, tokenOutput);
      } catch (MarkupParseException error) {
        createAnnotationForError(error, holder, totalOffset);
        return;
      }
    }

    else
      throw new IllegalArgumentException("Unaccounted-for marker-type: " + type);

    for (var token : tokenOutput.getResult())
      createAnnotationsForToken(token, holder, totalOffset);
  }

  private void createAnnotationForError(ErrorProvider errorProvider, AnnotationHolder holder, int offset) {
    var position = errorProvider.getErrorPosition() + offset;
    var message = errorProvider.getErrorMessage();

    holder.newAnnotation(HighlightSeverity.ERROR, message)
      .range(TextRange.create(position, position))
      .gutterIconRenderer(new ErrorTooltipGutterRenderer(message))
      .create();
  }

  private void createAnnotationsForToken(HierarchicalToken token, AnnotationHolder holder, int offset) {
    var range = TextRange.create(token.value.startInclusive + offset, token.value.endExclusive + offset);

    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(range)
      .textAttributes(CMColorScheme.forType(token.type))
      .create();

    var children = token.getChildren();

    if (children != null) {
      for (var childToken : children)
        createAnnotationsForToken(childToken, holder, offset);
    }
  }
}
