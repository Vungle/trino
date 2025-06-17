FROM trinodb/trino:463

# Create plugin directory
RUN mkdir -p /usr/lib/trino/plugin/datasketches-hll

# Copy the plugin files from target directory
COPY target/trino-datasketches-hll-408-SNAPSHOT.jar /usr/lib/trino/plugin/datasketches-hll/
COPY target/trino-datasketches-hll-408-SNAPSHOT-services.jar /usr/lib/trino/plugin/datasketches-hll/
COPY target/trino-datasketches-hll-408-SNAPSHOT/* /usr/lib/trino/plugin/datasketches-hll/

# Create function configuration directory and file
RUN mkdir -p /etc/trino/function
RUN echo "function-plugin.name=datasketches-hll" > /etc/trino/function/datasketches-hll.properties
