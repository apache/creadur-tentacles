# Apache Creadur Tentacles

[![ASF Build Status](https://ci-builds.apache.org/job/Creadur/job/Creadur-Tentacles/badge/icon)](https://ci-builds.apache.org/job/Creadur/job/Creadur-Tentacles/)
[![Travis Build Status](https://app.travis-ci.com/apache/creadur-tentacles.svg?branch=master)](https://app.travis-ci.com/apache/creadur-tentacles)

# Running

The tool will download all the archives from a staging repo, unpack
them and create a little report of what is there.

    java -ea -jar apache-tentacles-0.1-SNAPSHOT.jar https://repository.apache.org/content/repositories/orgapacheopenejb-090

Assertions must be enabled.

The tool is not specific to maven and will simply recursively walk
the provided URL and download all files matching the following
pattern:

    .*\.(jar|zip|war|ear|rar|tar.gz)

Tar.gz files are downloaded though there is currently no support for
unpacking them.

# Output

Once the tool has run, the following files directories will exist:

    repo/
    content/
    archives.html
    licenses.html
    notices.html
    style.css
    org.apache.openejb.openejb-core.3.0.4.openejb-core-3.0.4.jar.licenses.html
    org.apache.openejb.openejb-core.3.0.4.openejb-core-3.0.4.jar.notices.html
    org.apache.openejb.openejb-standalone.3.0.4.openejb-standalone-3.0.4.zip.licenses.html
    org.apache.openejb.openejb-standalone.3.0.4.openejb-standalone-3.0.4.zip.notices.html
    org.apache.openejb.openejb-tomcat-webapp.3.0.4.openejb-tomcat-webapp-3.0.4.war.licenses.html
    org.apache.openejb.openejb-tomcat-webapp.3.0.4.openejb-tomcat-webapp-3.0.4.war.notices.html
    ...

## repo

The repo directory will contain the full set of binaries, unmodified.
Theoretically, this tool could also download and check signatures
though it does not do that now.

## content

The content directory will contain the unpacked version of the
downloaded binaries

So this file for example:

    repo/foo.zip

Will be unpacked at the following location:

    content/foo.zip.contents/
    content/foo.zip.contents/LICENSE
    content/foo.zip.contents/NOTICE
    content/foo.zip.contents/README.txt
    content/foo.zip.contents/lib/bar.jar

Unpacking is recursive, so any binaries contained in foo.zip will
also be unpacked.

    content/foo.zip.contents/lib/bar.jar
    content/foo.zip.contents/lib/bar.jar.contents/
    content/foo.zip.contents/lib/bar.jar.contents/LICENSE
    content/foo.zip.contents/lib/bar.jar.contents/NOTICE
    content/foo.zip.contents/lib/bar.jar.contents/README.txt
    content/foo.zip.contents/lib/bar.jar.contents/org/
    content/foo.zip.contents/lib/bar.jar.contents/org/bar/
    content/foo.zip.contents/lib/bar.jar.contents/org/bar/Some.class

## Reports

The "main" report is currently called `archives.html` and will list
all of the top-level binaires, their LICENSE and NOTICE files and any
LICENSE and NOTICE files of any binaries they may contain.

Validation of the output at this point is all still manual.  One of
the first improvements would be to automatically flag any binaries
that:

  - contain no LICENSE and NOTICE files
  - contain more than one LICENSE or NOTICE file

In this report, each binary will have three links listed after its
name '(licenses, notices, contents)'

### foo.zip.licenses.html

This page will display the full text of the LICENSE files included in
the binary.  There will be two sections **Declared** and
**Undeclared**

The Declared section lists the single LICENSE file that was supplied
by the binary itself.  As the tool works recursively, it will also
collect any LICENSE file text from any binaries contained in the
foo.zip.  Well call these "sub" LICENSES for simplicity.

Some attempt is made to figure out if the text from sub LICENSE files
are contained in the declared LICENSE file.  If the sub license text
is contained in the declared LICENSE file it is not listed as
Undeclared.

The matching is not complete or perfect, but does help in more quickly
seeing where there might be a missing LICENSE text that should be
declared.

### foo.zip.notices.html

Functions identical to the previously described LICENSE page with
identical matching.

Note on the code, this all could probably be abstracted.  We probably
don't need separate License and Notice classes.

### foo.zip.contents

The unpacked contents of the foo.zip as described above.  Can be nice
to be able to browse around the zip and look for any jars that might
have LICENSE or NOTICE requirements but were overlooked.

#  Future work

Overall it would be great if this tool could perform some validation

Existence of LICENSE/NOTICE files:
  - flag binaries that contain no LICENSE or NOTICE files
  - flag binaries that contain too many LICENSE or NOTICE files

Contents of LICENSE/NOTICE files:
  - better matching of missing license/notice text
  - look false license/notice text, text that applied to "sub"
    binaries once included in a binary, but are no longer present
