package at.blvckbytes.cm_support;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

public class ErrorTooltipGutterRenderer extends GutterIconRenderer {

  private final String tooltipText;

  public ErrorTooltipGutterRenderer(String tooltipText) {
    this.tooltipText = tooltipText;
  }

  @Override
  public @NotNull Icon getIcon() {
    return AllIcons.General.Error;
  }

  @Override
  public @NotNull String getTooltipText() {
    return tooltipText;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ErrorTooltipGutterRenderer that = (ErrorTooltipGutterRenderer) o;
    return Objects.equals(tooltipText, that.tooltipText);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tooltipText);
  }
}