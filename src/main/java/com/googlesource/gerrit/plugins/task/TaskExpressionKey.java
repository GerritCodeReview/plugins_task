package com.googlesource.gerrit.plugins.task;

import com.google.auto.value.AutoValue;

/** A key for TaskExpression. */
@AutoValue
public abstract class TaskExpressionKey {
  public static TaskExpressionKey create(FileKey file, String expression) {
    return new AutoValue_TaskExpressionKey(file, expression);
  }

  public abstract FileKey file();

  public abstract String expression();
}
