// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.task;

import java.nio.file.Paths;
import java.util.NoSuchElementException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.jgit.errors.ConfigInvalidException;

/** This class is used by TaskExpression to decode the task from task reference. */
public class TaskReference {
  protected FileKey currentFile;
  protected String reference;

  public TaskReference(FileKey originalFile, String reference) {
    currentFile = originalFile;
    this.reference = reference.trim();
    if (reference.isEmpty()) {
      throw new NoSuchElementException();
    }
  }

  public TaskKey getTaskKey() throws ConfigInvalidException {
    TaskKey.Builder builder = new TaskKey.Builder(currentFile);
    ParseTreeWalker walker = new ParseTreeWalker();
    try {
      walker.walk(new TaskReferenceListener(builder), parse());
    } catch (RuntimeConfigInvalidException e) {
      throw e.checkedException;
    }
    return builder.buildTaskKey();
  }

  protected ParseTree parse() {
    Lexer lexer = new TaskReferenceLexer(new ANTLRInputStream(reference));
    lexer.removeErrorListeners();
    lexer.addErrorListener(TaskReferenceErrorListener.INSTANCE);
    return new TaskReferenceParser(new CommonTokenStream(lexer)).reference();
  }

  protected static class TaskReferenceErrorListener extends BaseErrorListener {
    protected static final TaskReferenceErrorListener INSTANCE = new TaskReferenceErrorListener();

    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer,
        Object offendingSymbol,
        int line,
        int charPositionInLine,
        String msg,
        RecognitionException e) {
      throw new NoSuchElementException();
    }
  }

  protected class TaskReferenceListener extends TaskReferenceBaseListener {
    TaskKey.Builder builder;

    TaskReferenceListener(TaskKey.Builder builder) {
      this.builder = builder;
    }

    @Override
    public void enterAbsolute(TaskReferenceParser.AbsoluteContext ctx) {
      builder.setAbsolute();
    }

    @Override
    public void enterRelative(TaskReferenceParser.RelativeContext ctx) {
      try {
        builder.setPath(
            ctx.dir().stream()
                .map(dir -> Paths.get(dir.NAME().getText()))
                .reduce(Paths.get(""), (a, b) -> a.resolve(b))
                .resolve(ctx.NAME().getText()));
      } catch (ConfigInvalidException e) {
        throw new RuntimeConfigInvalidException(e);
      }
    }

    @Override
    public void enterReference(TaskReferenceParser.ReferenceContext ctx) {
      builder.setTaskName(ctx.TASK().getText());
    }

    @Override
    public void enterFile_path(TaskReferenceParser.File_pathContext ctx) {
      if (ctx.absolute() == null && ctx.relative() == null) {
        try {
          builder.setRefRootFile();
        } catch (ConfigInvalidException e) {
          throw new RuntimeConfigInvalidException(e);
        }
      }
    }
  }
}
