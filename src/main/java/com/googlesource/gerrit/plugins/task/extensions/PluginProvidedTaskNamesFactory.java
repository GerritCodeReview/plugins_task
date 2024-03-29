// Copyright (C) 2024 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.task.extensions;

import com.google.common.reflect.Reflection;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.server.DynamicOptions.DynamicBean;
import com.google.gerrit.server.query.change.ChangeData;
import java.lang.reflect.Method;
import java.util.List;

public interface PluginProvidedTaskNamesFactory extends DynamicBean {
  static PluginProvidedTaskNamesFactory getProxyInstance(
      DynamicMap<DynamicBean> dynamicBeans, String pluginName, String exportName)
      throws IllegalArgumentException {
    Object bean = dynamicBeans.get(pluginName, exportName);
    if (bean == null) {
      throw new IllegalArgumentException(
          String.format(
              "provider '%s' not found. Is plugin '%s' installed?", exportName, pluginName));
    }
    return getProxyInstance(PluginProvidedTaskNamesFactory.class, bean);
  }

  static <T> T getProxyInstance(Class<T> classz, Object bean)
      throws ClassCastException, IllegalArgumentException {
    return Reflection.newProxy(
        classz,
        (Object proxy, Method method, Object[] args) ->
            bean.getClass()
                .getMethod(method.getName(), method.getParameterTypes())
                .invoke(bean, args));
  }

  List<String> getNames(ChangeData change, List<String> args) throws Exception;
}
