package at.blvckbytes.cm_support;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;

public record TokenAnnotation(
  TextRange range,
  TextAttributesKey attributesKey
) {}
