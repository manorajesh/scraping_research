package com.projectleo.scraper.database;

import com.projectleo.scraper.scrapers.JobResult;
import com.projectleo.scraper.util.PropertiesUtil;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Database implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(Database.class);
  private static final String COLLECTION_NAME = "job_results";
  private static final String CLUSTER_ENDPOINT = PropertiesUtil.getProperty("milvus.endpoint");
  private static final int HASH_LENGTH = 256; // 256 bits for SHA-256 hash
  private static final int VECTOR_DIMENSION = 3072; // for OpenAI's text-embedding-3-large
  private final MilvusClientV2 client;

  public Database() {
    ConnectConfig connectConfig = ConnectConfig.builder().uri(CLUSTER_ENDPOINT).build();

    client = new MilvusClientV2(connectConfig);

    createCollectionIfNotExists();
  }

  private void createCollectionIfNotExists() {
    if (!client.hasCollection(HasCollectionReq.builder().collectionName(COLLECTION_NAME).build())) {
      createCollection();
    }
  }

  private void createCollection() {
    // 1. Create collection schema
    CreateCollectionReq.CollectionSchema schema = client.createSchema();

    // id
    schema.addField(
        AddFieldReq.builder()
            .fieldName("id")
            .dataType(DataType.Int64)
            .isPrimaryKey(true)
            .autoID(true)
            .build());

    // vector
    schema.addField(
        AddFieldReq.builder()
            .fieldName("vector")
            .dataType(DataType.FloatVector)
            .dimension(VECTOR_DIMENSION) // 512 dimensions for 512-bit vectors
            .build());

    // jobLinkHash
    schema.addField(
        AddFieldReq.builder()
            .fieldName("job_link_hash")
            .dataType(DataType.BinaryVector)
            .dimension(HASH_LENGTH) // 256 bits for SHA-256 hash
            .build());

    // timestamp
    schema.addField(AddFieldReq.builder().fieldName("timestamp").dataType(DataType.Int64).build());

    // company
    schema.addField(AddFieldReq.builder().fieldName("company").dataType(DataType.String).build());

    // jobTitle
    schema.addField(AddFieldReq.builder().fieldName("job_title").dataType(DataType.String).build());

    // industry
    schema.addField(AddFieldReq.builder().fieldName("industry").dataType(DataType.String).build());

    // responsibilities
    schema.addField(
        AddFieldReq.builder().fieldName("responsibilities").dataType(DataType.Array).build());

    // qualifications
    schema.addField(
        AddFieldReq.builder().fieldName("qualifications").dataType(DataType.Array).build());

    // skills
    schema.addField(AddFieldReq.builder().fieldName("skills").dataType(DataType.Array).build());

    // location
    schema.addField(AddFieldReq.builder().fieldName("location").dataType(DataType.String).build());

    // 2. Prepare index parameters
    IndexParam indexParamForIdField =
        IndexParam.builder().fieldName("id").indexType(IndexParam.IndexType.AUTOINDEX).build();

    IndexParam indexParamForVectorField =
        IndexParam.builder()
            .fieldName("vector")
            .indexType(IndexParam.IndexType.IVF_FLAT)
            .metricType(IndexParam.MetricType.COSINE)
            .extraParams(Map.of("nlist", 1024))
            .build();

    List<IndexParam> indexParams = new ArrayList<>();
    indexParams.add(indexParamForIdField);
    indexParams.add(indexParamForVectorField);

    // 3 Create a collection with schema and index parameters
    CreateCollectionReq customizedSetupReq1 =
        CreateCollectionReq.builder()
            .collectionName(COLLECTION_NAME)
            .collectionSchema(schema)
            .indexParams(indexParams)
            .build();

    client.createCollection(customizedSetupReq1);

    // Check if the collection is created
    boolean res =
        client.getLoadState(GetLoadStateReq.builder().collectionName(COLLECTION_NAME).build());

    if (res) {
      logger.info("Collection created successfully");
    } else {
      logger.error("Collection failed to be created");
      throw new RuntimeException("Collection failed to be created");
    }
  }

  public void writeToDatabase(List<JobResult> jobResults) {}

  public boolean jobLinkExists(byte[] jobLinkHash) {
    List<Object> vectorList = new ArrayList<>();
    vectorList.add(jobLinkHash);

    SearchReq searchReq =
        SearchReq.builder()
            .collectionName(COLLECTION_NAME)
            .annsField("job_link_hash")
            .topK(1)
            .data(Collections.singletonList(vectorList))
            .build();

    SearchResp searchResp = client.search(searchReq);

    return !searchResp.getSearchResults().isEmpty();
  }

  @Override
  public void close() {
    ReleaseCollectionReq releaseCollectionReq =
        ReleaseCollectionReq.builder().collectionName(COLLECTION_NAME).build();

    client.releaseCollection(releaseCollectionReq);

    boolean res =
        client.getLoadState(GetLoadStateReq.builder().collectionName(COLLECTION_NAME).build());

    if (!res) {
      logger.info("Collection released successfully");
    } else {
      logger.error("Collection failed to be released");
      throw new RuntimeException("Collection failed to be released");
    }
  }
}
