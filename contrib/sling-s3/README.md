# Sling-S3

A set of [crankstart](https://github.com/apache/sling/tree/trunk/contrib/crankstart/) configuration files starting a Sling instance based on Oak, with optional support to MongoDB (as a node store) and/or S3 (as a blob store).

## Requirements

* make
* JRE 1.7
* ruby

## Usage

1. If you'd like to use S3, create `crank-s3.d/05-credentials.txt`.
2. Run of the `make` goals: `start`, `start-s3`, `start-mongo`, `start-s3-mongo`.

### 05-credentials.txt file format

```
defaults s3AccessKey ...
defaults s3SecretKey ...
defaults s3Bucket ...
defaults s3Region ...
defaults s3EndPoint ...
```

## Goals

* `make all` - creates crankstart configuration files for each possible configuration,
* `make start[-s3][-mongo]` - start Sling instance,
* `make update-bundles` - download the latest list of Sling bundles from the launchpad,
* `make clean` - remove all generated files and the Sling instance directory.
