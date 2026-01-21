package at.blvckbytes.cm_support;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CollectedInformation {

  private enum WalkFlag {
    RESET_MARKER_AFTER_ENCOUNTERING_KEY_VALUE
  }

  public final List<ScalarInfo> scalarInfos;
  private final EnumSet<WalkFlag> walkFlags;

  public CollectedInformation() {
    this.scalarInfos = new ArrayList<>();
    this.walkFlags = EnumSet.noneOf(WalkFlag.class);
  }

  public @Nullable CollectedInformation getIfNotEmpty() {
    if (scalarInfos.isEmpty())
      return null;

    return this;
  }

  public CollectedInformation walkAndCollect(PsiFile psiFile) {
    walkFlags.clear();
    walkRecursively(psiFile, null);
    return this;
  }

  private void walkRecursively(PsiElement psiElement, CMMarkerType effectiveMarker) {
    // Comments are not part of PsiElement#getChildren, so we need to locate them manually,
    // by walking up the chain of siblings up until we hit another non-comment node.
    PsiElement currentElement = psiElement;

    while ((currentElement = currentElement.getPrevSibling()) != null) {
      if (currentElement instanceof PsiComment comment) {
        var detectedMarker = CMMarkerType.byComment(comment);

        if (detectedMarker != null) {
          effectiveMarker = detectedMarker != CMMarkerType.BLOCK_OUT ? detectedMarker : null;
          break;
        }

        continue;
      }

      if (currentElement instanceof LeafPsiElement)
        continue;

      // Neither a comment nor a leaf (whitespace, etc.); hit another node, so stop here.
      break;
    }

    if (psiElement instanceof YAMLKeyValue keyValue) {
      var detectedMarker = CMMarkerType.byKey(keyValue.getKeyText());

      if (detectedMarker != null)
        effectiveMarker = detectedMarker;
    }

    if (psiElement instanceof YAMLDocument)
      walkFlags.add(WalkFlag.RESET_MARKER_AFTER_ENCOUNTERING_KEY_VALUE);

    if (effectiveMarker != null && psiElement instanceof YAMLScalar scalar) {
      var scalarInfo = createScalarInfo(scalar, effectiveMarker);

      if (scalarInfo != null)
        scalarInfos.add(scalarInfo);
    }

    for (var child : psiElement.getChildren()) {
      walkRecursively(child, effectiveMarker);

      if (child instanceof YAMLKeyValue && walkFlags.contains(WalkFlag.RESET_MARKER_AFTER_ENCOUNTERING_KEY_VALUE)) {
        walkFlags.remove(WalkFlag.RESET_MARKER_AFTER_ENCOUNTERING_KEY_VALUE);
        effectiveMarker = null;
      }
    }
  }

  private @Nullable ScalarInfo createScalarInfo(YAMLScalar scalar, CMMarkerType type) {
    var text = scalar.getText();

    if (text.isEmpty())
      return null;

    var firstChar = text.charAt(0);

    var contents = text;
    int charOffset = 0;

    // "Keep newlines" block-scalar style
    if (firstChar == '|') {
      // There are optional block-chomping modifier-chars that could follow (+/-), but since there
      // is absolutely no need to use them with ComponentMarkup, I will not bother supporting them.
      if (text.length() == 1 || text.charAt(1) != '\n')
        return null;

      contents = text.substring(2);
      charOffset = 2;
    }

    // "Replace newlines with spaces" block-scalar style; again, ComponentMarkup has no use-case for this mode.
    else if (firstChar == '>')
      return null;

    else if (firstChar == '\'' || firstChar == '\"') {
      contents = text.substring(1, text.length() - 1);
      charOffset = 1;
    }

    var totalOffset = scalar.getTextRange().getStartOffset() + charOffset;

    return new ScalarInfo(type, totalOffset, contents);
  }
}
