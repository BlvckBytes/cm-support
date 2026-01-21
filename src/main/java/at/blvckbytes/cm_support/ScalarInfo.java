package at.blvckbytes.cm_support;

public record ScalarInfo(
  CMMarkerType type,
  int startOffset,
  String contents
) {}
