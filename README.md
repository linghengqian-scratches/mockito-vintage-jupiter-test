- Served at https://github.com/linghengqian/graalvm-trace-metadata-smoketest/issues/1
- Execute the following command to view the Log. Among them, the unit test processed
  by `org.mockito.junit.MockitoJUnitRunner` will not report an error, and the class processed
  by `org.mockito.junit.jupiter.MockitoExtension` adds three `org.mockito.Mockito.lenient` separately

```shell
cd /tmp
sudo apt install unzip zip curl sed -y
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 22.3.r17-grl
sdk use java 22.3.r17-grl

git clone git@github.com:linghengqian/mockito-vintage-jupiter-test.git
cd ./mockito-vintage-jupiter-test/
./gradlew clean test
```