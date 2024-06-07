# Data Localization

This section discusses the approach to localizing package data.

## Package Version Localization

The payload of the HPKG (package binary) or HPKR (repository catalog) can contain localizations such as;

- Title
- Summary
- Description

These pieces of data may be conveyed in one or more languages, but data must be present for the English language to provide a fallback.  These localizations are effectively assigned to a specific version of a package because they are transported with the package data and so are referred to here are "package version localizations".  Package version localizations are not able to be edited within the web application; the only means by which they can be loaded into the application is by importing the data from a repository.

## Package / Fallback Localization

A human operator is able to edit a set of localizations for an package without consideration of the specific version of the package.  These localizations can be provided for any language and cover the same items as the package version localization; title, summary and description.  A user interface is provided within the web application to edit these values.  These localizations are a fallback for when localization data is not present in the package data payload.

## Choosing Localization Items

For a given localization item (eg; summary) for a given package version for display purposes, the following precedence applies and the first non-NULL value will be used.

- The value on the package version in the user's chosen language
- The value on the package in the user's chosen language
- The value on the package version in English
- The value on the package in English