#!/usr/bin/env bash

#sbt Universal/packageBin
target_base="target/universal"
base_name="tong-wen-0.1.0-SNAPSHOT"
project_name="tong-wen"
jar_name="$project_name.$base_name.jar"
target_dir="$target_base/$base_name"

if [ -d "$target_dir" ]; then
  rm -rf $target_dir
fi

echo "will unzip "$target_base/"${base_name}.zip"
unzip -d $target_base $target_base/"${base_name}.zip"
mv $target_base/$base_name/lib/$jar_name $target_base/$base_name

commit_hash=$(git rev-parse --short HEAD)
dest_image=10.1.18.188:30002/library/tong-wen:$commit_hash
docker build -t tong-wen:latest

docker tag tong-wen:latest $dest_image
docker push $dest_image