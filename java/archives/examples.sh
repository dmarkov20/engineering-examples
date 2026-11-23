#!/bin/sh

usage() {
    printf '%s\n' 'Usage: java/archives/examples.sh run|test [all|jdk-zip|commons-zip|tar-gz]'
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
    all) set -- jdk-zip commons-zip tar-gz ;;
    jdk-zip|commons-zip|tar-gz) set -- "$2" ;;
    *) usage >&2; exit 2 ;;
esac

archives_dir=$(CDPATH= cd "$(dirname "$0")" && pwd -P) || exit 1
cd "$archives_dir" || exit 1
if [ -n "${JAVA_HOME-}" ]; then
    PATH="$JAVA_HOME/bin:$PATH"
    export PATH
fi
maven=mvn
if ! command -v mvn >/dev/null 2>&1 && [ -x ../../.toolchains/maven/bin/mvn ]; then
    maven="$archives_dir/../../.toolchains/maven/bin/mvn"
fi

work=$(mktemp -d "${TMPDIR:-/tmp}/java-archives.XXXXXXXX") || exit 1
work=$(CDPATH= cd "$work" && pwd -P) || exit 1
classes="$work/classes"
mkdir "$classes" || exit 1
java -version || exit 1
printf 'umask: %s\nWork directory: %s\n' "$(umask)" "$work"
javac --release 21 -encoding UTF-8 -d "$classes" shared/src/main/java/examples/archives/*.java || exit 1
java -cp "$classes" examples.archives.ArchiveFixture "$work/source" || exit 1

run_jdk_example() (
    main=JdkZip
    set -- jdk-zip/JdkZip.java
    if [ "$action" = test ]; then
        main=JdkZipTest
        set -- "$@" jdk-zip/JdkZipTest.java
    fi
    javac --release 21 -encoding UTF-8 -cp "$classes" -d "$classes" "$@" &&
        java -ea "-Darchives.work=$work" -cp "$classes" "examples.archives.$main"
)

run_maven_example() (
    command -v "$maven" >/dev/null 2>&1 || {
        printf '%s\n' 'Put Maven 3.9.x on PATH.' >&2
        exit 1
    }
    case $1 in
        commons-zip) main=CommonsZip ;;
        tar-gz) main=TarGz ;;
    esac
    set -- -B -ntp "-Darchives.work=$work" -pl "$1" -am
    case $action in
        run) "$maven" "$@" -q "-Dexec.mainClass=examples.archives.$main" compile exec:java ;;
        test) "$maven" "$@" test ;;
    esac
)

result=0
for module in "$@"; do
    printf '\n== %s (%s) ==\n' "$module" "$action"
    case $module in
        jdk-zip) run_jdk_example || result=1 ;;
        *) run_maven_example "$module" || result=1 ;;
    esac
done
exit "$result"
