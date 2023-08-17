package com.googlesource.gerrit.plugins.names_factory_provider;

import com.googlesource.gerrit.plugins.task.extensions.PluginProvidedTaskNamesFactory;
import java.util.List;

public class FooBarProvider implements PluginProvidedTaskNamesFactory {
  @Override
  public List<String> getNames(List<String> args) {
    return List.of("foo", "bar");
  }
}
