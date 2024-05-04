@js-joda/extra Changelog
=========

### Later version

For later versions check the global [`CHANGELOG.md`](../../CHANGELOG.md) file in the root folder.

### 0.7.0

* [#574](https://github.com/js-joda/js-joda/pull/574) Reverting babel targets for UMD ([@pithu](https://github.com/pithu))

### 0.6.0

* [#567](https://github.com/js-joda/js-joda/pull/567) Remove generated distributions files from git ([@pithu](https://github.com/pithu))
* [#564](https://github.com/js-joda/js-joda/pull/564) Fix travis for PR's from forks ([@pithu](https://github.com/pithu))

### 0.5.0

* Upgrade dependencies #555 by @pithu
* Change @babel/preset-env targets, fix IE11 issues #555 by @pithu

### 0.4.0

 * Improve TS declarations with no breaking change #357 InExtremaRes/ts-declarations

### 0.3.0

* Add TypeScript declarations for extras package #339

### 0.2.1 

* dependency updates

### 0.2.0

#### public api

* hide `use(plug)` from public api, that is done automatically from now on.
* Interval must be imported from @js-joda/extra, it's not added to js-joda anymore. 

### 0.1.1

* dependency updates

### 0.1.0

* add missing method in `Interval`
* update eslint rules and adapt styling
* replace all `var`s with `const`/`let`
* update dependencies

### 0.0.3

* add and use babel-plugin-add-module-exports to get rid of having to use `.default` when `.use`ing @js-joda/extra

### 0.0.2

* fix dependencies, build as plugin
* provide examplesnpm r

### 0.0.1

initial release, basic setup, only `Interval` ported 
