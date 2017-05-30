The source for this this project is licensed under the
[Two-Clause BSD License](LICENSE.md) with the exception of the following files
as they originate from external projects:

- [`jars/sbt-launch.jar`](../jars/sbt-launch.jar)
  - _External Project_: SBT 0.13.13 <http://www.scala-sbt.org/0.13/docs/Manual-Installation.html>
  - _Release URL_: <https://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.13/sbt-launch.jar>
  - _License File_: [`licenses/LICENSE-sbt`](LICENSE-sbt)
  - _License Type_: Three-clause BSD License
  - _License Origin_: <https://github.com/sbt/sbt/blob/v0.13.13/LICENSE>

- [`resources/rt.jar`](../resources/rt.jar)
  - _External Project_: OpenJDK 1.7.0.85 from CentOS 7.1.1503 <http://vault.centos.org/7.1.1503/updates/x86_64>
  - _Release URL_: <http://vault.centos.org/7.1.1503/updates/x86_64/Packages/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.rpm>
  - _License File_: [`licenses/LICENSE-openjdk`](LICENSE-openjdk)
  - _License Type_: GNU General Public License (GPL) version 2
  - _License Origin_: `usr/lib/jvm/java-1.7.0-openjdk-1.7.0.85-2.6.1.2.el7_1.x86_64/LICENSE` in the `openjdk-headless` RPM

- [`src/main/resources/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.zip`](../src/main/resources/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.zip)
  - _External Project_: OpenJDK 1.7.0.85 from CentOS 7.1.1503 <http://vault.centos.org/7.1.1503/updates/x86_64>
  - _Release URL_: <http://vault.centos.org/7.1.1503/updates/x86_64/Packages/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.rpm>
  - _License File_: [`licenses/LICENSE-openjdk-third-party`](LICENSE-openjdk-third-party)
  - _License Type_: Various
  - _License Origin_: `usr/lib/jvm/java-1.7.0-openjdk-1.7.0.85-2.6.1.2.el7_1.x86_64/THIRD_PARTY_README` in the `openjdk-headless` RPM

Unfortunately, binaries cannot be distributed as they contain parts of
`tools.jar` from the building platform's Java Development Kit (JDK).  If at
some point we figure out a better packaging that avoid this, we will be able
to provide binary distributions.
