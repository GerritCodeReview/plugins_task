package com.googlesource.gerrit.plugins.names_factory_provider;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.server.DynamicOptions;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {
  @Override
  protected void configure() {
    bind(DynamicOptions.DynamicBean.class)
        .annotatedWith(Exports.named("foobar_provider"))
        .to(FooBarProvider.class);
  }
}
