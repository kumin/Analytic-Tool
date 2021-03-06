package com.vcc.bigdata.condition.escondition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.vcc.bigdata.condition.Condition;
import com.vcc.bigdata.model.ElasticConstant;
import com.vcc.bigdata.platform.elastic.ElasticBulkInsert;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author: kumin on 03/07/2018
 **/

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InteractCondition.class),
})
public abstract class AdvanceCondition implements Condition<QueryBuilder> {
    private static final Logger logger = LoggerFactory.getLogger(AdvanceCondition.class);


    public static final String MUST = "must";
    public static final String SHOULD = "should";

    public static final String INTERACT = "interact";
    public static final String COUNTER = "counter";
    public static final String CONTENT = "content";

    private String bool;
    private String name;

    public String getBool() {
        return bool;
    }

    public void setBool(String bool) {
        this.bool = bool;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract void saveResultOr(ElasticBulkInsert bulkInsert, int bulkSize, String index
            , QueryBuilder profileTypeQuery);

    public abstract boolean checkConditionMust(Client client, String id);

    public abstract SearchResponse getSatifyGuids(Client client);

    public static SearchHit getProfile(Client client, String id, QueryBuilder profileTypeQuery) {
        BoolQueryBuilder mainQuery = new BoolQueryBuilder();
        mainQuery.must(profileTypeQuery)
                .must(QueryBuilders.boolQuery()
                        .filter(QueryBuilders
                                .matchPhraseQuery("attr.id", id)));
        SearchResponse sr = client.prepareSearch(ElasticConstant.PROFILES_INDEX)
                .setTypes(ElasticConstant.PROFILES_TYPE)
                .setQuery(mainQuery)
                .execute()
                .actionGet();
        if (sr.getHits().getTotalHits() > 0) return sr.getHits().getAt(0);
        else return null;

    }

    public static void saveProfile(ElasticBulkInsert bulkInsert, int bulkSize, String index, SearchHit hit){

        Map<String, Object> source = hit.getSource();
        source.put("inserted_time",
                Collections.singletonList(Collections.singletonMap("value",new Date())));
        bulkInsert.addRequest(index
                , ElasticConstant.PROFILES_TYPE
                , hit.id()
                , source);

        if (bulkInsert.bulkSize() > bulkSize) {
            BulkResponse response = bulkInsert.submitBulk();
            logger.info(Thread.currentThread().getName() + " - Submit bulk took "
                    + response.getTook().getSecondsFrac());
        }
    }
}
