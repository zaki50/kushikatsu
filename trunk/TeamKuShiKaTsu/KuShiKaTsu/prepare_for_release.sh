#!/bin/sh
set -x

# findbugs 関連の記述をコードから取り除く
find src/ -name \*.java -exec sed -e 's/^import edu\.umd\.cs\.findbugs\.annotations\.[^;]*;$//' -e 's/@DefaultAnnotation(NonNull.class)//' -e 's/@CheckForNull//' -i "" {} \;

# findbugs 関連の jar を classpath から取り除く
sed -e 's/<classpathentry kind="lib" path="libs\/findbugs\/[^"]*"\/>//' -i "" .classpath

# debuggable を false にする
sed -e 's/android:debuggable="true"/android:debuggable="false"/' -i "" AndroidManifest.xml
