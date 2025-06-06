# DataSketches HyperLogLog Notes

Steps to add plugin UDF to Trino:

Reference: [Trino investigation](https://vungle.atlassian.net/wiki/spaces/DE/pages/2604204083/Trino+investigation#Vungle-Trino-Enhancement): 

**These stpes below are run under** ```trino``` **repository.**
Just save the plugin functions here, as they do not interact with the original trino code.

```pwd```
trino/plugin/trino-datasketches-hll

1. Build the plugin jar:
   ```bash
   ../../mvnw clean install -DskipTests
   ../../mvnw clean package -DskipTests
   ```
   
   Saved jars on S3:
   ```bash
      aws s3 cp target/trino-datasketches-hll-408-SNAPSHOT.jar s3://vungle2-dataeng/trino-plugin/datasketches-hll/
      aws s3 cp target/trino-datasketches-hll-408-SNAPSHOT-services.jar s3://vungle2-dataeng/trino-plugin/datasketches-hll/
      aws s3 cp --recursive target/trino-datasketches-hll-408-SNAPSHOT/* s3://vungle2-dataeng/trino-plugin/datasketches-hll/
   ```
   [trino-datasketches-hll](s3://vungle2-dataeng/trino-plugin/datasketches-hll/)
   
2. Build the image corresponding to the version mounted from trinodb/trino (e.g. ```FROM trinodb/trino:463```):
   ```bash
   docker build -t trino-datasketches-hll .
   ```
   
3. Run the image:
   ```bash
   docker run -p 8080:8080 trino-datasketches-hll
   ```
   
4. Verify the plugin function:
    ```bash
    trino --server http://localhost:8080
    ```
   
    ```
    trino> SELECT hll_estimate(hll_add(hll_add(hll_create(), 'value1'), 'value2'));
    _col0
    -------------------
    2.000000004967054
    (1 row)
    
    Query 20250605_091033_00001_z2qnh, FINISHED, 1 node
    Splits: 1 total, 1 done (100.00%)
    0.83 [0 rows, 0B] [0 rows/s, 0B/s]
    
    trino> exit;
    ```
5. Push the image to vungle docker hub:
    ```bash
    docker tag trino-datasketches-hll:latest vungle/trino:463-datasketches-hll
    docker push vungle/trino:463-datasketches-hll
    ```
   
    Check [docker hub](https://hub.docker.com/repository/docker/vungle/trino/tags/)

6. Test on staging env values.yaml:
    ```yaml
    image:
      pullPolicy: IfNotPresent
      repository: vungle/trino
      tag: 463-datasketches-hll
    ```

    ```bash
    trino --server https://internal-staging.trino.vungle.io/ --user=trino --password=true
    ```

   ```
    trino> SELECT hll_estimate(hll_add(hll_add(hll_create(), 'value1'), 'value2'));
    _col0
    -------------------
    2.000000004967054
    (1 row)
    
    Query 20250605_091033_00001_z2qnh, FINISHED, 1 node
    Splits: 1 total, 1 done (100.00%)
    0.83 [0 rows, 0B] [0 rows/s, 0B/s]
    
    trino> exit;
    ```
