# NameMC API

NameMC (https://namemc.com) Java wrapper

Project is not likely to receive any more changes.

### Add to project

Gradle:

```groovy
repositories {
	jcenter()
}

dependencies {
	implementation "dev.jaqobb:namemcapi:2.0.7"
}
```

Gradle Kotlin DSL:

```kotlin
repository {
	jcenter()
}

dependencies {
	implementation("dev.jaqobb:namemcapi:2.0.7")
}
```

### Usage example

Create new `ProfileRepository` object by using:

```java
ProfileRepository profileRepository = new ProfileRepository();
```

or:

```java
ProfileRepository profileRepository = new ProfileRepository(duration, unit);
```

By calling default `ProfileRepository` constructor cached profiles will be valid for 5 minutes.

Create new `ServerRepository` object by using:

```java
ServerRepository serverRepository = new ServerRepository();
```

or:

```java
ServerRepository serverRepository = new ServerRepository(duration, unit);
```

By calling default `ServerRepository` constructor cached servers will be valid for 10 minutes.

Create new `NameMCAPI` object by using:

```java
NameMCAPI api = new NameMCAPI();
```

or:

```java
NameMCAPI api = new NameMCAPI(profileRepository, serverRepository);
```

By calling default `NameMCAPI` constructor default constructors of `ProfileRepository` and `ServerRepository` will be used.

The only method I think you should care about in both repositories is `cache`. This method allows you to cache profile or server (depends on the repository) or if the profile or server is already cached, is valid, and re-cache is not forced, get the requested profile or server. In case if any error occurs, `callback` allows you to get that error.

I fell like all public methods in `Friend`, `Profile` and `Server` classes are self-explanatory (due to their names), and it is not needed to explain them.
