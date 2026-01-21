package at.blvckbytes.cm_support;

import com.intellij.psi.PsiComment;
import org.jetbrains.annotations.Nullable;

public enum CMMarkerType {
  MARKUP("cm-support Markup"),
  EXPRESSION("cm-support Expression"),
  BLOCK_OUT("!cm-support"),
  ;

  public final String marker;

  CMMarkerType(String marker) {
    this.marker = marker;
  }

  public static @Nullable CMMarkerType byComment(PsiComment comment) {
    var commentContents = comment.getText();

    if (!commentContents.startsWith("#"))
      return null;

    commentContents = commentContents.substring(1).trim();

    for (var type : values()) {
      if (type.marker.equalsIgnoreCase(commentContents))
        return type;
    }

    return null;
  }
}
