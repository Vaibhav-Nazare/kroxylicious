# Kroxylicious Sample

This sample filter project provides examples to help you learn how [custom filters](https://kroxylicious.io/kroxylicious/#_custom_filters) work in Kroxylicious. To learn more about Kroxylicious, visit the [docs](https://kroxylicious.io/kroxylicious). 

## Getting started

### Build

Building the sample project is easy! You can build the **kroxylicious-sample** jar either on its own or with the rest of the Kroxylicious project.

To build all of Kroxylicious, including the sample:

```
$ mvn clean install
```

To build the sample on its own:

```
$ mvn clean install -pl kroxylicious-sample
```

Build with the `dist` profile for creating executable JARs:

```
$ mvn clean verify -Pdist -Dquick
```

### Run

Build with the `dist` profile as above, then run the following command:

```
$ java -cp {path-to-your-class-path}:kroxylicious-sample/target/kroxylicious-sample-*-SNAPSHOT.jar:kroxylicious/target/kroxylicious-*-SNAPSHOT.jar io.kroxylicious.proxy.Kroxylicious --config kroxylicious-sample/sample-proxy-config.yml
```

### Configure

Filters can be added and removed by altering the `filters` list in the `sample-proxy-config.yml` file. You can also reconfigure the sample filters by changing the configuration values in this file.

The **SampleFetchResponseFilter** and **SampleProduceRequestFilter** each have two configuration values that must be specified for them to work:

 - `findValue` - the string the filter will search for in the produce/fetch data
 - `replacementValue` - the string the filter will replace the value above with

#### Default Configuration


The default configuration for **SampleProduceRequestFilter** is:

```yaml
filters:
  - type: SampleProduceRequest
    config:
      findValue: foo
      replacementValue: bar
```

This means that it will search for the string `foo` in the produce data and replace all occurrences with the string `bar`. For example, if a Kafka Producer sent a produce request with data `{"myValue":"foo"}`, the filter would transform this into `{"myValue":"bar"}` and Kroxylicious would send that to the Kafka Broker instead. 

The default configuration for **SampleFetchResponseFilter** is:

```yaml
filters:
  - type: SampleFetchResponse
    config:
      findValue: bar
      replacementValue: baz
```

This means that it will search for the string `bar` in the fetch data and replace all occurrences with the string `baz`. For example, if a Kafka Broker sent a fetch response with data `{"myValue":"bar"}`, the filter would transform this into `{"myValue":"baz"}` and Kroxylicious would send that to the Kafka Consumer instead.