# Scala-based analyzer framework for shimple

Early work in progress. First target is to get control and data flows graphs in a format that Needle can use.

## Next steps

1. Remove hardcoded path value from main, take command line arguments.

2. Flesh out denotable values.

```
D = Set[Value]
Value = Basic + ObjectPointer
ObjectPointer = ClassName + BasePointer
```

3. Add continuations:

```
Kont = KontStore x KontAddr
KontStore = Map[KontAddr, Set[Kont]]
```

## Getting Shimple

An example:

```
sbt 'runMain soot.main -cp to-analyze -pp -f S BubbleSort'
```

## Running Analyzer

Example:

```
sbt
```

```
run classDirectory className methodName
```
