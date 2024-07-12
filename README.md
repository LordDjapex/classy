# Java Classy Version 0.1

**Java Classy** is a Java library designed to generate model and DTO classes based on your Database Model that you can then use within your runtime. Packages where you can find these classes are named classy.model and classy.dto respectively.


## Getting Started

### Add Dependency

To start using Java Classy, add the following dependency to your `pom.xml` or `build.gradle`:

#### Maven
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>java-classy</artifactId>
    <version>0.1</version>
</dependency>
```
### Gradle
```xml
implementation 'com.example:java-classy:0.1'
```

### Configuration:
In order to run this library, db_connect.properties is required to be present within your resources folder. The content within it should look like this
```xml
db.url=<DATABASE_URL>
db.schema=<SCHEMA>
db.username=<DB_USERNAME>
db.password=<DB_PASSWORD>
classy.annotateJPA=<Boolean value>
classy.generateDTOs=<Boolean value>
```

annotateJPA - If this parameter is set to true, your model classes will get annotated with JPA annotations

generateDTOs - If this parameter is set to true, alongside your model classes, you will have DTO representatives of these models as well, located within classy.dto package upon run time. 
generate Github markdown for this please

### Code Setup
In order to use this library, not much of code setup is required. Just annotate your main class with @GenerateModel annotation and it should generate code fine.
