# SVFA (Sparse Value Flow Analysis) implementation based on Soot

This is a scala implementation of a framework that builds a sparse-value flow graph using Soot.

## Status

   * experimental

## Usage

   * clone this repository or download an stable release
   * you will need to add a github token to your **~/.gitconfig**.
     ```
     [github]
             token = TOKEN
     ```
   * build this project using sbt (`sbt compile test`)
   * publish the artifact as a JAR file in your m2 repository (`sbt publish`)
   * create a dependency to the svfa-scala artifact in your maven project. 

```{xml}
<dependency>	
  <groupId>br.unb.cic</groupId>
  <artifactId>svfa-scala_2.12</artifactId>
  <version>0.0.2-SNAPSHOT</version>
 </dependency>
```

   * implement a class that extends the `JSVFA class` (see some examples in the scala tests). you must provide implementations to the following methods
      * `getEntryPoints()` to set up the "main" methods. This implementation must return a list of Soot methods
      * `sootClassPath()` to set up the soot classpath. This implementation must return a string
      * `analyze(unit)` to identify the type of a node  (source, sink, simple node) in the graph; given a statement (soot unit)


## Dependencies

This project use some of the [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) test cases. The FlowDroid test cases in `src/test/java/securibench` are under [LGPL-2.1](https://github.com/secure-software-engineering/FlowDroid/blob/develop/LICENSE) license.

### dex2jar

There is an [open issue](https://github.com/soot-oss/SootUp/issues/669) due to dex2jar being flaky.
This dependency is needed for _sootup.java.bytecode_ and gave me only a _slight_ panic attack.

The only way I found to build this was:
- Clone project https://github.com/ThexXTURBOXx/dex2jar
- Checkout the tag needed (for sootup.java.bytecode 1.1.2 it was v61)
- Run ``./gradlew publishToMavenLocal`` as is
- Change build.gradle from dex2jar to
 ```
group = 'com.github.ThexXTURBOXx'
version = 'v61'
```
- Run ``./gradlew publishToMavenLocal`` again
- Navigate to where the package was deployed to the local maven cache, somewhere like `C:\Users\USERNAME\.m2\repository\com\github\ThexXTURBOXx`
- Copy everything inside
- Create a new folder called `dex2jar`
- Paste everything inside this new folder.

Yes, by the end of this process there should be 3 copies of the library.
