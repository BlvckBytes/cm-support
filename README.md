# cm-support

Adding syntax-highlighting- and error-reporting-support to IntelliJ for [ComponentMarkup](https://github.com/BlvckBytes/ComponentMarkup) by injecting additional annotations to scalar values within configuration-files.

## Overview

Currently, the plugin only supports YAML-files, but I am looking to expand on that limitation soon, seeing how I am myself trying to move away from that cursed language.

In order to receive syntax-highlighting, keys have to be marked, as the plugin has no way of knowing which keys will contain markup or expressions. Markers are inherited by scalars transitively, meaning that if you want to enable syntax-highlighting on a whole subtree of the file, adding the marker to its root will suffice. Inherited markers can also be blocked out by making use of the bang (`!`).

Example:

```yml
# cm-support Markup
myMarkups:
  first: '<&c>Hello, first!'
  second: '<&b>Hello, second!'
  third: '<&d>Hello, third!'
  # !cm-support
  noMarkup: 'I am a plain string'

# cm-support Expression
myExpression: '0..10'
```

While marker-comments like these are great for getting to know the plugin initially, they do tend to add a lot of visual clutter, so keys may also be suffixed with `_M` for markup and `_E` for expressions, as to reduce noise while still ensuring unambiguous highlighting. These suffixes also trickle down but personally, I'd really advise against that in mixed-key environments, making it easier for the user editing the config to know what's what by adding a suffix to each key that is running through `ComponentMarkup`'s parsers.

```yml
myMarkups:
  first_M: '<&c>Hello, first!'
  second_M: '<&b>Hello, second!'
  third_M: '<&d>Hello, third!'
  noMarkup: 'I am a plain string'

myExpression_E: '0..10'
```
