#!/usr/bin/env bash


while [[ $1 = -* ]]; do
  case $1 in
    -d) DEBUG=1 ;;
  esac
  shift
done
ACTION=$1
shift

case $ACTION in
  compile)
  javac -cp lib/jmh-core-1.19.jar:lib/jmh-generator-annprocess-1.19.jar: nefk/*.java
  mv BenchmarkList META-INF/
  mv CompilerHints META-INF/
  ;;
  jar)
  jar cmvf META-INF/MANIFEST.MF nefk.jar nefk META-INF/*
  mv nefk.jar lib
  ;;
  run)
java -XX:+UnlockDiagnosticVMOptions  -XX:CompileCommand=print,nefk.Switch::* -classpath lib/commons-math3-3.2.jar:lib/jmh-core-1.19.jar:lib/jmh-generator-annprocess-1.19.jar:lib/jopt-simple-4.6.jar:lib/nefk.jar org.openjdk.jmh.Main
  ;;
esac

