#!/bin/sh
#set -x

force=0
if [ x"$1" = x-f ]; then
  force=1
fi

projectdir=$(dirname $0)
echo "changing directory to ${projectdir}" >&2
pushd "${projectdir}" > /dev/null

echo "creating destination directory" >&2

# 念のため android:versionName が含まれていることを確認
if ! grep -q android:versionName AndroidManifest.xml; then
  echo "failed to get version name. exit." >&2
  popd > /dev/null
  exit 1
fi

# android:versionName の記述からバージョンを取得する
versionName=$(grep android:versionName AndroidManifest.xml | sed -e 's/.*android:versionName="\([0-9][0-9.]*[0-9]\)".*/\1/')
if [ x"$versionName" = x ]; then
  echo "failed to get version name. exit." >&2
  popd > /dev/null
  exit 1
fi

#ビルドした成果物の格納先
dest="released_binaries/${versionName}"
if [ -e "${dest}" ]; then
  if [ $force -eq 1 ]; then
    rm -rf "${dest}/*"
  else
    echo "destination directory already exists. Is versionName correct?" >&2
    echo "add -f to arguments if you intend to overwrite." >&2
    popd > /dev/null
    exit 1
  fi
fi
mkdir -p "${dest}"
echo "destinationdirectory created: ${dest}" >&2

echo "invoking 'ant clean'. output is written to ant_clean.log" >&2
if ! ant clean > ant_clean.log; then
  echo "failed to clean project. exit." >&2
  cat ./ant_clean.log
  popd > /dev/null
  exit 1
fi
echo "invoking 'ant release'. output is written to ant_release.log" >&2
if ! ant release | tee ./ant_release.log | grep "Please enter"; then
  echo "failed to build release binary. exit." >&2
  cat ./ant_release.log
  popd > /dev/null
  exit 1
fi

echo "copying artifacts to ${dest}" >&2
cp -a bin/*-release.apk "${dest}/"
cp -a bin/proguard "${dest}/"

popd > /dev/null
exit 0
