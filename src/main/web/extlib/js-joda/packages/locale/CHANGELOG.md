@js-joda/locale Changelog
=========

### Later version

For later versions check the global [`CHANGELOG.md`](../../CHANGELOG.md) file in the root folder.

### 4.6.0

* [#578](https://github.com/js-joda/js-joda/pull/578) WeekFields export and typings ([@StrayAlien](https://github.com/StrayAlien))

### 4.5.0

* [#574](https://github.com/js-joda/js-joda/pull/574) Reverting babel targets for UMD ([@pithu](https://github.com/pithu))

### 4.4.0

* [#568](https://github.com/js-joda/js-joda/pull/568) Add russian locale prebuilt ([@pithu](https://github.com/pithu))

### 4.3.0

* [#567](https://github.com/js-joda/js-joda/pull/567) Remove generated distributions files from git ([@pithu](https://github.com/pithu))
* [#564](https://github.com/js-joda/js-joda/pull/564) Fix travis for PR's from forks ([@pithu](https://github.com/pithu))

### 4.2.1

* [#560](https://github.com/js-joda/js-joda/pull/560) Add RFC_1123_DATE_TIME to locale ts definition ([@pithu](https://github.com/pithu))

### 4.2.0

* Upgrade dependencies #555 by @pithu
* Change @babel/preset-env targets, fix IE11 issues #555 by @pithu
* Add new built-in formatter DateTimeFormatter.RFC_1123_DATE_TIME  #556 by @pithu

### 4.1.0

* Fix bug with GMT timezone parsing and improve caching #550 by @pithu

### 4.0.1

* Add new Locales to prebuilt packages #543 by @pithu

### 4.0.0 

Even this is a major release, it should not break anything. 
Main changes are typescript definition reorganisation for the Locale class 
and some added prebuilt Locale packages for browser usage.

* added TS Typings to locale packages in #389 by @InExtremaRes
* added Korean locale browser support in #529 by @leejaycoke
* added Japanese locale browser support in #491 by @ty-v1
* several dependency updates

### 3.2.2

* fix es5 issues

### 3.2.1

* add ro locales to prebuilt packages
* update dependencies

### 3.2.0 

* cache cldr operations #420 by @JKillian

### 3.1.1

* add fr-FR, sv, sv-SE locales to prebuilt packages

### 3.1.0

* update dependencies, update cldr-data to 36.0.0 for prebuilt packages

### 3.0.0

* Switch to latest @js-joda/core and @js-joda/timezone peer dependencies

### 2.0.1

* update README to reflect new package name `@js-joda/locale*`
* update dev dependencies

### 2.0.0

#### public API

introduced new plugin concept
 * Hide `use(plug)` concept from public api.
   The function for extending js-joda is not exported anymore.
   The code for extending js-joda `use(plug)` is not required anymore, because @js-joda/locale automaticaly extends
   js-joda when imported.
   However, using `Locale` now requires extracting it from the `@js-joda/locale` module instead of `js-joda`

* get rid of postinstall build

* add possibility to publish locale specific packages (e.g. @js-joda/locale_de, @js-joda/locale_en-US, ...)

* add prebuilt packages to main @js-joda/locale package (e.g. @js-joda/locale/dist/prebuilt/de/js-joda-locale, ...)

### 1.0.0

just bump the version

### 0.8.2

get rid of dependencies for postinstall package build, use `postinstall_build` to install the needed
dependencies only for the actual build

### 0.8.1

update package builder, use a webpack plugin to ignore unnecessary cldr-data files instead of `null-loader`

### 0.8.0

- update dependencies
- fix repository location in `package.json`
- increase testcoverage
- small bugfixes
- add build_package script and postinstall run to be able to build locale specific packages that 
  can be used directly

### 0.1.0

initial release 
