package at.blvckbytes.cm_support;

public record ParserError(
  String message,
  int position
) {}
