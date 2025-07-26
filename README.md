# Auto Collapse Generics

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/org.macrobit.auto-collapse-generics.svg)](https://plugins.jetbrains.com/plugin/auto-collapse-generics)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/org.macrobit.auto-collapse-generics.svg)](https://plugins.jetbrains.com/plugin/auto-collapse-generics)

An IntelliJ IDEA plugin that automatically collapses verbose generic declarations in Java class headers and method parameters when a file is opened.

## ✨ Features

- Auto-collapses:
  - Generic type declarations in class headers
  - `extends` and `implements` generic arguments
  - Generic constructor parameters

### Before

```java
public class ComplexService<
        InputType,
        OutputType,
        Transformer extends BaseTransformer<InputType, OutputType>,
        Validator extends RuleSet<InputType>
        > extends AbstractService<InputType, OutputType, Transformer> {

  public ComplexService(
          ServiceContext<InputType, OutputType> context,
          Transformer transformer,
          Validator validator
  ) {
    super(context, transformer);
  }
}
```

### Before

```java
public class ComplexService<...> extends AbstractService<...> {

  public ComplexService(
          ServiceContext<...> context,
          Transformer transformer,
          Validator validator
  ) {
    super(context, transformer);
  }
}
```
