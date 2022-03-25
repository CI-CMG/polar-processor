# polar-processor

The polar-processor splits JTS geometries across the meridian and anti-meridian near the Earth’s poles to comply with the GeoJSON recommendation https://datatracker.ietf.org/doc/html/rfc7946#section-3.1.9
## Adding to your project

Add the following dependency to your pom.xml

```xml
<dependency>
  <groupId>io.github.ci-cmg</groupId>
  <artifactId>polar-processor</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Runtime Requirements
* Java 8

## Building From Source
Maven 3.6.0+ is required.
```bash
mvn clean install
```

## Usage

### Split JTS geometries across the meridian and anti-meridian near the Earth’s poles

```java
Geometry splitGeometry = PolarProcessor.splitPolar(polygon, geometryFactory);
```