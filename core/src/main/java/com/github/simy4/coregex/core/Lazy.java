/*
 * Copyright 2021-2023 Alex Simkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.simy4.coregex.core;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Supplier;

@SuppressWarnings("serial")
final class Lazy<T> implements Supplier<T>, Serializable {
  private static final long serialVersionUID = 1L;

  private transient volatile Supplier<? extends T> supplier;
  private T value;

  public Lazy(Supplier<? extends T> supplier) {
    this.supplier = requireNonNull(supplier, "supplier");
  }

  @Override
  public T get() {
    return (supplier == null) ? value : doGet();
  }

  private synchronized T doGet() {
    Supplier<? extends T> supplier = this.supplier;
    if (supplier != null) {
      value = supplier.get();
      this.supplier = null;
    }
    return value;
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    get(); // resolve value
    oos.defaultWriteObject();
  }
}
