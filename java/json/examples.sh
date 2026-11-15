#!/bin/sh

usage() {
    printf '%s\n' 'Usage: java/json/examples.sh run|test [all|nashorn-jdk8|nashorn-standalone|jackson|gson|json-p|jep540]'
}

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

case ${1-} in
    run|test) action=$1 ;;
    -h|--help) usage; exit 0 ;;
    *) usage >&2; exit 2 ;;
esac
if [ "$#" -gt 2 ]; then
    usage >&2
    exit 2
fi

case ${2-all} in
    all) set -- nashorn-jdk8 nashorn-standalone jackson gson json-p jep540 ;;
    nashorn-jdk8|nashorn-standalone|jackson|gson|json-p|jep540) set -- "$2" ;;
    *) usage >&2; exit 2 ;;
esac

json_dir=$(CDPATH= cd "$(dirname "$0")" && pwd -P) || exit 1

run_maven_example() (
    command -v mvn >/dev/null 2>&1 || fail 'Put Maven 3.9.x on PATH.'
    cd "$json_dir/$1" || exit 1
    case $action in
        run) mvn -B -ntp -q compile exec:java ;;
        test) mvn -B -ntp test ;;
    esac
)

run_jdk_example() (
    module=$1
    case $module in
        nashorn-jdk8)
            home=${JDK8_HOME:?Set JDK8_HOME to a JDK 8u60 or later in the JDK 8 line.}
            set --
            ;;
        jep540)
            home=${JDK28_HOME:?Set JDK28_HOME to OpenJDK 28-ea+23-1886.}
            set -- --add-modules jdk.incubator.json
            ;;
    esac

    bin=$(CDPATH= cd "$home/bin" && pwd -P) || exit 1
    [ -x "$bin/java" ] && [ -x "$bin/javac" ] || fail "Missing java or javac in $bin"
    version=$("$bin/java" -version 2>&1) || fail "$version"
    case $module in
        nashorn-jdk8)
            case $version in
                *'version "1.8.0_'*) ;;
                *) fail "nashorn-jdk8 requires JDK 8. Found $version" ;;
            esac
            ;;
        jep540)
            case $version in
                *'(build 28-ea+23-1886)'*) ;;
                *) fail "jep540 requires OpenJDK 28-ea+23-1886. Found $version" ;;
            esac
            ;;
    esac

    cd "$json_dir/$module" || exit 1
    mkdir -p out || exit 1
    case $action in
        run)
            "$bin/javac" "$@" -encoding UTF-8 -d out JsonExample.java &&
                "$bin/java" "$@" -cp out JsonExample
            ;;
        test)
            "$bin/javac" "$@" -encoding UTF-8 -d out JsonExample.java JsonExampleTest.java &&
                "$bin/java" "$@" -ea -cp out JsonExampleTest
            ;;
    esac
)

result=0
for module in "$@"; do
    printf '\n== %s (%s) ==\n' "$module" "$action"
    case $module in
        nashorn-jdk8|jep540) run_jdk_example "$module" || result=1 ;;
        *) run_maven_example "$module" || result=1 ;;
    esac
done
exit "$result"
