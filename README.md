

#### Compile running all the tests  **Import add -D{vendor/profile}**
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0  -Doracle
```
#### Compile test running skip tests **Import add -D{vendor/profile}** lazy.initialization to avoid connection issues
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0  -Dmunit.disable.lazy.initialization=false  -Doracle
```
#### Compile test running skip tests **Import add -D{vendor/profile}**
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0  -Dmunit.disable.lazy.initialization=false -DskipTest  -Doracle
```
#### Compile test running  MTF's test **Import add -D{vendor/profile}**
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0  -Dmunit.disable.lazy.initialization=false -Dtest=none  -DfailIfNoTests=false  -Doracle
```
#### Compile test running  MTF's test **Import add -D{vendor/profile}** and skip tita
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0  -Dmunit.disable.lazy.initialization=false -Dtest=none  -DfailIfNoTests=false  -Doracle -DskipTita
```
### Remote Debug 
```
mvn clean install  -DruntimeProduct=MULE_EE -DruntimeVersion=4.3.0     -Dmunit.disable.lazy.initialization=false -Dtest=none  -DfailIfNoTests=false -DoracleTestsFolder=oracle/arrays-and-struct-source-oracle-test-case.xml -Doracle -Dmunit.debug=true 
```