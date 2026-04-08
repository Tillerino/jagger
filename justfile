mvn := `if command -v mvnd &> /dev/null; then echo mvnd; else echo mvn; fi`

updates-flags := "-q '-Dmaven.version.ignore=.*\\.Beta\\d*,.*\\.android\\d*,.*-M\\d' -Dversions.outputFile=updates.txt -Dversions.outputLineWidth=1000 -P release"

# lists all recipes
@recipes:
  just --list

# apply spotless format
format:
  {{mvn}} -q spotless:apply

test args="":
  {{mvn}} clean spotless:apply test {{args}}

test-single project args="":
  {{mvn}} clean spotless:apply test -pl :jagger-tests-{{project}} -am {{args}}

install-processor:
  {{mvn}} clean install -pl :jagger-processor -am

# show all available updates
updates:
  {{mvn}} versions:display-plugin-updates {{updates-flags}} && { grep -- "->" updates.txt */updates.txt */*/updates.txt | sed 's/\.\+/./g'; }
  {{mvn}} versions:display-property-updates {{updates-flags}} && { grep -- "->" updates.txt */updates.txt */*/updates.txt | sed 's/\.\+/./g'; }
  {{mvn}} versions:display-dependency-updates {{updates-flags}} && { grep -- "->" updates.txt */updates.txt */*/updates.txt | sed 's/\.\+/./g'; }
  rm updates.txt */updates.txt */*/updates.txt

# regenerate the TOC in README.md
readme-update-toc:
  docker run --rm -v .:/work --entrypoint=/bin/sh mkenney/npm -c 'npm config set unsafe-perm true; npm install -g markdown-toc; cd /work; for f in README.md docs/*; do markdown-toc -i $f; done'

readme-build-embedme:
  echo "FROM mkenney/npm" > /tmp/embedme.dockerfile
  echo "RUN wget https://github.com/Tillerino/embedme/tarball/master; tar -zxvf master; cd */; yarn --frozen-lockfile --non-interactive --no-progress; yarn build; npm install -g;" >> /tmp/embedme.dockerfile
  echo 'ENTRYPOINT [ "/bin/sh", "-c" ]' >> /tmp/embedme.dockerfile
  docker build -t embedme -f /tmp/embedme.dockerfile .

# check the snippets in all docs - fails if snippet would change
readme-check-snippets:
  just readme-build-embedme
  just format
  docker run --rm -v .:/work embedme 'cd /work; embedme --verify README.md docs/*'

# update the snippets in all docs
readme-update-snippets:
  just readme-build-embedme
  just format
  docker run --rm -v .:/work embedme 'cd /work; embedme README.md docs/*'

# check links in readme
readme-links:
  docker run -v .:/tmp:ro --rm -i ghcr.io/tcort/markdown-link-check:stable /tmp/README.md
  docker run -v .:/tmp:ro --rm -i ghcr.io/tcort/markdown-link-check:stable /tmp/docs

# estimate size of shaded libraries
shaded-sizes:
  #!/bin/sh
  cd jagger-tests
  for f in */target/jagger-tests-*.jar; do
    dir=$(dirname $f)
    name=$(basename $f)
    if [ -f $dir/original-$name ]; then
      # size of shaded jar
      size=$(du -b $f | cut -f1)
      # difference in KiB
      eff=$(($size / 1024))

      name=$(dirname $dir)
      name=${name#jagger-tests-}
      echo "$name: $eff KiB"
    fi
  done

# check if features and their test organization are aligned
check-feature-organization:
    #!/usr/bin/env python3
    import os
    import sys
    from pathlib import Path
    
    # Check if features and their tests are properly organized
    processor_features_dir = Path("jagger-processor/src/main/java/org/tillerino/jagger/processor/features")
    jackson_tests_dir = Path("jagger-tests/jackson/src/test/java/org/tillerino/jagger/tests/base/features")
    jackson_serde_dir = Path("jagger-tests/jackson/src/main/java/org/tillerino/jagger/tests/base/features")
    base_model_dir = Path("jagger-tests/base/src/main/java/org/tillerino/jagger/tests/model/features")
    
    # Get all feature classes
    feature_files = list(processor_features_dir.glob("*.java"))
    feature_names = [f.stem for f in feature_files]
    
    missing_tests = []
    missing_serde = []
    missing_models = []
    
    for feature_name in feature_names:
        # Check if test exists
        test_file = jackson_tests_dir / f"{feature_name}Test.java"
        if not test_file.exists():
            missing_tests.append(feature_name)
            
        # Check if serde exists
        serde_file = jackson_serde_dir / f"{feature_name}Serde.java"
        if not serde_file.exists():
            missing_serde.append(feature_name)
            
        # Check if model exists
        model_file = base_model_dir / f"{feature_name}Model.java"
        if not model_file.exists():
            missing_models.append(feature_name)
    
    # Report any missing files
    if missing_tests or missing_serde or missing_models:
        print("Feature organization issues found:")
        if missing_tests:
            print(f"  Missing tests: {', '.join(missing_tests)}")
        if missing_serde:
            print(f"  Missing serde: {', '.join(missing_serde)}")
        if missing_models:
            print(f"  Missing models: {', '.join(missing_models)}")
        sys.exit(1)
    else:
        print("All features are properly organized.")

release:
    # format so that we fail earlier if there are issues (release plugin will notice dirty working directory)
    just format
    # don't use mvnd here. no need to overoptimize
    mvn release:prepare -DtagNameFormat=@{project.version} '-Darguments=-Dmaven.build.cache.skipCache=true'
    mvn release:perform -P release '-Darguments=-Dmaven.build.cache.skipCache=true --projects jagger-processor --also-make'
