package at.blvckbytes.cm_support;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CMYamlScalarAnnotator extends ExternalAnnotator<CollectedInformation, AnnotationInformation> {

  @Override
  public @Nullable CollectedInformation collectInformation(@NotNull PsiFile file) {
    return new CollectedInformation()
      .walkAndCollect(file)
      .getIfNotEmpty();
  }

  @Override
  public @Nullable AnnotationInformation doAnnotate(CollectedInformation info) {
    return new AnnotationInformation()
      .parseCollectedInfo(info);
  }

  @Override
  public void apply(@NotNull PsiFile file, AnnotationInformation annotationInfo, @NotNull AnnotationHolder holder) {
    for (var tokenAnnotation : annotationInfo.tokenAnnotations()) {
      holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .range(tokenAnnotation.range())
        .textAttributes(tokenAnnotation.attributesKey())
        .create();
    }

    for (var parserError : annotationInfo.parserErrors()) {
      holder.newAnnotation(HighlightSeverity.ERROR, parserError.message())
        .range(TextRange.create(parserError.position(), parserError.position()))
        .gutterIconRenderer(new ErrorTooltipGutterRenderer(parserError.message()))
        .create();
    }
  }
}
